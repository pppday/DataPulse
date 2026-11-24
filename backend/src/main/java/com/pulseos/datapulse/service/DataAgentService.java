package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.AgentQueryResultVO;
import com.pulseos.datapulse.dto.AskAgentRequest;
import com.pulseos.datapulse.entity.QueryHistory;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * DataPulse 自然语言 Text-to-SQL 取数、多轮分析对话与历史审计查询服务接口。
 * （接口名沿用历史命名，核心职责为自然语言转 SQL、沙箱校验执行与商业洞察生成。）
 */
public interface DataAgentService {
    AgentQueryResultVO askAgent(AskAgentRequest request);
    Flux<String> streamChat(AskAgentRequest request);
    void clearConversationMemory(String conversationId);
    List<QueryHistory> listHistory(int limit);
    QueryHistory getHistoryDetail(Long id);
}
