package com.pulseos.datapulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pulseos.datapulse.entity.QueryHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** SQL 审计与查询历史表（sys_query_history）Mapper，内置联表拉取操作人账号与真实姓名的最近记录查询。 */
@Mapper
public interface QueryHistoryMapper extends BaseMapper<QueryHistory> {

    @Select("""
        SELECT q.*, u.username, u.real_name
        FROM sys_query_history q
        LEFT JOIN sys_user u ON q.user_id = u.id
        ORDER BY q.created_at DESC
        LIMIT #{limit}
    """)
    List<QueryHistory> selectRecentHistoryWithUser(@Param("limit") int limit);
}
