package com.pulseos.datapulse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pulseos.datapulse.common.BusinessException;
import com.pulseos.datapulse.entity.SysDatasource;
import com.pulseos.datapulse.mapper.SysDatasourceMapper;
import com.pulseos.datapulse.service.DynamicDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DynamicDataSourceServiceImpl implements DynamicDataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceServiceImpl.class);

    private final SysDatasourceMapper datasourceMapper;
    private final DataSource defaultSpringDataSource;

    private final Map<Long, HikariDataSource> poolMap = new ConcurrentHashMap<>();

    @Override
    public List<SysDatasource> listAll() {
        return datasourceMapper.selectList(new LambdaQueryWrapper<SysDatasource>().orderByDesc(SysDatasource::getIsDefault).orderByDesc(SysDatasource::getId));
    }

    @Override
    public SysDatasource getById(Long id) {
        if (id == null) return getDefault();
        SysDatasource ds = datasourceMapper.selectById(id);
        return ds != null ? ds : getDefault();
    }

    @Override
    public SysDatasource getDefault() {
        SysDatasource ds = datasourceMapper.selectOne(new LambdaQueryWrapper<SysDatasource>().eq(SysDatasource::getIsDefault, 1).last("LIMIT 1"));
        if (ds == null) {
            ds = datasourceMapper.selectOne(new LambdaQueryWrapper<SysDatasource>().last("LIMIT 1"));
        }
        return ds;
    }

    @Override
    public boolean testConnection(SysDatasource ds) {
        String url = buildJdbcUrl(ds);
        try {
            Class.forName(resolveDriverClassName(ds));
            DriverManager.setLoginTimeout(5);
            try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword())) {
                return conn.isValid(3);
            }
        } catch (Exception e) {
            log.warn("[数据源连通性测试失败] url={}, error={}", url, e.getMessage());
            throw new BusinessException(400, "数据源连接测试失败: " + e.getMessage());
        }
    }

    @Override
    public SysDatasource saveDatasource(SysDatasource ds) {
        testConnection(ds);
        ds.setUpdatedAt(LocalDateTime.now());

        // 若设置了当前数据源为默认 (isDefault == 1)，先将系统中其他数据源重置为非默认
        if (Integer.valueOf(1).equals(ds.getIsDefault())) {
            datasourceMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysDatasource>()
                    .set(SysDatasource::getIsDefault, 0));
        }

        if (ds.getId() == null) {
            ds.setCreatedAt(LocalDateTime.now());
            ds.setStatus(1);
            if (ds.getIsDefault() == null) {
                ds.setIsDefault(0);
            }
            datasourceMapper.insert(ds);
        } else {
            closePool(ds.getId());
            // 若尝试将默认数据源改为非默认，校验系统中是否还有其他默认数据源
            if (Integer.valueOf(0).equals(ds.getIsDefault())) {
                Long defaultCount = datasourceMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDatasource>()
                                .eq(SysDatasource::getIsDefault, 1)
                                .ne(SysDatasource::getId, ds.getId())
                );
                if (defaultCount == 0) {
                    throw new BusinessException(400, "系统必须保留至少一个默认数据源，无法直接取消当前唯一的默认状态");
                }
            }
            datasourceMapper.updateById(ds);
        }
        return ds;
    }

    @Override
    public void setDefaultDatasource(Long id) {
        SysDatasource target = datasourceMapper.selectById(id);
        if (target == null) {
            throw new BusinessException(404, "数据源不存在");
        }
        // 重置所有为非默认
        datasourceMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysDatasource>()
                .set(SysDatasource::getIsDefault, 0));
        target.setIsDefault(1);
        target.setUpdatedAt(LocalDateTime.now());
        datasourceMapper.updateById(target);
        log.info("[数据源管理] 数据源 [{}] (id={}) 已设为系统全局默认数据源", target.getName(), id);
    }

    @Override
    public void deleteDatasource(Long id) {
        SysDatasource ds = datasourceMapper.selectById(id);
        if (ds != null && ds.getIsDefault() == 1) {
            throw new BusinessException(400, "禁止删除默认主数据源");
        }
        closePool(id);
        datasourceMapper.deleteById(id);
    }

    @Override
    public DataSource getDataSource(Long id) {
        SysDatasource ds = getById(id);
        if (ds == null) {
            return defaultSpringDataSource;
        }

        if (ds.getId() == 1L
                && ("127.0.0.1".equals(ds.getHost()) || "dw-master.datapulse.internal".equals(ds.getHost()))
                && "datapulse_db".equals(ds.getDatabaseName())) {
            return defaultSpringDataSource;
        }

        try {
            return poolMap.computeIfAbsent(ds.getId(), k -> {
                log.info("[动态初始化 HikariCP 连接池] dsId={}, dbType={}, name={}", ds.getId(), ds.getDbType(), ds.getName());
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(buildJdbcUrl(ds));
                config.setUsername(ds.getUsername());
                config.setPassword(ds.getPassword());
                config.setDriverClassName(resolveDriverClassName(ds));
                config.setMaximumPoolSize(8);
                config.setMinimumIdle(1);
                config.setConnectionTimeout(5000);
                config.setInitializationFailTimeout(5000);
                config.setIdleTimeout(300000);
                config.setPoolName("HikariDynamic-" + ds.getId());
                return new HikariDataSource(config);
            });
        } catch (Exception e) {
            poolMap.remove(ds.getId());
            log.error("[动态数据源初始化失败] dsId={}, name={}, error={}", ds.getId(), ds.getName(), e.getMessage());
            throw new BusinessException(400, "目标数据源连接失败：无法连通数据库 [" + ds.getName() + " (" + ds.getHost() + ":" + ds.getPort() + "/" + ds.getDatabaseName() + ")]，请检查网络配置或数据库服务状态");
        }
    }

    @Override
    public JdbcTemplate getJdbcTemplate(Long id) {
        return new JdbcTemplate(getDataSource(id));
    }

    static String formatHostForJdbc(String rawHost) {
        String host = (rawHost == null || rawHost.isBlank()) ? "127.0.0.1" : rawHost.trim();
        // 裸 IPv6 地址（如 2408:xxxx::1 或 ::1）自动补方括号以符合 JDBC URI 规范
        if (host.contains(":") && !(host.startsWith("[") && host.endsWith("]"))) {
            return "[" + host + "]";
        }
        return host;
    }

    private boolean isPostgres(SysDatasource ds) {
        return ds != null && "POSTGRESQL".equalsIgnoreCase(ds.getDbType());
    }

    private String resolveDriverClassName(SysDatasource ds) {
        return isPostgres(ds) ? "org.postgresql.Driver" : "com.mysql.cj.jdbc.Driver";
    }

    String buildJdbcUrl(SysDatasource ds) {
        String host = formatHostForJdbc(ds.getHost());
        if (isPostgres(ds)) {
            int port = ds.getPort() != null && ds.getPort() > 0 ? ds.getPort() : 5432;
            String db = (ds.getDatabaseName() != null && !ds.getDatabaseName().isBlank()) ? ds.getDatabaseName().trim() : "postgres";
            return String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=5&socketTimeout=30&stringtype=unspecified",
                    host, port, db);
        }
        int port = ds.getPort() != null && ds.getPort() > 0 ? ds.getPort() : 3306;
        String db = (ds.getDatabaseName() != null && !ds.getDatabaseName().isBlank()) ? ds.getDatabaseName().trim() : "datapulse_db";
        return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                host, port, db);
    }

    private void closePool(Long id) {
        HikariDataSource ds = poolMap.remove(id);
        if (ds != null && !ds.isClosed()) {
            ds.close();
            log.info("[动态注销 HikariCP 连接池] dsId={}", id);
        }
    }

    @PreDestroy
    public void cleanup() {
        poolMap.values().forEach(ds -> {
            if (!ds.isClosed()) ds.close();
        });
        poolMap.clear();
    }
}
