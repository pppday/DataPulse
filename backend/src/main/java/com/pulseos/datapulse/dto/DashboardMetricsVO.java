package com.pulseos.datapulse.dto;

import com.pulseos.datapulse.entity.QueryHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 平台运营驾驶舱核心统计指标视图对象（VO）。
 * <p>
 * 汇总全站自然语言查库总次数、AST 安全沙箱放行率与拦截次数、平均响应耗时、
 * 数仓纳管表数量与总数据量，以及最近查询审计动态列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsVO {
    /** 平台累计自然语言取数总次数 */
    private Long totalQueries;
    /** 经 AST 安全沙箱校验放行的查询次数 */
    private Long totalSafePass;
    /** 被 AST 安全沙箱拦截的高危或越权查询次数 */
    private Long totalIntercepted;
    /** 安全合规放行率（百分比，如 98.5） */
    private Double safePassRate;
    /** 全部查询的平均执行耗时（毫秒） */
    private Double avgExecutionTimeMs;
    /** 默认数仓当前纳管的业务表总数 */
    private Integer warehouseTablesCount;
    /** 默认数仓所有业务表的估算总行数 */
    private Long warehouseTotalRows;
    /** 最近一段时间的查询审计流水记录 */
    private List<QueryHistory> recentQueries;
}
