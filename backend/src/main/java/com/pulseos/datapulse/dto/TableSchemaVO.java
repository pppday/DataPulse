package com.pulseos.datapulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据表元数据与语义治理配置视图对象（VO）。
 * <p>
 * 整合底层数据库读取到的表结构（表名、行数、各列类型/注释/主键标识）以及管理员维护的白名单开关和业务计算口径规则，
 * 用于在前端「数据资产目录」页展示，也作为构建 Text-to-SQL 提示词的权威结构依据。
 */
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

    /** 单列字段的元数据描述（字段名、数据类型、字段注释、是否主键） */
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
