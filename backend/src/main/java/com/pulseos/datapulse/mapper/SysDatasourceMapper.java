package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.SysDatasource;
import org.apache.ibatis.annotations.Mapper;

/** 动态多数据源配置表（sys_datasource）Mapper。 */
@Mapper
public interface SysDatasourceMapper extends BaseMapper<SysDatasource> {
}
