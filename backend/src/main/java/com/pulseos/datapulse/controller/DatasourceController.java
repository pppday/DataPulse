package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.entity.SysDatasource;
import com.pulseos.datapulse.service.DynamicDataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 动态多数据源管理控制器。
 * <p>
 * 负责管理平台接入的外部只读业务数据库（支持 MySQL 与 PostgreSQL）：
 * 普通分析师可查看列表并测试连通性，但新增/修改、切换默认库、删除数据源等高危变更
 * 统一通过 {@code @PreAuthorize("hasRole('ADMIN')")} 严格限定仅管理员可操作。
 */
@RestController
@RequestMapping("/api/v1/datasource")
@RequiredArgsConstructor
public class DatasourceController {

    private final DynamicDataSourceService dynamicDataSourceService;

    /** 查询平台已纳管的全部数据源列表（密码字段在业务层已脱敏处理） */
    @GetMapping("/list")
    public Result<List<SysDatasource>> list() {
        return Result.success(dynamicDataSourceService.listAll());
    }

    /** 实时建立临时 JDBC 连接执行 {@code SELECT 1} 探测目标数据库是否连通 */
    @PostMapping("/test")
    public Result<Boolean> test(@RequestBody SysDatasource ds) {
        return Result.success(dynamicDataSourceService.testConnection(ds));
    }

    /** 新增或更新数据源配置，同时刷新对应 HikariCP 连接池缓存（仅管理员） */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public Result<SysDatasource> save(@RequestBody SysDatasource ds) {
        return Result.success(dynamicDataSourceService.saveDatasource(ds));
    }

    /** 将指定数据源设为平台默认取数目标库（仅管理员） */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/default")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        dynamicDataSourceService.setDefaultDatasource(id);
        return Result.success(true);
    }

    /** 删除指定数据源并回收其底层 HikariCP 连接池资源（仅管理员，内置 1 号主库受保护不可删） */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        dynamicDataSourceService.deleteDatasource(id);
        return Result.success(true);
    }
}
