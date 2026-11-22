package com.pulseos.datapulse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.pulseos.datapulse.mapper")
public class DataPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataPulseApplication.class, args);
        System.out.println("============================================================");
        System.out.println("[INFO] DataPulse Enterprise Text-to-SQL Agent Platform Online!");
        System.out.println("[INFO] Local MySQL 8.0 Connected (datapulse_db:3306)");
        System.out.println("[INFO] Server listening on port 8090");
        System.out.println("============================================================");
    }
}
