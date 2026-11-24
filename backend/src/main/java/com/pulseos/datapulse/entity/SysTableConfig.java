package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 数据源表级白名单与业务口径规则实体（映射 `sys_table_config`，控制哪些表/视图允许查询以及喂给大模型的额外业务说明）。 */
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
