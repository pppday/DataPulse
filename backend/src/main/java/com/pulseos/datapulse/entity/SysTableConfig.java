package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("sys_table_config")
public class SysTableConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long datasourceId;
    private String tableName;
    private String tableComment;
    private Integer isWhitelisted;
    private String customPromptRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
