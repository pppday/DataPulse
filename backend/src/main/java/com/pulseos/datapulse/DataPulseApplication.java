package com.pulseos.datapulse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DataPulse 企业级 Text-to-SQL 自然语言智能取数与可视化平台后端启动入口。
 * <p>
 * 负责启动 Spring Boot 容器并扫描 MyBatis-Plus 的 Mapper 接口（{@code com.pulseos.datapulse.mapper}），
 * 向上承载自然语言查库编排、SQL 安全沙箱、动态多数据源路由、看板指标与 Excel 导出等全部后端能力。
 */
@SpringBootApplication
@MapperScan("com.pulseos.datapulse.mapper")
public class DataPulseApplication {

    /**
     * 应用主启动方法：初始化 Spring 上下文并监听配置端口（默认 8090）。
     */
    public static void main(String[] args) {
        SpringApplication.run(DataPulseApplication.class, args);
        System.out.println("============================================================");
        System.out.println("[INFO] DataPulse Enterprise Text-to-SQL Agent Platform Online!");
        System.out.println("[INFO] Local MySQL 8.0 Connected (datapulse_db:3306)");
        System.out.println("[INFO] Server listening on port 8090");
        System.out.println("============================================================");
    }
}
