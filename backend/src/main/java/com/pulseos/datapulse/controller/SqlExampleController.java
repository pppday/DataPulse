package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.entity.SysSqlExample;
import com.pulseos.datapulse.service.SqlExampleRetrievalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Few-Shot 参考 SQL 知识库管理控制器。
 * <p>
 * 用于沉淀经过人工审核的高质量「自然语言问题 -> 标准 SQL」示范案例；
 * 当用户提问时，Text-to-SQL 引擎会自动从这里检索最相似的范例塞进大模型提示词中，显著提高复杂指标 SQL 的一次准确率。
 * 写操作（新增、一键沉淀历史查询、删除）仅限管理员（{@code ROLE_ADMIN}）。
 */
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class SqlExampleController {

    private final SqlExampleRetrievalService sqlExampleRetrievalService;

    /** 查看指定数据源下已沉淀的全部参考 SQL 范例列表 */
    @GetMapping
    public Result<List<SysSqlExample>> list(@RequestParam(defaultValue = "1") Long datasourceId) {
        return Result.success(sqlExampleRetrievalService.listByDatasource(datasourceId));
    }

    /** 手工录入一条高质量 Few-Shot 参考 SQL 范例并自动计算语义向量（仅管理员） */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<SysSqlExample> add(@RequestBody CreateSqlExampleRequest req) {
        return Result.success(sqlExampleRetrievalService.add(
                req.getDatasourceId() != null ? req.getDatasourceId() : 1L,
                req.getQuestion(),
                req.getSqlText()
        ));
    }

    /** 将某条已验证正确的历史查询记录一键沉淀为官方参考 SQL 范例（仅管理员） */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/from-history/{historyId}")
    public Result<SysSqlExample> addFromHistory(
            @PathVariable Long historyId,
            @RequestParam(defaultValue = "1") Long datasourceId) {
        return Result.success(sqlExampleRetrievalService.addFromHistory(historyId, datasourceId));
    }

    /** 删除指定 ID 的参考 SQL 范例（仅管理员） */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        sqlExampleRetrievalService.delete(id);
        return Result.success(true);
    }

    /** 新增参考 SQL 范例的请求载荷 */
    @Data
    public static class CreateSqlExampleRequest {
        private Long datasourceId = 1L;
        private String question;
        private String sqlText;
    }
}
