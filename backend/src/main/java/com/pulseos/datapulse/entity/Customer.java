package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 客户维度表实体（映射默认数仓 `dim_customer`，记录客户大区、省市与分级）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dim_customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerCode;
    private String customerName;
    private String region;
    private String province;
    private String city;
    private String tier;
    private LocalDateTime createdAt;
}
