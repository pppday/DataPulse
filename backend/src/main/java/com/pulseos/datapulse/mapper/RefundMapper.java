package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.Refund;
import org.apache.ibatis.annotations.Mapper;

/** 默认数仓退款事实表（fact_refund）Mapper。 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
}
