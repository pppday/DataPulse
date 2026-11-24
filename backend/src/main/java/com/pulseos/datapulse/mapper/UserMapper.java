package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 平台账号表（sys_user）Mapper。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
