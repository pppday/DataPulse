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

/** 退款退货事实表实体（映射默认数仓 `fact_refund`，记录退款金额、原因、状态与退款日期）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fact_refund")
public class Refund {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private Long orderId;
    private Long customerId;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundStatus;
    private LocalDate refundDate;
    private LocalDateTime createdAt;
}
