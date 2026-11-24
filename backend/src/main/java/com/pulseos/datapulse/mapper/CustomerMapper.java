package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/** 默认数仓客户维表（dim_customer）Mapper。 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
