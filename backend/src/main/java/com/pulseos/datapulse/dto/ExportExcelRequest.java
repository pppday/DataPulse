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
public class ExportExcelRequest {
    /**
     * 导出的目标文件名（不含或含扩展名，如：DataPulse_商品销量排行）
     */
    private String fileName;

    /**
     * 主数据工作表名称（默认：数据明细）
     */
    private String sheetName;

    /**
     * 列头字段列表（按前端或SQL定义的顺序展示）
     */
    private List<String> columns;

    /**
     * 明细行数据列表（Map<字段名, 字段值>）
     */
    private List<Map<String, Object>> rows;

    /**
     * 大模型商业决策洞察摘要（写入分析报告工作表）
     */
    private String insightSummary;

    /**
     * 数据源名称（如：电商核心数仓）
     */
    private String datasourceName;

    /**
     * 底层经由 AST 安全审查并执行的 SQL 语句
     */
    private String generatedSql;

    /**
     * 执行耗时 (ms)
     */
    private Long executionTimeMs;

    /**
     * 是否命中 Redis 语义缓存
     */
    private Boolean cacheHit;

    /**
     * 可选：若客户端未传明细 rows，则可传入 prompt 或 customSql，由后端自动执行后再流式导出
     */
    private String prompt;
    private String customSql;
    private Long datasourceId;
}
