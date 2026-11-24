package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.SysTableConfig;
import org.apache.ibatis.annotations.Mapper;

/** 表级白名单与业务口径规则表（sys_table_config）Mapper。 */
@Mapper
public interface SysTableConfigMapper extends BaseMapper<SysTableConfig> {
}
