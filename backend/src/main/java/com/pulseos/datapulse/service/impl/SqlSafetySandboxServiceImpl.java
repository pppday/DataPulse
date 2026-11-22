package com.pulseos.datapulse.service.impl;

import com.pulseos.datapulse.service.SqlSafetySandboxService;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class SqlSafetySandboxServiceImpl implements SqlSafetySandboxService {

    private static final Logger log = LoggerFactory.getLogger(SqlSafetySandboxServiceImpl.class);

    private static final Set<String> ALLOWED_TABLES = Set.of(
            "dim_customer",
            "dim_product",
            "dim_department",
            "fact_order",
            "fact_refund"
    );

    private static final Set<String> SENSITIVE_TABLES = Set.of(
            "sys_user",
            "sys_query_history",
            "mysql",
            "information_schema",
            "performance_schema"
    );

    @Override
    public SafetyCheckResult inspectAndSanitize(String rawSql) {
        return inspectAndSanitize(rawSql, ALLOWED_TABLES);
    }

    @Override
    public SafetyCheckResult inspectAndSanitize(String rawSql, Set<String> allowedTables) {
        Set<String> effectiveAllowedTables = (allowedTables != null && !allowedTables.isEmpty())
                ? allowedTables : ALLOWED_TABLES;

        if (rawSql == null || rawSql.isBlank()) {
            return SafetyCheckResult.builder()
                    .safe(false)
                    .reason("SQL语句为空")
                    .sanitizedSql("")
                    .build();
        }

        // 1. 基础清理：去除 markdown 包裹符 ```sql ... ```
        String cleanSql = rawSql.trim();
        cleanSql = cleanSql.replaceAll("^```sql\\s*", "")
                           .replaceAll("^```\\s*", "")
                           .replaceAll("\\s*```$", "")
                           .trim();

        // 去除末尾分号以便 AST 解析
        if (cleanSql.endsWith(";")) {
            cleanSql = cleanSql.substring(0, cleanSql.length() - 1).trim();
        }

        // 规范化未用反引号包裹的中文列别名，防止 JSqlParser Lexer 抛出 TokenMgrException
        cleanSql = normalizeChineseAliases(cleanSql);

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(cleanSql);
        } catch (Exception e) {
            log.warn("[SQL AST 语法解析失败] sql={}, error={}", cleanSql, e.getMessage());
            String reasonMsg;
            String upper = cleanSql.toUpperCase();
            if (!upper.startsWith("SELECT") && !upper.startsWith("WITH") && !upper.startsWith("SHOW") && !upper.startsWith("EXPLAIN")) {
                reasonMsg = "【非数据查询请求】输入内容未能推导出标准只读 SQL，请提出具体的业务分析疑问（例如：统计各大区销售额、各品类退款率等）";
            } else {
                String rawMsg = e.getMessage() != null ? e.getMessage() : "语法非标";
                if (rawMsg.length() > 120) {
                    rawMsg = rawMsg.substring(0, 120) + "...";
                }
                reasonMsg = "SQL 语法树解析未通过: " + rawMsg;
            }
            return SafetyCheckResult.builder()
                    .safe(false)
                    .reason(reasonMsg)
                    .sanitizedSql(cleanSql)
                    .build();
        }

        // 2. 检查语句类型：必须且只能是 SELECT 查询
        if (!(statement instanceof Select select)) {
            String stmtType = statement.getClass().getSimpleName();
            log.warn("[SQL 安全沙箱阻断] 拦截到非 SELECT 危险语句类型: {}", stmtType);
            return SafetyCheckResult.builder()
                    .safe(false)
                    .reason("【安全沙箱拦截】严格禁止执行任何新增、修改、删除或DDL指令 (发现 " + stmtType + ")")
                    .sanitizedSql(cleanSql)
                    .build();
        }

        // 3. 表白名单与敏感表隔离检查
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        Set<String> tableList = tablesNamesFinder.getTables(statement);

        for (String tbl : tableList) {
            String lowerTbl = tbl.toLowerCase(Locale.ROOT).replace("`", "");
            if (SENSITIVE_TABLES.contains(lowerTbl)) {
                log.warn("[SQL 安全沙箱阻断] 拦截到尝试访问敏感系统表: {}", lowerTbl);
                return SafetyCheckResult.builder()
                        .safe(false)
                        .reason("【安全沙箱拦截】禁止通过智能Agent查询系统用户与审计敏感表: `" + lowerTbl + "`")
                        .sanitizedSql(cleanSql)
                        .build();
            }

            if (!effectiveAllowedTables.contains(lowerTbl)) {
                log.warn("[SQL 安全沙箱阻断] 未在数仓业务白名单中的表: {}", lowerTbl);
                return SafetyCheckResult.builder()
                        .safe(false)
                        .reason("【安全沙箱拦截】数据表 `" + lowerTbl + "` 不在当前数据源已授权的白名单中")
                        .sanitizedSql(cleanSql)
                        .build();
            }
        }

        // 4. 强制 LIMIT 限制注入（防大表全量查导致 JVM OOM 与 MySQL 慢查询，默认安全上限 1000）
        String finalSql;
        try {
            if (select instanceof PlainSelect plainSelect) {
                enforceLimit(plainSelect.getLimit(), plainSelect::setLimit, "PlainSelect");
                finalSql = statement.toString();
            } else if (select instanceof SetOperationList setOpList) {
                enforceLimit(setOpList.getLimit(), setOpList::setLimit, "SetOperationList");
                finalSql = statement.toString();
            } else {
                String wrappedSql = "SELECT * FROM (" + statement.toString() + ") AS _dp_limited LIMIT 1000";
                Statement wrappedStmt = CCJSqlParserUtil.parse(wrappedSql);
                if (!(wrappedStmt instanceof Select)) {
                    return SafetyCheckResult.builder()
                            .safe(false)
                            .reason("【安全沙箱拦截】改写后的查询未通过只读 Select 校验")
                            .sanitizedSql(cleanSql)
                            .build();
                }
                finalSql = wrappedStmt.toString();
            }
        } catch (Exception ex) {
            log.warn("[注入 LIMIT 异常，采用兜底包层限制] {}", ex.getMessage());
            String upperClean = cleanSql.toUpperCase(Locale.ROOT).trim();
            if (!upperClean.matches("(?s).*\\bLIMIT\\s+([1-9]\\d{0,2}|1000)\\s*$")) {
                finalSql = "SELECT * FROM (" + cleanSql + ") AS _dp_limited LIMIT 1000";
            } else {
                finalSql = cleanSql;
            }
        }

        return SafetyCheckResult.builder()
                .safe(true)
                .reason("PASS")
                .sanitizedSql(finalSql + ";")
                .build();
    }

    private void enforceLimit(Limit existingLimit, Consumer<Limit> setter, String kind) {
        if (existingLimit == null) {
            Limit limit = new Limit();
            limit.setRowCount(new LongValue(1000));
            setter.accept(limit);
            log.info("[SQL AST 重写: {}] 未检测到 LIMIT，自动注入安全上限 LIMIT 1000", kind);
            return;
        }
        Expression rowCountExpr = existingLimit.getRowCount();
        if (rowCountExpr instanceof LongValue lv) {
            if (lv.getValue() > 1000) {
                existingLimit.setRowCount(new LongValue(1000));
                log.info("[SQL AST 重写: {}] 检测到 LIMIT 超过安全上限，自动收敛为 LIMIT 1000", kind);
            }
        } else {
            existingLimit.setRowCount(new LongValue(1000));
            log.info("[SQL AST 重写: {}] 非数字 LIMIT 表达式，强制收敛为 LIMIT 1000", kind);
        }
    }

    private String normalizeChineseAliases(String sql) {
        if (sql == null || sql.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inBacktick = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !inBacktick) {
                inSingleQuote = !inSingleQuote;
                sb.append(c);
            } else if (c == '`' && !inSingleQuote) {
                inBacktick = !inBacktick;
                sb.append(c);
            } else if (!inSingleQuote && !inBacktick && isChinese(c)) {
                sb.append('`');
                while (i < sql.length() && (isChinese(sql.charAt(i)) || Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    sb.append(sql.charAt(i));
                    i++;
                }
                sb.append('`');
                i--;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }
}

