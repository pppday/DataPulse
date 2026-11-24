package com.pulseos.datapulse.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseos.datapulse.entity.SysSqlExample;
import com.pulseos.datapulse.service.DynamicDataSourceService;
import com.pulseos.datapulse.service.SchemaCatalogService;
import com.pulseos.datapulse.service.SqlExampleRetrievalService;
import com.pulseos.datapulse.service.SqlSafetySandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring AI Function Calling（工具调用）扩展工具箱。
 * <p>
 * 把平台核心取数能力（白名单表结构查询、Few-Shot SQL 范例检索、经安全沙箱校验后的只读 SQL 试跑）
 * 封装成带 {@code @Tool} 注解的标准工具方法，供大模型在需要主动拉取元信息或验证 SQL 时按需调用。
 * 所有实际落库的查询都会先强制经过 {@link SqlSafetySandboxService}（JSqlParser AST 沙箱）校验，绝不裸跑 SQL。
 */
@Component
@RequiredArgsConstructor
public class DataPulseAiTools {

    /** 返回给大模型的预览数据最大行数，防止超大结果集把模型上下文（Token）撑爆 */
    private static final int TOOL_PREVIEW_LIMIT = 30;

    private final SchemaCatalogService schemaCatalogService;
    private final SqlExampleRetrievalService sqlExampleRetrievalService;
    private final SqlSafetySandboxService sqlSafetySandboxService;
    private final DynamicDataSourceService dynamicDataSourceService;
    private final ObjectMapper objectMapper;

    /**
     * 工具 1：拉取目标数据源已授权的白名单表结构与业务口径说明，帮助模型搞清楚有哪些表、哪些字段能查。
     */
    @Tool(description = "获取指定 MySQL 数据源已授权白名单表的结构定义、字段注释与业务计算口径规则")
    public String listAuthorizedSchema(
            @ToolParam(description = "目标数据源ID，默认 1") Long datasourceId) {
        // 未指定数据源时默认回退到 1 号主数仓
        long dsId = datasourceId != null ? datasourceId : 1L;
        return schemaCatalogService.getWarehouseSchemaPrompt(dsId);
    }

    /**
     * 工具 2：按自然语言问题检索最相似的 3 条人工审核过的参考 SQL（Few-Shot 范例），提升复杂统计口径的命中率。
     */
    @Tool(description = "按语义相似度检索已人工校验的同类自然语言问答与参考 SQL 示例")
    public String searchSimilarSqlExamples(
            @ToolParam(description = "目标数据源ID，默认 1") Long datasourceId,
            @ToolParam(description = "用户的自然语言分析问题") String question) {
        long dsId = datasourceId != null ? datasourceId : 1L;
        List<SysSqlExample> examples = sqlExampleRetrievalService.retrieveTopK(dsId, question, 3);
        if (examples == null || examples.isEmpty()) {
            return "暂无匹配的参考示例。";
        }
        // 拼装成简洁的「问 + SQL」文本块喂给大模型参考
        StringBuilder sb = new StringBuilder();
        for (SysSqlExample ex : examples) {
            sb.append("Q: ").append(ex.getQuestion()).append("\n")
                    .append("SQL: ").append(ex.getSqlText()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 工具 3：对大模型生成的 SQL 做安全审计并执行只读查询，返回结构化 JSON 预览。
     * <p>
     * 为什么不能直接执行？因为模型可能误生成写语句、越权查系统表或全表扫描拖垮数据库，
     * 因此必须先过白名单 + JSqlParser AST 安全沙箱，再把返回结果截断为前 30 行预览。
     */
    @Tool(description = "校验并执行只读 MySQL SELECT 查询；所有 SQL 必须先经过 JSqlParser AST 安全沙箱与表白名单校验并强制限制行数")
    public String validateAndExecuteReadOnlySql(
            @ToolParam(description = "目标数据源ID，默认 1") Long datasourceId,
            @ToolParam(description = "待校验并执行的只读 SELECT SQL 语句") String sql) {
        long dsId = datasourceId != null ? datasourceId : 1L;
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // 第一步：查出该数据源允许查询的表白名单，并交由 AST 安全沙箱校验 + 自动追加 LIMIT
            Set<String> allowedTables = schemaCatalogService.getWhitelistedTables(dsId);
            SqlSafetySandboxService.SafetyCheckResult check =
                    sqlSafetySandboxService.inspectAndSanitize(sql, allowedTables);
            if (!check.isSafe()) {
                // 被沙箱拦截时把拦截原因如实返回，方便上层纠错或提示用户
                result.put("safe", false);
                result.put("blockedReason", check.getReason());
                return objectMapper.writeValueAsString(result);
            }

            // 第二步：路由到对应数据源的 JdbcTemplate 执行净化后的只读 SQL
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(dsId);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(check.getSanitizedSql());
            // 仅截取前 TOOL_PREVIEW_LIMIT 行返回给大模型，避免上下文过长
            List<Map<String, Object>> preview = rows.size() > TOOL_PREVIEW_LIMIT
                    ? rows.subList(0, TOOL_PREVIEW_LIMIT)
                    : rows;

            result.put("safe", true);
            result.put("sanitizedSql", check.getSanitizedSql());
            result.put("totalRowCount", rows.size());
            result.put("previewRows", preview);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            // 执行报错（如语法错误、字段不存在）时封装成 JSON 返回，便于调用方查看具体错误
            result.put("safe", false);
            result.put("error", e.getMessage());
            try {
                return objectMapper.writeValueAsString(result);
            } catch (Exception ex) {
                // 兜底：极端情况下连 JSON 序列化也失败时手动拼一条安全 JSON
                return "{\"safe\":false,\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}";
            }
        }
    }
}
