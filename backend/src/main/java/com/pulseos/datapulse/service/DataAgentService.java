package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.AgentQueryResultVO;
import com.pulseos.datapulse.dto.AskAgentRequest;
import com.pulseos.datapulse.entity.QueryHistory;
import reactor.core.publisher.Flux;

import java.util.List;

public interface DataAgentService {
    AgentQueryResultVO askAgent(AskAgentRequest request);
    Flux<String> streamChat(AskAgentRequest request);
    void clearConversationMemory(String conversationId);
    List<QueryHistory> listHistory(int limit);
    QueryHistory getHistoryDetail(Long id);
}
