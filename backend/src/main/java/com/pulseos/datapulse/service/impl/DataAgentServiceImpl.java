package com.pulseos.datapulse.service.impl;

import com.pulseos.datapulse.ai.DataPulseAiTools;
import com.pulseos.datapulse.common.BusinessException;
import com.pulseos.datapulse.config.UserContext;
import com.pulseos.datapulse.dto.AgentQueryResultVO;
import com.pulseos.datapulse.dto.AskAgentRequest;
import com.pulseos.datapulse.entity.QueryHistory;
import com.pulseos.datapulse.entity.SysDatasource;
import com.pulseos.datapulse.entity.SysSqlExample;
import com.pulseos.datapulse.mapper.QueryHistoryMapper;
import com.pulseos.datapulse.service.DataAgentService;
import com.pulseos.datapulse.service.DynamicDataSourceService;
import com.pulseos.datapulse.service.SchemaCatalogService;
import com.pulseos.datapulse.service.SemanticQueryCacheService;
import com.pulseos.datapulse.service.SqlExampleRetrievalService;
import com.pulseos.datapulse.service.SqlSafetySandboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataAgentServiceImpl implements DataAgentService {

    private static final Logger log = LoggerFactory.getLogger(DataAgentServiceImpl.class);

    private final SchemaCatalogService schemaCatalogService;
    private final SqlSafetySandboxService sqlSafetySandboxService;
    private final DynamicDataSourceService dynamicDataSourceService;
    private final SemanticQueryCacheService semanticQueryCacheService;
    private final SqlExampleRetrievalService sqlExampleRetrievalService;
    private final QueryHistoryMapper queryHistoryMapper;
    private final DataPulseAiTools dataPulseAiTools;
    private final ObjectProvider<ChatClient> statelessChatClientProvider;
    private final ObjectProvider<ChatClient> conversationalChatClientProvider;
    private final ObjectProvider<ChatMemory> chatMemoryProvider;

    @Value("${spring.ai.openai.api-key:}")
    private String llmApiKey;

    public DataAgentServiceImpl(
            SchemaCatalogService schemaCatalogService,
            SqlSafetySandboxService sqlSafetySandboxService,
            DynamicDataSourceService dynamicDataSourceService,
            SemanticQueryCacheService semanticQueryCacheService,
            SqlExampleRetrievalService sqlExampleRetrievalService,
            QueryHistoryMapper queryHistoryMapper,
            DataPulseAiTools dataPulseAiTools,
            @Qualifier("statelessChatClient") ObjectProvider<ChatClient> statelessChatClientProvider,
            @Qualifier("conversationalChatClient") ObjectProvider<ChatClient> conversationalChatClientProvider,
            ObjectProvider<ChatMemory> chatMemoryProvider) {
        this.schemaCatalogService = schemaCatalogService;
        this.sqlSafetySandboxService = sqlSafetySandboxService;
        this.dynamicDataSourceService = dynamicDataSourceService;
        this.semanticQueryCacheService = semanticQueryCacheService;
        this.sqlExampleRetrievalService = sqlExampleRetrievalService;
        this.queryHistoryMapper = queryHistoryMapper;
        this.dataPulseAiTools = dataPulseAiTools;
        this.statelessChatClientProvider = statelessChatClientProvider;
        this.conversationalChatClientProvider = conversationalChatClientProvider;
        this.chatMemoryProvider = chatMemoryProvider;
    }

    private boolean isLlmConfigured() {
        return llmApiKey != null
                && !llmApiKey.isBlank()
                && !llmApiKey.startsWith("sk-placeholder")
                && statelessChatClientProvider.getIfAvailable() != null;
    }

    @Override
    public AgentQueryResultVO askAgent(AskAgentRequest request) {
        Long userId = UserContext.getCurrentUserId();
        String userPrompt = request.getPrompt() != null ? request.getPrompt().trim() : "";
        String customSql = request.getCustomSql() != null ? request.getCustomSql().trim() : "";

        if (userPrompt.isEmpty() && customSql.isEmpty()) {
            throw new BusinessException(400, "提问需求或自定义 SQL 不能为空");
        }

        Long datasourceId = request.getDatasourceId() != null ? request.getDatasourceId() : 1L;
        SysDatasource ds = dynamicDataSourceService.getById(datasourceId);

        String generatedSql;
        boolean isCustomSqlMode = !customSql.isEmpty();
        List<String> fewShotQuestions = List.of();

        if (isCustomSqlMode) {
            generatedSql = customSql;
            if (userPrompt.isEmpty()) {
                userPrompt = "自定义 SQL 执行分析";
            }
        } else {
            // 1. 尝试从 Redis 查询指纹缓存中读取 (命中则毫秒级返回)
            AgentQueryResultVO cached = semanticQueryCacheService.get(ds.getId(), userPrompt);
            if (cached != null) {
                cached.setCacheHit(true);
                cached.setDatasourceId(ds.getId());
                cached.setDatasourceName(ds.getName());
                return cached;
            }

            // 2. 动态获取当前数据源已授权的数仓元数据 DDL 字典与白名单
            String schemaContext = schemaCatalogService.getWarehouseSchemaPrompt(ds.getId());

            // 2.5 Few-Shot 相似问题与 SQL 示例向量检索（异常时降级跳过，不影响主流程）
            String fewShotBlock = "";
            try {
                List<SysSqlExample> matched = sqlExampleRetrievalService.retrieveTopK(ds.getId(), userPrompt, 3);
                if (!matched.isEmpty()) {
                    fewShotQuestions = matched.stream().map(SysSqlExample::getQuestion).toList();
                    StringBuilder fb = new StringBuilder();
                    fb.append("【参考示例（已人工校验的相似问题与 SQL，仅借鉴写法与业务口径，表和字段以元数据字典为准）】\n");
                    for (SysSqlExample ex : matched) {
                        fb.append("Q: ").append(ex.getQuestion()).append("\n");
                        fb.append("SQL: ").append(ex.getSqlText()).append("\n");
                    }
                    fewShotBlock = fb.toString();
                }
            } catch (Exception e) {
                log.warn("[Few-Shot 示例检索异常，跳过注入] error={}", e.getMessage());
            }

            // 3. 调用大模型生成针对性只读 SQL
            generatedSql = generateSqlWithLlm(userPrompt, schemaContext, fewShotBlock);
        }

        Set<String> allowedTables = schemaCatalogService.getWhitelistedTables(ds.getId());

        // 4. 经过 JSqlParser AST 语法树安全审查沙箱（基于动态白名单）
        SqlSafetySandboxService.SafetyCheckResult safetyResult = sqlSafetySandboxService.inspectAndSanitize(generatedSql, allowedTables);

        if (!safetyResult.isSafe()) {
            // 被安全沙箱拦截
            QueryHistory history = QueryHistory.builder()
                    .userId(userId)
                    .userPrompt(userPrompt)
                    .generatedSql(safetyResult.getSanitizedSql())
                    .isSafe(0)
                    .safetyReason(safetyResult.getReason())
                    .executionTimeMs(0L)
                    .rowCount(0)
                    .chartType("table")
                    .insightSummary("【安全风险拦截提示】该查询由于触发安全防御机制已被拒绝执行，未向底层数据库发送任何请求。")
                    .createdAt(LocalDateTime.now())
                    .build();
            queryHistoryMapper.insert(history);

            return AgentQueryResultVO.builder()
                    .historyId(history.getId())
                    .userPrompt(userPrompt)
                    .generatedSql(safetyResult.getSanitizedSql())
                    .isSafe(0)
                    .safetyReason(safetyResult.getReason())
                    .executionTimeMs(0L)
                    .columns(List.of())
                    .rows(List.of())
                    .rowCount(0)
                    .chartType("table")
                    .chartOptions(Map.of())
                    .insightSummary(history.getInsightSummary())
                    .cacheHit(false)
                    .datasourceId(ds.getId())
                    .datasourceName(ds.getName())
                    .fewShotQuestions(fewShotQuestions)
                    .build();
        }

        // 5. 执行底层只读 SQL 查询 (动态数据源路由)
        JdbcTemplate targetJdbcTemplate = dynamicDataSourceService.getJdbcTemplate(ds.getId());
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> rows;
        try {
            rows = targetJdbcTemplate.queryForList(safetyResult.getSanitizedSql());
        } catch (Exception e) {
            log.error("[SQL 底层执行失败] sql={}, error={}", safetyResult.getSanitizedSql(), e.getMessage());
            throw new BusinessException(400, "SQL 执行失败: " + e.getMessage());
        }
        long executionTimeMs = System.currentTimeMillis() - startTime;

        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());

        // 6. 自动推断最优图表类型并生成 ECharts Options 配置
        String chartType = inferChartType(request.getChartPreference(), columns, rows);
        Map<String, Object> chartOptions = buildEChartsOptions(chartType, columns, rows, userPrompt);

        // 7. 深度商业洞察提炼
        String insightSummary = generateInsightWithLlm(userPrompt, safetyResult.getSanitizedSql(), rows);

        // 8. 持久化记录
        QueryHistory history = QueryHistory.builder()
                .userId(userId)
                .userPrompt(userPrompt)
                .generatedSql(safetyResult.getSanitizedSql())
                .isSafe(1)
                .safetyReason("PASS")
                .executionTimeMs(executionTimeMs)
                .rowCount(rows.size())
                .chartType(chartType)
                .insightSummary(insightSummary)
                .createdAt(LocalDateTime.now())
                .build();
        queryHistoryMapper.insert(history);

        AgentQueryResultVO vo = AgentQueryResultVO.builder()
                .historyId(history.getId())
                .userPrompt(userPrompt)
                .generatedSql(safetyResult.getSanitizedSql())
                .isSafe(1)
                .safetyReason("PASS")
                .executionTimeMs(executionTimeMs)
                .columns(columns)
                .rows(rows)
                .rowCount(rows.size())
                .chartType(chartType)
                .chartOptions(chartOptions)
                .insightSummary(insightSummary)
                .cacheHit(false)
                .datasourceId(ds.getId())
                .datasourceName(ds.getName())
                .fewShotQuestions(fewShotQuestions)
                .build();

        // 写入 Redis 查询指纹缓存 (TTL: 1小时)
        semanticQueryCacheService.put(ds.getId(), userPrompt, vo, 3600);

        return vo;
    }

    @Override
    public Flux<String> streamChat(AskAgentRequest request) {
        String userPrompt = request != null && request.getPrompt() != null ? request.getPrompt().trim() : "";
        if (userPrompt.isEmpty()) {
            throw new BusinessException(400, "提问内容不能为空");
        }
        Long datasourceId = request.getDatasourceId() != null ? request.getDatasourceId() : 1L;
        String convId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId().trim()
                : "user:" + UserContext.getCurrentUserId() + ":ds:" + datasourceId;

        ChatClient convClient = conversationalChatClientProvider.getIfAvailable();
        if (!isLlmConfigured() || convClient == null) {
            return Flux.just("未配置 DEEPSEEK_API_KEY，请在环境变量中配置后体验 Spring AI 流式多轮对话与 Tool Calling。");
        }

        String agentSystemPrompt = """
                你是 DataPulse 企业级智能数据洞察 Agent（数据源 ID=%d）。
                请结合历史对话上下文，按需调用工具完成分析：
                1. 如需了解表名、字段与业务口径，先调用 listAuthorizedSchema(%d)；
                2. 可调用 searchSimilarSqlExamples(%d, question) 参考历史标准 SQL 写法；
                3. 如需真实查数，必须调用 validateAndExecuteReadOnlySql(%d, sql) 获取经 AST 安全沙箱审查的执行结果；
                4. 严禁臆造数据或跳过安全工具直接输出未验证的数值结论。
                """.formatted(datasourceId, datasourceId, datasourceId, datasourceId);

        return convClient.prompt()
                .system(agentSystemPrompt)
                .user(userPrompt)
                .tools(dataPulseAiTools)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .stream()
                .content();
    }

    @Override
    public void clearConversationMemory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        ChatMemory memory = chatMemoryProvider.getIfAvailable();
        if (memory != null) {
            memory.clear(conversationId.trim());
        }
    }

    @Override
    public List<QueryHistory> listHistory(int limit) {
        int safeLimit = Math.min(Math.max(limit, 5), 100);
        return queryHistoryMapper.selectRecentHistoryWithUser(safeLimit);
    }

    @Override
    public QueryHistory getHistoryDetail(Long id) {
        return queryHistoryMapper.selectById(id);
    }

    /**
     * 调用 Spring AI ChatClient 进行 Text-to-SQL 推导
     */
    private String generateSqlWithLlm(String userPrompt, String schemaContext, String fewShotBlock) {
        if (!isLlmConfigured()) {
            log.info("[DEEPSEEK_API_KEY 未配置，直接使用本地启发式规则兜底]");
            return fallbackSqlHeuristic(userPrompt);
        }
        String systemInstruction = """
            你是企业级智能 Text-to-SQL 数据分析 Agent。
            你的职责是基于给出的数据库元数据字典，将用户的自然语言问题转化为精准、严谨、只读的 MySQL 8.0 SQL 查询语句。
            
            【必须严格遵守的铁律】：
            1. 只能使用上下文【企业数据库架构元数据字典】中包含的数据表与字段，严禁臆造不存在的表名或字段名。
            2. 严禁任何 DML/DDL（禁止 UPDATE, DELETE, INSERT, DROP, ALTER, TRUNCATE 等任何非只读指令）。
            3. 列别名必须清晰、具有业务可读性。所有中文别名及在 ORDER BY / GROUP BY 中引用的中文别名，必须一律用反引号包裹（例如: `总销售额`, `商品名称`, `销售总数量`）。
            4. 排序与条数控制：
               - 当用户明确指定了数量（如“前5个”、“TOP 10”、“前3名”）时，严格按照指定数字使用 LIMIT（如 LIMIT 5）。
               - 当用户明确要求“所有 / 全部 / 列表 / 清单”（例如“查所有商品”、“列出所有订单”）或进行 GROUP BY 汇总分组时，切勿擅自添加 LIMIT 限制！
               - 仅当用户既未指定数量又未要求全部、属于宽泛排行类问题时，才可使用合理的默认上限（例如 LIMIT 50）。
            5. 条件过滤谨慎原则：除非用户问题中有极其明确的状态限制（例如用户明确指出“已支付”、“已完成”），否则切勿擅自主观添加未要求的多余过滤条件（例如不要擅自添加 status IN (...) 等严苛状态码过滤），避免因测试数据或状态流转导致 0 行无数据返回！
            6. 输出必须只包含一条合法的标准 MySQL 8.0 SQL 语句，禁止输出任何寒暄或 Markdown 以外的多余解释。
            """;

        StringBuilder userContentBuilder = new StringBuilder(schemaContext);
        if (fewShotBlock != null && !fewShotBlock.isBlank()) {
            userContentBuilder.append("\n").append(fewShotBlock);
        }
        userContentBuilder.append("\n【用户分析需求】: ").append(userPrompt).append("\n请直接输出标准 MySQL 8.0 SQL：");
        String userContent = userContentBuilder.toString();

        try {
            ChatClient chatClient = statelessChatClientProvider.getObject();
            String raw = chatClient.prompt()
                    .system(systemInstruction)
                    .user(userContent)
                    .call()
                    .content();

            if (raw != null && !raw.isBlank()) {
                String cleanSql = raw.replaceAll("(?s)```sql\\s*(.*?)\\s*```", "$1")
                        .replaceAll("(?s)```\\s*(.*?)\\s*```", "$1")
                        .trim();
                if (!cleanSql.isBlank()) {
                    return cleanSql;
                }
            }
        } catch (Exception e) {
            log.warn("[Spring AI Text-to-SQL 调用失败，启用智能规则兜底引擎] error={}", e.getMessage());
        }

        // 离线/降级高保真规则匹配兜底
        return fallbackSqlHeuristic(userPrompt);
    }

    /**
     * 自动推断最优图表类型 (bar / line / pie / table)
     */
    private String inferChartType(String preference, List<String> columns, List<Map<String, Object>> rows) {
        if (preference != null && !preference.isBlank() && !"auto".equalsIgnoreCase(preference)) {
            return preference.toLowerCase();
        }

        if (rows.isEmpty() || columns.size() < 2) {
            return "table";
        }

        String firstCol = columns.get(0).toLowerCase();

        // 1. 若首列包含日期/时间相关词汇，推荐折线图
        if (firstCol.contains("date") || firstCol.contains("time") || firstCol.contains("月") || firstCol.contains("日") || firstCol.contains("年")) {
            return "line";
        }

        // 2. 若数据行数在 2~6 行之间，且有分类与汇总金额，适合饼图
        if (rows.size() >= 2 && rows.size() <= 6 && (firstCol.contains("类") || firstCol.contains("渠道") || firstCol.contains("等级") || firstCol.contains("status"))) {
            return "pie";
        }

        // 3. 多数对比场景（大区排行、品类对比、销售榜单）首选柱状图
        return "bar";
    }

    /**
     * 自动组装标准化 ECharts 配置项
     */
    private Map<String, Object> buildEChartsOptions(String chartType, List<String> columns, List<Map<String, Object>> rows, String title) {
        if (rows.isEmpty() || columns.size() < 2) {
            return Map.of();
        }

        String xCol = columns.get(0);
        List<Object> xData = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            xData.add(row.get(xCol));
        }

        Map<String, Object> options = new HashMap<>();

        if ("pie".equalsIgnoreCase(chartType)) {
            String valCol = columns.get(1);
            List<Map<String, Object>> pieData = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                pieData.add(Map.of(
                        "name", String.valueOf(row.get(xCol)),
                        "value", row.get(valCol) != null ? row.get(valCol) : 0
                ));
            }

            options.put("tooltip", Map.of("trigger", "item", "formatter", "{b}: {c} ({d}%)"));
            options.put("legend", Map.of("orient", "horizontal", "bottom", 10));
            options.put("series", List.of(Map.of(
                    "name", valCol,
                    "type", "pie",
                    "radius", List.of("35%", "65%"),
                    "avoidLabelOverlap", true,
                    "itemStyle", Map.of("borderRadius", 6, "borderColor", "#fff", "borderWidth", 2),
                    "data", pieData
            )));
            return options;
        }

        // Bar or Line
        List<Map<String, Object>> seriesList = new ArrayList<>();
        List<String> legendData = new ArrayList<>();

        for (int i = 1; i < columns.size(); i++) {
            String colName = columns.get(i);
            legendData.add(colName);

            List<Object> seriesData = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                seriesData.add(row.get(colName) != null ? row.get(colName) : 0);
            }

            Map<String, Object> seriesItem = new HashMap<>();
            seriesItem.put("name", colName);
            seriesItem.put("type", "line".equalsIgnoreCase(chartType) ? "line" : "bar");
            if ("line".equalsIgnoreCase(chartType)) {
                seriesItem.put("smooth", true);
            } else {
                seriesItem.put("barMaxWidth", 36);
                seriesItem.put("itemStyle", Map.of("borderRadius", List.of(4, 4, 0, 0)));
            }
            seriesItem.put("data", seriesData);
            seriesList.add(seriesItem);
        }

        options.put("tooltip", Map.of("trigger", "axis"));
        options.put("legend", Map.of("data", legendData, "top", 10));
        options.put("grid", Map.of("left", "3%", "right", "4%", "bottom", "18%", "containLabel", true));
        options.put("xAxis", Map.of(
                "type", "category",
                "data", xData,
                "axisLabel", Map.of("interval", 0, "rotate", 30)
        ));
        options.put("yAxis", Map.of("type", "value"));
        options.put("series", seriesList);

        return options;
    }

    /**
     * 基于实际执行结果提炼商业分析与决策洞察
     */
    private String generateInsightWithLlm(String prompt, String sql, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "根据当前筛选条件，数仓内未检索到对应周期的流水或记录。建议放宽时间跨度或检查筛选维度。";
        }
        if (!isLlmConfigured()) {
            return "本次查询共汇总 %d 条核心业务指标。从数据分布来看，头部指标具有明显的集中效应，建议重点跟进前排高贡献度的核心维度，优化资源倾斜并提升整体投资回报率。".formatted(rows.size());
        }

        String dataSummarySnippet = rows.size() > 5 ? rows.subList(0, 5).toString() + "...(共" + rows.size() + "条)" : rows.toString();

        String promptText = """
            你是一名资深商业数据分析总监。根据以下用户的提问、生成的SQL和实际执行返回的业务数据样本，用清晰专业、客观精炼的中文写出 2~3 句核心商业洞察与经营决策建议。
            
            【业务提问】: %s
            【执行SQL】: %s
            【实际数据】: %s
            
            请直接输出 2~3 句话的商业洞察建议：
            """.formatted(prompt, sql, dataSummarySnippet);

        try {
            ChatClient chatClient = statelessChatClientProvider.getObject();
            String insight = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();
            if (insight != null && !insight.isBlank()) {
                return insight.trim();
            }
        } catch (Exception e) {
            log.warn("[Spring AI Insight 生成失败，采用本地规则提炼] error={}", e.getMessage());
        }

        // 本地规则洞察兜底
        return "本次查询共汇总 %d 条核心业务指标。从数据分布来看，头部指标具有明显的集中效应，建议重点跟进前排高贡献度的核心维度，优化资源倾斜并提升整体投资回报率。".formatted(rows.size());
    }

    /**
     * 离线降级 SQL 启发式规则匹配
     */
    private String fallbackSqlHeuristic(String prompt) {
        String p = prompt.toLowerCase();
        if (p.contains("大区") && (p.contains("销售") || p.contains("利润") || p.contains("额"))) {
            return "SELECT d.region AS 大区, SUM(o.total_amount) AS 总销售额, SUM(o.profit_amount) AS 总利润 FROM fact_order o JOIN dim_department d ON o.dept_id = d.id WHERE o.order_status = 'COMPLETED' GROUP BY d.region ORDER BY 总销售额 DESC LIMIT 100;";
        }
        if (p.contains("退款") || p.contains("售后")) {
            return "SELECT p.category AS 产品品类, COUNT(r.id) AS 退款笔数, SUM(r.refund_amount) AS 退款总额 FROM fact_refund r JOIN fact_order o ON r.order_id = o.id JOIN dim_product p ON o.product_id = p.id WHERE r.refund_status = 'APPROVED' GROUP BY p.category ORDER BY 退款总额 DESC LIMIT 100;";
        }
        if (p.contains("客户") || p.contains("vip") || p.contains("买得最多")) {
            return "SELECT c.customer_name AS 客户名称, c.region AS 大区, c.tier AS 客户等级, SUM(o.total_amount) AS 累计消费额 FROM fact_order o JOIN dim_customer c ON o.customer_id = c.id WHERE o.order_status = 'COMPLETED' GROUP BY c.id, c.customer_name, c.region, c.tier ORDER BY 累计消费额 DESC LIMIT 10;";
        }
        if (p.contains("产品") || p.contains("商品") || p.contains("畅销")) {
            return "SELECT p.product_name AS 产品名称, p.category AS 品类, SUM(o.quantity) AS 销售总数量, SUM(o.total_amount) AS 销售总额 FROM fact_order o JOIN dim_product p ON o.product_id = p.id WHERE o.order_status = 'COMPLETED' GROUP BY p.id, p.product_name, p.category ORDER BY 销售总额 DESC LIMIT 10;";
        }
        // 默认通用分析
        return "SELECT d.dept_name AS 销售部门, COUNT(o.id) AS 成交订单量, SUM(o.total_amount) AS 业绩总额 FROM fact_order o JOIN dim_department d ON o.dept_id = d.id WHERE o.order_status = 'COMPLETED' GROUP BY d.dept_name ORDER BY 业绩总额 DESC LIMIT 100;";
    }
}
