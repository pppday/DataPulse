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

@Component
@RequiredArgsConstructor
public class DataPulseAiTools {

    private static final int TOOL_PREVIEW_LIMIT = 30;

    private final SchemaCatalogService schemaCatalogService;
    private final SqlExampleRetrievalService sqlExampleRetrievalService;
    private final SqlSafetySandboxService sqlSafetySandboxService;
    private final DynamicDataSourceService dynamicDataSourceService;
    private final ObjectMapper objectMapper;

    @Tool(description = "获取指定 MySQL 数据源已授权白名单表的结构定义、字段注释与业务计算口径规则")
    public String listAuthorizedSchema(
            @ToolParam(description = "目标数据源ID，默认 1") Long datasourceId) {
        long dsId = datasourceId != null ? datasourceId : 1L;
        return schemaCatalogService.getWarehouseSchemaPrompt(dsId);
    }

    @Tool(description = "按语义相似度检索已人工校验的同类自然语言问答与参考 SQL 示例")
    public String searchSimilarSqlExamples(
            @ToolParam(description = "目标数据源ID，默认 1") Long datasourceId,
            @ToolParam(description = "用户的自然语言分析问题") String question) {
        long dsId = datasourceId != null ? datasourceId : 1L;
        List<SysSqlExample> examples = sqlExampleRetrievalService.retrieveTopK(dsId, question, 3);
        if (examples == null || examples.isEmpty()) {
            return "暂无匹配的参考示例。";
        }
        StringBuilder sb = new StringBuilder();
        for (SysSqlExample ex : examples) {
            sb.append("Q: ").append(ex.getQuestion()).append("\n")
                    .append("SQL: ").append(ex.getSqlText()).append("\n");
        }
        return sb.toString().trim();
    }

    @Tool(description = "校验并执行只读 MySQL SELECT 查询；所有 SQL 必须先经过 JSqlParser AST 安全沙箱与表白名单校验并强制限制行数")
    public String validateAndExecuteReadOnlySql(
            @ToolParam(description = "目标数据源ID，默认 1") Long datasourceId,
            @ToolParam(description = "待校验并执行的只读 SELECT SQL 语句") String sql) {
        long dsId = datasourceId != null ? datasourceId : 1L;
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Set<String> allowedTables = schemaCatalogService.getWhitelistedTables(dsId);
            SqlSafetySandboxService.SafetyCheckResult check =
                    sqlSafetySandboxService.inspectAndSanitize(sql, allowedTables);
            if (!check.isSafe()) {
                result.put("safe", false);
                result.put("blockedReason", check.getReason());
                return objectMapper.writeValueAsString(result);
            }

            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(dsId);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(check.getSanitizedSql());
            List<Map<String, Object>> preview = rows.size() > TOOL_PREVIEW_LIMIT
                    ? rows.subList(0, TOOL_PREVIEW_LIMIT)
                    : rows;

            result.put("safe", true);
            result.put("sanitizedSql", check.getSanitizedSql());
            result.put("totalRowCount", rows.size());
            result.put("previewRows", preview);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            result.put("safe", false);
            result.put("error", e.getMessage());
            try {
                return objectMapper.writeValueAsString(result);
            } catch (Exception ex) {
                return "{\"safe\":false,\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}";
            }
        }
    }
}
