package com.pulseos.datapulse.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

public interface SqlSafetySandboxService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SafetyCheckResult {
        private boolean safe;
        private String reason;
        private String sanitizedSql;
    }

    SafetyCheckResult inspectAndSanitize(String rawSql, Set<String> allowedTables, String dbType);
    SafetyCheckResult inspectAndSanitize(String rawSql, Set<String> allowedTables);
    SafetyCheckResult inspectAndSanitize(String rawSql);
}
