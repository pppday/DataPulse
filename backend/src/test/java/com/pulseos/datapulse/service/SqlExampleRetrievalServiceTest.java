package com.pulseos.datapulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseos.datapulse.entity.SysSqlExample;
import com.pulseos.datapulse.mapper.QueryHistoryMapper;
import com.pulseos.datapulse.mapper.SysSqlExampleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqlExampleRetrievalServiceTest {

    @Test
    void cosineHandlesIdenticalOrthogonalAndOppositeVectors() {
        float[] a = {1f, 0f, 0f};
        float[] b = {1f, 0f, 0f};
        float[] c = {0f, 1f, 0f};
        float[] d = {-1f, 0f, 0f};

        assertEquals(1.0, SqlExampleRetrievalService.cosine(a, b), 1e-6);
        assertEquals(0.0, SqlExampleRetrievalService.cosine(a, c), 1e-6);
        assertEquals(-1.0, SqlExampleRetrievalService.cosine(a, d), 1e-6);
    }

    @Test
    void bigramJaccardRanksRelevantQuestionHigher() {
        String q = "统计华东大区有效销售额排行";
        String relevant = "统计2026年各销售大区的有效销售额与毛利润排行榜";
        String unrelated = "统计各支付渠道成交订单笔数占比";

        double sRel = SqlExampleRetrievalService.bigramJaccard(q, relevant);
        double sUnrel = SqlExampleRetrievalService.bigramJaccard(q, unrelated);

        assertTrue(sRel > sUnrel, "Relevant example score (" + sRel + ") should exceed unrelated (" + sUnrel + ")");
        assertTrue(sRel >= 0.15);
    }

    @Test
    void retrieveTopKFallbackRanksByBigramSimilarity() {
        SysSqlExampleMapper exampleMapper = mock(SysSqlExampleMapper.class);
        QueryHistoryMapper historyMapper = mock(QueryHistoryMapper.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        SqlSafetySandboxService sandbox = mock(SqlSafetySandboxService.class);
        SchemaCatalogService catalog = mock(SchemaCatalogService.class);

        when(embeddingClient.isEnabled()).thenReturn(false);
        when(exampleMapper.selectList(any())).thenReturn(List.of(
                SysSqlExample.builder().id(1L).datasourceId(1L)
                        .question("统计各支付渠道的成交订单笔数").sqlText("SELECT 1;").build(),
                SysSqlExample.builder().id(2L).datasourceId(1L)
                        .question("统计2026年各销售大区的有效销售额与毛利润排行榜").sqlText("SELECT 2;").build(),
                SysSqlExample.builder().id(3L).datasourceId(1L)
                        .question("分析各产品品类的退款金额与退款率").sqlText("SELECT 3;").build()
        ));

        SqlExampleRetrievalService svc = new SqlExampleRetrievalService(
                exampleMapper, historyMapper, embeddingClient, sandbox, catalog, new ObjectMapper()
        );

        List<SysSqlExample> top = svc.retrieveTopK(1L, "查询各个销售大区有效销售额排行", 2);
        assertFalse(top.isEmpty());
        assertEquals(2L, top.get(0).getId());
    }
}
