package com.pulseos.datapulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 自然语言查库取数与流式问答请求体（DTO）。
 * <p>
 * 支持两种查询入口：输入自然语言 {@code prompt} 让 Text-to-SQL 引擎自动生成并执行 SQL，
 * 或直接传入用户手改后的 {@code customSql} 送入安全沙箱复跑验证。
 */
@Data
public class AskAgentRequest {
    /**
     * 自然语言分析需求提问（与 customSql 二选一必填）
     */
    private String prompt;

    /**
     * 用户手工编写或二次修改的 SQL 语句（若存在则直接送入 AST 沙箱审查与执行）
     */
    private String customSql;

    /**
     * 期望偏好图表类型: auto(自动推断), bar(柱状图), line(折线图), pie(饼图), table(表格)
     */
    private String chartPreference = "auto";

    /**
     * 目标数据源ID，默认为 1 (主数据源)
     */
    private Long datasourceId = 1L;

    /**
     * 多轮会话 ID（用于 Spring AI Redis ChatMemory 持久化上下文）
     */
    private String conversationId;
}
