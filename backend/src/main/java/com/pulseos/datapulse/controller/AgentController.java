package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.dto.AgentQueryResultVO;
import com.pulseos.datapulse.dto.AskAgentRequest;
import com.pulseos.datapulse.dto.ExportExcelRequest;
import com.pulseos.datapulse.entity.QueryHistory;
import com.pulseos.datapulse.service.DataAgentService;
import com.pulseos.datapulse.service.ExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Text-to-SQL 自然语言智能取数与分析核心控制器（兼容保留历史路由 {@code /api/v1/agent}）。
 * <p>
 * 负责承接前端工作台的全部核心交互：同步自然语言查库取数、SSE 流式对话问答、
 * 清空多轮对话记忆、流式导出 Excel 报表以及查询个人历史审计记录。
 */
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final DataAgentService dataAgentService;
    private final ExcelExportService excelExportService;

    /**
     * 核心取数接口：输入自然语言问题，编排完成「语意缓存命中 / SQL 生成 / AST 沙箱审计 / 动态库执行 / 图表推荐与洞察总结」。
     */
    @PostMapping("/query")
    public Result<AgentQueryResultVO> query(@RequestBody AskAgentRequest request) {
        return Result.success(dataAgentService.askAgent(request));
    }

    /**
     * SSE 流式对话接口：以 {@code text/event-stream} 逐字推送大模型对业务问题与查询结果的实时解析说明。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody AskAgentRequest request) {
        return dataAgentService.streamChat(request);
    }

    /**
     * 清空指定会话 ID 在 Redis 中的多轮上下文记忆（前端点击「新建对话」时触发）。
     */
    @DeleteMapping("/memory/{conversationId}")
    public Result<Boolean> clearMemory(@PathVariable String conversationId) {
        dataAgentService.clearConversationMemory(conversationId);
        return Result.success(true);
    }

    /**
     * Excel 流式导出接口：将前端当前查询出的表格结果集直接通过 EasyExcel 写入 HTTP 响应流供浏览器下载。
     */
    @PostMapping("/export/excel")
    public void exportExcel(@RequestBody ExportExcelRequest request, HttpServletResponse response) {
        excelExportService.exportToResponse(request, response);
    }

    /**
     * 拉取当前登录用户最近的自然语言取数历史审计列表（默认返回最近 30 条）。
     */
    @GetMapping("/history")
    public Result<List<QueryHistory>> history(@RequestParam(defaultValue = "30") int limit) {
        return Result.success(dataAgentService.listHistory(limit));
    }

    /**
     * 按主键查询某条历史取数记录的完整详情（含生成的 SQL、耗时、返回行数与业务洞察快照）。
     */
    @GetMapping("/history/{id}")
    public Result<QueryHistory> historyDetail(@PathVariable Long id) {
        return Result.success(dataAgentService.getHistoryDetail(id));
    }
}
