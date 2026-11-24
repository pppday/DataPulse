package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.dto.DashboardMetricsVO;
import com.pulseos.datapulse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经营大盘与平台运行看板数据控制器。
 * <p>
 * 供前端首页驾驶舱定时拉取实时 GMV、订单量、品类销售、区域分布以及自然语言取数平台自身的查询统计指标。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 获取首页经营总览大盘的全部聚合指标与多维图表序列 */
    @GetMapping("/metrics")
    public Result<DashboardMetricsVO> getMetrics() {
        return Result.success(dashboardService.getMetrics());
    }
}
