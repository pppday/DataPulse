package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 销售部门维度表实体（映射默认数仓 `dim_department`，记录部门名称、负责人及所属大区）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dim_department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deptCode;
    private String deptName;
    private String managerName;
    private String region;
    private LocalDateTime createdAt;
}
