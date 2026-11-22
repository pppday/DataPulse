package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_query_history")
public class QueryHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userPrompt;
    private String generatedSql;
    private Integer isSafe;
    private String safetyReason;
    private Long executionTimeMs;
    private Integer rowCount;
    private String chartType;
    private String insightSummary;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String realName;
}
