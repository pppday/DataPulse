package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.DashboardMetricsVO;

/** 运营监控大盘核心指标聚合服务接口。 */
public interface DashboardService {
    DashboardMetricsVO getMetrics();
}
