package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务健康检查探针控制器（公开白名单接口，无需 JWT）。
 * <p>
 * 供 Docker 容器健康检查、Nginx 反向代理探活以及前端状态指示灯检测后端服务是否正常运行。
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /** 返回服务在线状态、名称、版本号与当前服务器时间戳 */
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "DataPulse Enterprise Agent Middle Platform");
        data.put("version", "1.0.0");
        data.put("timestamp", LocalDateTime.now());
        return Result.success(data);
    }
}
