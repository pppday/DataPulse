package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.dto.TableSchemaVO;
import com.pulseos.datapulse.service.SchemaCatalogService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据资产元数据目录与语义治理控制器。
 * <p>
 * 负责向前端展示目标数据源下所有业务表的字段结构、抽样预览数据，
 * 并允许管理员配置表的授权白名单开关、中文业务备注以及专属计算口径规则（直接影响 Text-to-SQL 提示词与沙箱放行范围）。
 */
@RestController
@RequestMapping("/api/v1/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaCatalogService schemaCatalogService;

    /** 获取指定数据源下所有业务表的元数据详情（字段类型、注释、白名单状态、业务口径规则） */
    @GetMapping("/tables")
    public Result<List<TableSchemaVO>> listTables(@RequestParam(defaultValue = "1") Long datasourceId) {
        return Result.success(schemaCatalogService.listWarehouseTables(datasourceId));
    }

    /** 对指定白名单业务表进行前 N 行只读抽样预览，方便用户直观了解数据样例 */
    @GetMapping("/preview/{tableName}")
    public Result<Map<String, Object>> preview(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") Long datasourceId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(schemaCatalogService.previewTableData(datasourceId, tableName, limit));
    }

    /** 更新某张表的白名单授权状态、业务中文注释与计算口径规则（仅管理员，修改后会联动清空相关语意缓存） */
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

    /** 更新表级治理配置的请求载荷 */
    @Data
    public static class UpdateTableConfigRequest {
        private Long datasourceId = 1L;
        private String tableName;
        private Boolean isWhitelisted = true;
        private String tableComment;
        private String customRules;
    }
}
