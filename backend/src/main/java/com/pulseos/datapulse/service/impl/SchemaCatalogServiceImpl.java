package com.pulseos.datapulse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pulseos.datapulse.common.BusinessException;
import com.pulseos.datapulse.dto.TableSchemaVO;
import com.pulseos.datapulse.entity.SysDatasource;
import com.pulseos.datapulse.entity.SysTableConfig;
import com.pulseos.datapulse.mapper.SysTableConfigMapper;
import com.pulseos.datapulse.service.DynamicDataSourceService;
import com.pulseos.datapulse.service.SchemaCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 【数仓元数据字典扫描与 Prompt 裁剪服务】
 *
 * 通俗理解：这是教大模型“看懂数据库长什么样”的翻译官。
 * - 自动按目标数据源类型（MySQL 查 `information_schema`，PostgreSQL 查 `pg_class`/`pg_stat_user_tables`）扫描所有实体表与只读分析视图（VIEW）；
 * - 默认过滤掉系统敏感表（`sys_` / `mysql` / `pg_` 等），结合 `sys_table_config` 里的白名单开关和自定义口径规则，生成精准、精简且带方言约束的 Schema Prompt。
 */
@Service
@RequiredArgsConstructor
public class SchemaCatalogServiceImpl implements SchemaCatalogService {

    private final DynamicDataSourceService dynamicDataSourceService;
    private final SysTableConfigMapper tableConfigMapper;

    private static final Set<String> SYSTEM_SENSITIVE_PREFIXES = Set.of(
            "sys_", "mysql", "information_schema", "performance_schema", "sys", "pg_"
    );

    private boolean isPostgres(SysDatasource ds) {
        return ds != null && "POSTGRESQL".equalsIgnoreCase(ds.getDbType());
    }

    @Override
    public String getWarehouseSchemaPrompt(Long datasourceId) {
        SysDatasource ds = dynamicDataSourceService.getById(datasourceId);
        boolean isPg = isPostgres(ds);
        List<TableSchemaVO> tables = listWarehouseTables(ds.getId());
        List<TableSchemaVO> whitelisted = tables.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsWhitelisted()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【企业数据库架构元数据字典 (数据源: %s / 类型: %s / 库名: %s)】:\n\n",
                ds.getName(), isPg ? "PostgreSQL 16" : "MySQL 8.0", ds.getDatabaseName()));

        for (TableSchemaVO t : whitelisted) {
            sb.append("数据表: `").append(t.getTableName()).append("` (").append(t.getTableComment()).append(")\n");
            if (t.getCustomPromptRules() != null && !t.getCustomPromptRules().isBlank()) {
                sb.append("  业务计算与外键规则: ").append(t.getCustomPromptRules()).append("\n");
            }
            sb.append("  字段结构定义:\n");
            for (TableSchemaVO.ColumnMeta c : t.getColumns()) {
                sb.append("    - `").append(c.getColumnName()).append("` ")
                        .append(c.getColumnType())
                        .append(Boolean.TRUE.equals(c.getIsPrimaryKey()) ? " [PRIMARY KEY 主键]" : "")
                        .append(" // ").append(c.getColumnComment()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("【Text-to-SQL 输出铁律】:\n");
        sb.append("1. 严禁访问上述表结构之外的任何未经授权表；\n");
        if (isPg) {
            sb.append("2. 必须生成标准 PostgreSQL 16 语法，严禁使用反引号（`）和 MySQL 专有函数；\n");
            sb.append("3. 所有中文列别名必须用英文双引号包裹，如 \"总对话数\", \"平均耗时\"；\n");
        } else {
            sb.append("2. 必须生成 MySQL 8.0 语法；\n");
            sb.append("3. 所有中文列别名必须用反引号包裹，如 `总销售额`, `大区名称`；\n");
        }

        return sb.toString();
    }

    @Override
    public List<TableSchemaVO> listWarehouseTables(Long datasourceId) {
        SysDatasource ds = dynamicDataSourceService.getById(datasourceId);
        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(ds.getId());
        boolean isPg = isPostgres(ds);
        String dbName = ds.getDatabaseName();

        // 1. 读取该数据源在 sys_table_config 中的配置
        List<SysTableConfig> configs = tableConfigMapper.selectList(
                new LambdaQueryWrapper<SysTableConfig>().eq(SysTableConfig::getDatasourceId, ds.getId())
        );
        Map<String, SysTableConfig> configMap = configs.stream()
                .collect(Collectors.toMap(SysTableConfig::getTableName, c -> c, (a, b) -> a));

        // 2. 按数据库方言扫描该库下的所有实体表与只读分析视图
        List<Map<String, Object>> tableRows;
        if (isPg) {
            String pgTablesSql = """
                SELECT
                    t.table_name AS "TABLE_NAME",
                    COALESCE(obj_description(c.oid, 'pg_class'), '') AS "TABLE_COMMENT",
                    COALESCE(s.n_live_tup, 0) AS "TABLE_ROWS",
                    t.table_type AS "TABLE_TYPE"
                FROM information_schema.tables t
                LEFT JOIN pg_class c
                  ON c.relname = t.table_name
                 AND c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema)
                LEFT JOIN pg_stat_user_tables s
                  ON s.relname = t.table_name AND s.schemaname = t.table_schema
                WHERE t.table_schema = 'public'
                  AND t.table_type IN ('BASE TABLE', 'VIEW')
                ORDER BY CASE WHEN t.table_type = 'VIEW' THEN 0 ELSE 1 END, t.table_name ASC
            """;
            tableRows = jdbcTemplate.queryForList(pgTablesSql);
        } else {
            String tablesSql = """
                SELECT TABLE_NAME, TABLE_COMMENT, TABLE_ROWS, TABLE_TYPE
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_TYPE IN ('BASE TABLE', 'VIEW')
                ORDER BY CASE WHEN TABLE_TYPE = 'VIEW' THEN 0 ELSE 1 END, TABLE_NAME ASC
            """;
            tableRows = jdbcTemplate.queryForList(tablesSql, dbName);
        }

        List<TableSchemaVO> result = new ArrayList<>();

        for (Map<String, Object> trow : tableRows) {
            String tableName = (String) trow.get("TABLE_NAME");
            String rawComment = (String) trow.get("TABLE_COMMENT");
            if ("VIEW".equalsIgnoreCase(rawComment)) {
                rawComment = "";
            }
            Number tableRowsCount = (Number) trow.get("TABLE_ROWS");

            // 判断是否是系统内置敏感表
            boolean isSystemSensitive = isSensitiveTable(tableName);

            SysTableConfig cfg = configMap.get(tableName);
            boolean isWhitelisted;
            String displayComment;
            String customRules = null;

            if (cfg != null) {
                isWhitelisted = cfg.getIsWhitelisted() != null && cfg.getIsWhitelisted() == 1;
                displayComment = cfg.getTableComment() != null && !cfg.getTableComment().isBlank()
                        ? cfg.getTableComment() : rawComment;
                customRules = cfg.getCustomPromptRules();
            } else {
                // 默认策略：业务表默认开启白名单，sys_/pg_ 开头的系统表默认隔离
                isWhitelisted = !isSystemSensitive;
                displayComment = rawComment != null && !rawComment.isBlank() ? rawComment : tableName;
            }

            // 扫描字段
            List<Map<String, Object>> colRows;
            if (isPg) {
                String pgColsSql = """
                    SELECT
                        c.column_name AS "COLUMN_NAME",
                        c.udt_name AS "COLUMN_TYPE",
                        COALESCE(pgd.description, '') AS "COLUMN_COMMENT",
                        CASE WHEN pk.column_name IS NOT NULL THEN 'PRI' ELSE '' END AS "COLUMN_KEY"
                    FROM information_schema.columns c
                    LEFT JOIN pg_class cls
                      ON cls.relname = c.table_name
                     AND cls.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = c.table_schema)
                    LEFT JOIN pg_description pgd
                      ON pgd.objoid = cls.oid AND pgd.objsubid = c.ordinal_position
                    LEFT JOIN (
                        SELECT kcu.table_name, kcu.column_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                         AND tc.table_schema = kcu.table_schema
                        WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_schema = 'public'
                    ) pk ON pk.table_name = c.table_name AND pk.column_name = c.column_name
                    WHERE c.table_schema = 'public' AND c.table_name = ?
                    ORDER BY c.ordinal_position ASC
                """;
                colRows = jdbcTemplate.queryForList(pgColsSql, tableName);
            } else {
                String colsSql = """
                    SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT, COLUMN_KEY
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    ORDER BY ORDINAL_POSITION ASC
                """;
                colRows = jdbcTemplate.queryForList(colsSql, dbName, tableName);
            }

            List<TableSchemaVO.ColumnMeta> colMetas = new ArrayList<>();
            for (Map<String, Object> crow : colRows) {
                String cname = (String) crow.get("COLUMN_NAME");
                String ctype = (String) crow.get("COLUMN_TYPE");
                String ccomment = (String) crow.get("COLUMN_COMMENT");
                String ckey = (String) crow.get("COLUMN_KEY");

                colMetas.add(TableSchemaVO.ColumnMeta.builder()
                        .columnName(cname)
                        .columnType(ctype)
                        .columnComment(ccomment != null && !ccomment.isBlank() ? ccomment : cname)
                        .isPrimaryKey("PRI".equalsIgnoreCase(ckey))
                        .build());
            }

            result.add(TableSchemaVO.builder()
                    .tableName(tableName)
                    .tableComment(displayComment != null && !displayComment.isBlank() ? displayComment : tableName)
                    .rowCount(tableRowsCount != null ? tableRowsCount.longValue() : 0L)
                    .isWhitelisted(isWhitelisted)
                    .customPromptRules(customRules)
                    .columns(colMetas)
                    .build());
        }

        return result;
    }

    @Override
    public Map<String, Object> previewTableData(Long datasourceId, String tableName, int limit) {
        Set<String> whitelisted = getWhitelistedTables(datasourceId);
        if (!whitelisted.contains(tableName.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(403, "数据表 `" + tableName + "` 未纳入当前数据源的分析白名单，禁止访问采样");
        }

        SysDatasource ds = dynamicDataSourceService.getById(datasourceId);
        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(datasourceId);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String sql = isPostgres(ds)
                ? "SELECT * FROM \"" + tableName.replace("\"", "") + "\" LIMIT " + safeLimit
                : "SELECT * FROM `" + tableName.replace("`", "") + "` LIMIT " + safeLimit;

        List<Map<String, Object>> rawRows = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> rows = sanitizeJdbcRows(rawRows);
        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());

        Map<String, Object> response = new HashMap<>();
        response.put("tableName", tableName);
        response.put("columns", columns);
        response.put("rows", rows);
        response.put("total", rows.size());

        return response;
    }

    public static List<Map<String, Object>> sanitizeJdbcRows(List<Map<String, Object>> rawRows) {
        if (rawRows == null || rawRows.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> cleaned = new ArrayList<>(rawRows.size());
        for (Map<String, Object> row : rawRows) {
            Map<String, Object> copy = new LinkedHashMap<>(row.size());
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                copy.put(entry.getKey(), sanitizeJdbcValue(entry.getValue()));
            }
            cleaned.add(copy);
        }
        return cleaned;
    }

    private static Object sanitizeJdbcValue(Object val) {
        if (val == null) return null;
        if (val instanceof byte[] bytes) {
            return "[binary " + bytes.length + " bytes]";
        }
        if ("org.postgresql.util.PGobject".equals(val.getClass().getName())) {
            try {
                String type = String.valueOf(val.getClass().getMethod("getType").invoke(val));
                String strVal = String.valueOf(val.getClass().getMethod("getValue").invoke(val));
                if ("vector".equalsIgnoreCase(type)) {
                    return "[pgvector embedding]";
                }
                return strVal != null && strVal.length() > 300 ? strVal.substring(0, 300) + "..." : strVal;
            } catch (Exception ignored) {
                return val.toString();
            }
        }
        return val;
    }

    @Override
    public void updateTableConfig(Long datasourceId, String tableName, boolean isWhitelisted, String tableComment, String customRules) {
        SysTableConfig config = tableConfigMapper.selectOne(
                new LambdaQueryWrapper<SysTableConfig>()
                        .eq(SysTableConfig::getDatasourceId, datasourceId)
                        .eq(SysTableConfig::getTableName, tableName)
        );

        if (config == null) {
            config = SysTableConfig.builder()
                    .datasourceId(datasourceId)
                    .tableName(tableName)
                    .tableComment(tableComment)
                    .isWhitelisted(isWhitelisted ? 1 : 0)
                    .customPromptRules(customRules)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            tableConfigMapper.insert(config);
        } else {
            config.setIsWhitelisted(isWhitelisted ? 1 : 0);
            if (tableComment != null) config.setTableComment(tableComment);
            if (customRules != null) config.setCustomPromptRules(customRules);
            config.setUpdatedAt(LocalDateTime.now());
            tableConfigMapper.updateById(config);
        }
    }

    @Override
    public Set<String> getWhitelistedTables(Long datasourceId) {
        List<TableSchemaVO> tables = listWarehouseTables(datasourceId);
        return tables.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsWhitelisted()))
                .map(t -> t.getTableName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private boolean isSensitiveTable(String tableName) {
        String lower = tableName.toLowerCase(Locale.ROOT);
        return SYSTEM_SENSITIVE_PREFIXES.stream().anyMatch(lower::startsWith);
    }
}
