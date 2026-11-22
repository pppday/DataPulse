package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.entity.SysDatasource;
import com.pulseos.datapulse.service.DynamicDataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/datasource")
@RequiredArgsConstructor
public class DatasourceController {

    private final DynamicDataSourceService dynamicDataSourceService;

    @GetMapping("/list")
    public Result<List<SysDatasource>> list() {
        return Result.success(dynamicDataSourceService.listAll());
    }

    @PostMapping("/test")
    public Result<Boolean> test(@RequestBody SysDatasource ds) {
        return Result.success(dynamicDataSourceService.testConnection(ds));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public Result<SysDatasource> save(@RequestBody SysDatasource ds) {
        return Result.success(dynamicDataSourceService.saveDatasource(ds));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/default")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        dynamicDataSourceService.setDefaultDatasource(id);
        return Result.success(true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        dynamicDataSourceService.deleteDatasource(id);
        return Result.success(true);
    }
}
