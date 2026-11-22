package com.pulseos.datapulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSchemaVO {
    private String tableName;
    private String tableComment;
    private Long rowCount;
    private Boolean isWhitelisted;
    private String customPromptRules;
    private List<ColumnMeta> columns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnMeta {
        private String columnName;
        private String columnType;
        private String columnComment;
        private Boolean isPrimaryKey;
    }
}
