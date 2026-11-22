package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.dto.TableSchemaVO;
import com.pulseos.datapulse.service.SchemaCatalogService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaCatalogService schemaCatalogService;

    @GetMapping("/tables")
    public Result<List<TableSchemaVO>> listTables(@RequestParam(defaultValue = "1") Long datasourceId) {
        return Result.success(schemaCatalogService.listWarehouseTables(datasourceId));
    }

    @GetMapping("/preview/{tableName}")
    public Result<Map<String, Object>> preview(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") Long datasourceId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(schemaCatalogService.previewTableData(datasourceId, tableName, limit));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/table/config")
    public Result<Boolean> updateConfig(@RequestBody UpdateTableConfigRequest req) {
        schemaCatalogService.updateTableConfig(
                req.getDatasourceId() != null ? req.getDatasourceId() : 1L,
                req.getTableName(),
                Boolean.TRUE.equals(req.getIsWhitelisted()),
                req.getTableComment(),
                req.getCustomRules()
        );
        return Result.success(true);
    }

    @Data
    public static class UpdateTableConfigRequest {
        private Long datasourceId = 1L;
        private String tableName;
        private Boolean isWhitelisted = true;
        private String tableComment;
        private String customRules;
    }
}
