package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.TableSchemaVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SchemaCatalogService {
    String getWarehouseSchemaPrompt(Long datasourceId);
    List<TableSchemaVO> listWarehouseTables(Long datasourceId);
    Map<String, Object> previewTableData(Long datasourceId, String tableName, int limit);
    void updateTableConfig(Long datasourceId, String tableName, boolean isWhitelisted, String tableComment, String customRules);
    Set<String> getWhitelistedTables(Long datasourceId);
}
