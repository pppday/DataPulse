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

@Service
@RequiredArgsConstructor
public class SchemaCatalogServiceImpl implements SchemaCatalogService {

    private final DynamicDataSourceService dynamicDataSourceService;
    private final SysTableConfigMapper tableConfigMapper;

    private static final Set<String> SYSTEM_SENSITIVE_PREFIXES = Set.of(
            "sys_", "mysql", "information_schema", "performance_schema", "sys"
    );

    @Override
    public String getWarehouseSchemaPrompt(Long datasourceId) {
        SysDatasource ds = dynamicDataSourceService.getById(datasourceId);
        List<TableSchemaVO> tables = listWarehouseTables(ds.getId());
        List<TableSchemaVO> whitelisted = tables.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsWhitelisted()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【企业数据库架构元数据字典 (数据源: %s / 库名: %s)】:\n\n", ds.getName(), ds.getDatabaseName()));

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
        sb.append("2. 必须生成 MySQL 8.0 语法；\n");
        sb.append("3. 所有中文列别名必须用反引号包裹，如 `总销售额`, `大区名称`；\n");

        return sb.toString();
    }

    @Override
    public List<TableSchemaVO> listWarehouseTables(Long datasourceId) {
        SysDatasource ds = dynamicDataSourceService.getById(datasourceId);
        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(ds.getId());

        String dbName = ds.getDatabaseName();

        // 1. 读取该数据源在 sys_table_config 中的配置
        List<SysTableConfig> configs = tableConfigMapper.selectList(
                new LambdaQueryWrapper<SysTableConfig>().eq(SysTableConfig::getDatasourceId, ds.getId())
        );
        Map<String, SysTableConfig> configMap = configs.stream()
                .collect(Collectors.toMap(SysTableConfig::getTableName, c -> c, (a, b) -> a));

        // 2. 从 information_schema 扫描该数据库下的所有实体表
        String tablesSql = """
            SELECT TABLE_NAME, TABLE_COMMENT, TABLE_ROWS
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = ?
              AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME ASC
        """;

        List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(tablesSql, dbName);
        List<TableSchemaVO> result = new ArrayList<>();

        for (Map<String, Object> trow : tableRows) {
            String tableName = (String) trow.get("TABLE_NAME");
            String rawComment = (String) trow.get("TABLE_COMMENT");
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
                // 默认策略：业务表默认开启白名单，sys_ 开头的系统表默认隔离
                isWhitelisted = !isSystemSensitive;
                displayComment = rawComment != null && !rawComment.isBlank() ? rawComment : tableName;
            }

            // 扫描字段
            String colsSql = """
                SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT, COLUMN_KEY
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION ASC
            """;

            List<Map<String, Object>> colRows = jdbcTemplate.queryForList(colsSql, dbName, tableName);
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

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(datasourceId);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String sql = "SELECT * FROM `" + tableName + "` LIMIT " + safeLimit;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());

        Map<String, Object> response = new HashMap<>();
        response.put("tableName", tableName);
        response.put("columns", columns);
        response.put("rows", rows);
        response.put("total", rows.size());

        return response;
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
