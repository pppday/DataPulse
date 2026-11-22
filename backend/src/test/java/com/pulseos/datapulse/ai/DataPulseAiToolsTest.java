package com.pulseos.datapulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseos.datapulse.service.DynamicDataSourceService;
import com.pulseos.datapulse.service.SchemaCatalogService;
import com.pulseos.datapulse.service.SqlExampleRetrievalService;
import com.pulseos.datapulse.service.impl.SqlSafetySandboxServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DataPulseAiToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validateAndExecuteBlocksDeleteAndSysUser() throws Exception {
        SchemaCatalogService catalog = mock(SchemaCatalogService.class);
        SqlExampleRetrievalService retrieval = mock(SqlExampleRetrievalService.class);
        DynamicDataSourceService dsService = mock(DynamicDataSourceService.class);
        when(catalog.getWhitelistedTables(1L)).thenReturn(Set.of("fact_order", "dim_department"));

        DataPulseAiTools tools = new DataPulseAiTools(
                catalog, retrieval, new SqlSafetySandboxServiceImpl(), dsService, objectMapper
        );

        JsonNode delNode = objectMapper.readTree(
                tools.validateAndExecuteReadOnlySql(1L, "DELETE FROM fact_order WHERE id = 1")
        );
        assertFalse(delNode.get("safe").asBoolean());
        assertTrue(delNode.get("blockedReason").asText().contains("拦截"));

        JsonNode sysNode = objectMapper.readTree(
                tools.validateAndExecuteReadOnlySql(1L, "SELECT username, password FROM sys_user")
        );
        assertFalse(sysNode.get("safe").asBoolean());
        assertTrue(sysNode.get("blockedReason").asText().contains("sys_user"));

        verifyNoInteractions(dsService);
    }

    @Test
    void validateAndExecuteAllowsSafeSelectAndReturnsPreviewJson() throws Exception {
        SchemaCatalogService catalog = mock(SchemaCatalogService.class);
        SqlExampleRetrievalService retrieval = mock(SqlExampleRetrievalService.class);
        DynamicDataSourceService dsService = mock(DynamicDataSourceService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        when(catalog.getWhitelistedTables(1L)).thenReturn(Set.of("fact_order", "dim_department"));
        when(dsService.getJdbcTemplate(1L)).thenReturn(jdbcTemplate);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("大区", "华东", "有效销售额", 240000.0)
        ));

        DataPulseAiTools tools = new DataPulseAiTools(
                catalog, retrieval, new SqlSafetySandboxServiceImpl(), dsService, objectMapper
        );

        String out = tools.validateAndExecuteReadOnlySql(
                1L, "SELECT d.region AS 大区, SUM(o.total_amount) AS 有效销售额 FROM fact_order o JOIN dim_department d ON o.dept_id = d.id GROUP BY d.region"
        );
        JsonNode node = objectMapper.readTree(out);
        assertTrue(node.get("safe").asBoolean());
        assertEquals(1, node.get("totalRowCount").asInt());
        assertTrue(node.get("sanitizedSql").asText().toUpperCase().contains("LIMIT 1000"));
        assertEquals("华东", node.get("previewRows").get(0).get("大区").asText());
    }
}
