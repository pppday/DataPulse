package com.pulseos.datapulse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pulseos.datapulse.dto.DashboardMetricsVO;
import com.pulseos.datapulse.entity.Customer;
import com.pulseos.datapulse.entity.Department;
import com.pulseos.datapulse.entity.Order;
import com.pulseos.datapulse.entity.Product;
import com.pulseos.datapulse.entity.QueryHistory;
import com.pulseos.datapulse.entity.Refund;
import com.pulseos.datapulse.mapper.CustomerMapper;
import com.pulseos.datapulse.mapper.DepartmentMapper;
import com.pulseos.datapulse.mapper.OrderMapper;
import com.pulseos.datapulse.mapper.ProductMapper;
import com.pulseos.datapulse.mapper.QueryHistoryMapper;
import com.pulseos.datapulse.mapper.RefundMapper;
import com.pulseos.datapulse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 【运营监控大盘指标聚合服务】
 * 统计平台累计查询次数、SQL 安全沙箱通过率与拦截数、平均查询耗时、核心数仓总行数以及最近 8 条查询审计明细。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final QueryHistoryMapper queryHistoryMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final DepartmentMapper departmentMapper;
    private final OrderMapper orderMapper;
    private final RefundMapper refundMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public DashboardMetricsVO getMetrics() {
        Long totalQueries = queryHistoryMapper.selectCount(null);
        Long totalSafePass = queryHistoryMapper.selectCount(
                new LambdaQueryWrapper<QueryHistory>().eq(QueryHistory::getIsSafe, 1)
        );
        Long totalIntercepted = queryHistoryMapper.selectCount(
                new LambdaQueryWrapper<QueryHistory>().eq(QueryHistory::getIsSafe, 0)
        );

        double safePassRate = 100.0;
        if (totalQueries != null && totalQueries > 0) {
            safePassRate = BigDecimal.valueOf((double) totalSafePass / totalQueries * 100.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Double avgLatency = 0.0;
        try {
            Double val = jdbcTemplate.queryForObject(
                    "SELECT IFNULL(AVG(execution_time_ms), 0) FROM sys_query_history WHERE is_safe = 1",
                    Double.class
            );
            if (val != null) {
                avgLatency = BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
            }
        } catch (Exception ignored) {
        }

        long warehouseTotalRows = 0L;
        warehouseTotalRows += customerMapper.selectCount(null);
        warehouseTotalRows += productMapper.selectCount(null);
        warehouseTotalRows += departmentMapper.selectCount(null);
        warehouseTotalRows += orderMapper.selectCount(null);
        warehouseTotalRows += refundMapper.selectCount(null);

        List<QueryHistory> recent = queryHistoryMapper.selectRecentHistoryWithUser(8);

        return DashboardMetricsVO.builder()
                .totalQueries(totalQueries)
                .totalSafePass(totalSafePass)
                .totalIntercepted(totalIntercepted)
                .safePassRate(safePassRate)
                .avgExecutionTimeMs(avgLatency)
                .warehouseTablesCount(5)
                .warehouseTotalRows(warehouseTotalRows)
                .recentQueries(recent)
                .build();
    }
}
