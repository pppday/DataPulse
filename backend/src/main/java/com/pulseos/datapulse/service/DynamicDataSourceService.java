package com.pulseos.datapulse.service;

import com.pulseos.datapulse.entity.SysDatasource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

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
