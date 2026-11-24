package com.pulseos.datapulse.service;

import com.pulseos.datapulse.entity.SysDatasource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

/** 动态多数据源连接池（MySQL / PostgreSQL）维护、探活与 JdbcTemplate 路由接口。 */
public interface DynamicDataSourceService {
    List<SysDatasource> listAll();
    SysDatasource getById(Long id);
    SysDatasource getDefault();
    boolean testConnection(SysDatasource ds);
    SysDatasource saveDatasource(SysDatasource ds);
    void deleteDatasource(Long id);
    void setDefaultDatasource(Long id);
    DataSource getDataSource(Long id);
    JdbcTemplate getJdbcTemplate(Long id);
}
