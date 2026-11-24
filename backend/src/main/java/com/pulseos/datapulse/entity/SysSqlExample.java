package com.pulseos.datapulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Text-to-SQL Few-Shot 优质参考示例实体（映射 `sys_sql_example`，保存典型问法、标准 SQL 及其文本向量）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sql_example")
public class SysSqlExample {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long datasourceId;
    private String question;
    private String sqlText;
    private String embedding;
    private String embeddingModel;
    private String source;
    private LocalDateTime createdAt;
}
