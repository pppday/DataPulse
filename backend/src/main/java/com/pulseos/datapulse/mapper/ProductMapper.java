package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/** 默认数仓商品维表（dim_product）Mapper。 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
