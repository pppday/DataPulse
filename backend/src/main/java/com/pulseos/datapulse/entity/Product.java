package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品维度表实体（映射默认数仓 `dim_product`，记录商品编码、品类、单价、成本价与库存）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dim_product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    private String category;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private String status;
    private LocalDateTime createdAt;
}
