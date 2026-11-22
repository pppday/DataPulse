package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

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
