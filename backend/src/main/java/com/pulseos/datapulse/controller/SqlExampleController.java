package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.entity.SysSqlExample;
import com.pulseos.datapulse.service.SqlExampleRetrievalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class SqlExampleController {

    private final SqlExampleRetrievalService sqlExampleRetrievalService;

    @GetMapping
    public Result<List<SysSqlExample>> list(@RequestParam(defaultValue = "1") Long datasourceId) {
        return Result.success(sqlExampleRetrievalService.listByDatasource(datasourceId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<SysSqlExample> add(@RequestBody CreateSqlExampleRequest req) {
        return Result.success(sqlExampleRetrievalService.add(
                req.getDatasourceId() != null ? req.getDatasourceId() : 1L,
                req.getQuestion(),
                req.getSqlText()
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/from-history/{historyId}")
    public Result<SysSqlExample> addFromHistory(
            @PathVariable Long historyId,
            @RequestParam(defaultValue = "1") Long datasourceId) {
        return Result.success(sqlExampleRetrievalService.addFromHistory(historyId, datasourceId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        sqlExampleRetrievalService.delete(id);
        return Result.success(true);
    }

    @Data
    public static class CreateSqlExampleRequest {
        private Long datasourceId = 1L;
        private String question;
        private String sqlText;
    }
}
