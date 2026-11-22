package com.pulseos.datapulse.service.impl;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.pulseos.datapulse.config.MybatisPlusConfig;
import com.pulseos.datapulse.service.SqlSafetySandboxService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlSafetySandboxServiceImplTest {

    private final SqlSafetySandboxServiceImpl sandbox = new SqlSafetySandboxServiceImpl();

    @Test
    void unionQueryGetsLimit1000Injected() {
        String sql = "SELECT customer_name FROM dim_customer UNION SELECT product_name FROM dim_product";
        SqlSafetySandboxService.SafetyCheckResult res = sandbox.inspectAndSanitize(sql);

        assertTrue(res.isSafe(), "UNION query on whitelisted tables should be safe");
        assertTrue(res.getSanitizedSql().toUpperCase().contains("LIMIT 1000"),
                "UNION query must have LIMIT 1000 injected: " + res.getSanitizedSql());
    }

    @Test
    void excessiveLimitIsCappedTo1000() {
        String sql = "SELECT order_no, total_amount FROM fact_order LIMIT 5000";
        SqlSafetySandboxService.SafetyCheckResult res = sandbox.inspectAndSanitize(sql);

        assertTrue(res.isSafe());
        assertTrue(res.getSanitizedSql().toUpperCase().contains("LIMIT 1000"));
        assertFalse(res.getSanitizedSql().contains("5000"));
    }

    @Test
    void deleteStatementIsBlocked() {
        String sql = "DELETE FROM fact_order WHERE id = 1";
        SqlSafetySandboxService.SafetyCheckResult res = sandbox.inspectAndSanitize(sql);

        assertFalse(res.isSafe());
        assertTrue(res.getReason().contains("拦截"));
    }

    @Test
    void sensitiveSysUserTableIsBlocked() {
        String sql = "SELECT username, password FROM sys_user";
        SqlSafetySandboxService.SafetyCheckResult res = sandbox.inspectAndSanitize(sql);

        assertFalse(res.isSafe());
        assertTrue(res.getReason().contains("sys_user"));
    }

    @Test
    void nonWhitelistedTableIsBlocked() {
        String sql = "SELECT * FROM sys_sql_example";
        SqlSafetySandboxService.SafetyCheckResult res = sandbox.inspectAndSanitize(sql);

        assertFalse(res.isSafe());
        assertTrue(res.getReason().contains("白名单"));
    }

    @Test
    void plainSelectGetsLimit1000Injected() {
        String sql = "SELECT d.region AS 销售大区, SUM(o.total_amount) AS 有效销售额 " +
                "FROM fact_order o JOIN dim_department d ON o.dept_id = d.id GROUP BY d.region";
        SqlSafetySandboxService.SafetyCheckResult res = sandbox.inspectAndSanitize(sql);

        assertTrue(res.isSafe());
        assertTrue(res.getSanitizedSql().toUpperCase().contains("LIMIT 1000"));
    }

    @Test
    void mybatisPlusPaginationInterceptorInitializesCleanly() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();
        assertNotNull(interceptor);
        assertEquals(1, interceptor.getInterceptors().size());
    }

    @Test
    void withCteQueryGetsLimit1000Injected() {
        String sql = "WITH cte AS (SELECT id, total_amount FROM fact_order) SELECT * FROM cte";
        SqlSafetySandboxService.SafetyCheckResult res = sandbox.inspectAndSanitize(sql);

        assertTrue(res.isSafe(), "WITH CTE on whitelisted table should pass: " + res.getReason());
        assertTrue(res.getSanitizedSql().toUpperCase().contains("LIMIT 1000"),
                "WITH CTE should have LIMIT 1000 injected: " + res.getSanitizedSql());
    }
}
