package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/** 默认数仓销售部门维表（dim_department）Mapper。 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
