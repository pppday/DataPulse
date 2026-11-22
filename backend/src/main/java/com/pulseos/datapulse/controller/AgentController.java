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

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final DataAgentService dataAgentService;
    private final ExcelExportService excelExportService;

    @PostMapping("/query")
    public Result<AgentQueryResultVO> query(@RequestBody AskAgentRequest request) {
        return Result.success(dataAgentService.askAgent(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody AskAgentRequest request) {
        return dataAgentService.streamChat(request);
    }

    @DeleteMapping("/memory/{conversationId}")
    public Result<Boolean> clearMemory(@PathVariable String conversationId) {
        dataAgentService.clearConversationMemory(conversationId);
        return Result.success(true);
    }

    @PostMapping("/export/excel")
    public void exportExcel(@RequestBody ExportExcelRequest request, HttpServletResponse response) {
        excelExportService.exportToResponse(request, response);
    }

    @GetMapping("/history")
    public Result<List<QueryHistory>> history(@RequestParam(defaultValue = "30") int limit) {
        return Result.success(dataAgentService.listHistory(limit));
    }

    @GetMapping("/history/{id}")
    public Result<QueryHistory> historyDetail(@PathVariable Long id) {
        return Result.success(dataAgentService.getHistoryDetail(id));
    }
}
