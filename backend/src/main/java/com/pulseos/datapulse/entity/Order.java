package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 销售订单事实表实体（映射默认数仓 `fact_order`，记录下单量、销售额、毛利、支付方式与下单日期）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fact_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long customerId;
    private Long productId;
    private Long deptId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountRate;
    private BigDecimal totalAmount;
    private BigDecimal profitAmount;
    private String orderStatus;
    private String payMethod;
    private LocalDate orderDate;
    private LocalDateTime createdAt;
}
