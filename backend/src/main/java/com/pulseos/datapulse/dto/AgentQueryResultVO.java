package com.pulseos.datapulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 自然语言取数与可视化分析响应结果视图对象（VO）。
 * <p>
 * 一次 Text-to-SQL 查询完成后返回给前端工作台的全部信息：生成的 SQL、AST 安全审计结论、
 * 执行耗时、结果集列名与明细行、ECharts 推荐图表配置、AI 商业洞察摘要以及缓存/Few-Shot 命中状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentQueryResultVO {
    /** 落库后的审计历史记录主键 ID */
    private Long historyId;
    /** 用户原始提问文本 */
    private String userPrompt;
    /** 最终经过沙箱净化并实际执行的只读 SQL */
    private String generatedSql;
    /** 安全审计状态：1 表示安全放行，0 表示被 AST 沙箱拦截 */
    private Integer isSafe;
    /** 安全放行说明或拦截原因 */
    private String safetyReason;
    /** 从开始查库到出结果的总耗时（毫秒） */
    private Long executionTimeMs;
    /** 查询结果的有序字段列名列表 */
    private List<String> columns;
    /** 查询结果的明细数据行列表 */
    private List<Map<String, Object>> rows;
    /** 实际返回的行数 */
    private Integer rowCount;
    /** 自动推断或用户指定的图表类型（bar/line/pie/table 等） */
    private String chartType;
    /** 后端预拼装的 ECharts 图表配置项（含 xAxis/series 等） */
    private Map<String, Object> chartOptions;
    /** 大模型基于真实数据生成的中文业务洞察摘要 */
    private String insightSummary;
    /** 是否命中 Redis 语义缓存（true 表示秒级复用，未消耗大模型生成 SQL） */
    private Boolean cacheHit;
    /** 本次查询命中的目标数据源 ID */
    private Long datasourceId;
    /** 本次查询命中的目标数据源展示名称 */
    private String datasourceName;
    /** 本次 Text-to-SQL 自动检索并参考的 Few-Shot 范例问题列表 */
    private List<String> fewShotQuestions;
}
