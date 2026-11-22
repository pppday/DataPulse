package com.pulseos.datapulse.dto;

import com.pulseos.datapulse.entity.QueryHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsVO {
    private Long totalQueries;
    private Long totalSafePass;
    private Long totalIntercepted;
    private Double safePassRate;
    private Double avgExecutionTimeMs;
    private Integer warehouseTablesCount;
    private Long warehouseTotalRows;
    private List<QueryHistory> recentQueries;
}
