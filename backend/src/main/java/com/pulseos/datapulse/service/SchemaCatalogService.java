package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.TableSchemaVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 动态数据源表结构扫描、数据预览、白名单管理与 Schema Prompt 生成服务接口。 */
public interface SchemaCatalogService {
    String getWarehouseSchemaPrompt(Long datasourceId);
    List<TableSchemaVO> listWarehouseTables(Long datasourceId);
    Map<String, Object> previewTableData(Long datasourceId, String tableName, int limit);
    void updateTableConfig(Long datasourceId, String tableName, boolean isWhitelisted, String tableComment, String customRules);
    Set<String> getWhitelistedTables(Long datasourceId);
}
