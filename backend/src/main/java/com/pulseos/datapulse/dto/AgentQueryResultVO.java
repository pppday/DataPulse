package com.pulseos.datapulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentQueryResultVO {
    private Long historyId;
    private String userPrompt;
    private String generatedSql;
    private Integer isSafe;
    private String safetyReason;
    private Long executionTimeMs;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private Integer rowCount;
    private String chartType;
    private Map<String, Object> chartOptions;
    private String insightSummary;
    private Boolean cacheHit;
    private Long datasourceId;
    private String datasourceName;
    private List<String> fewShotQuestions;
}
