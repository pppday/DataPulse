package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 动态多数据源连接配置实体（映射 `sys_datasource`，存储 MySQL / PostgreSQL 主机、端口、库名、凭据及默认标记）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_datasource")
public class SysDatasource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String dbType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private Integer isDefault;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
