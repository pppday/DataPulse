package com.pulseos.datapulse.config;

import com.pulseos.datapulse.service.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 大模型客户端与多轮对话记忆配置。
 * <p>
 * 底层大模型的超时时间、重试策略（Retry）、API BaseURL 与模型名由 Spring AI 自动读取
 * {@code application.yml} 注入到 {@link ChatClient.Builder} 中；本类基于同一 Builder 克隆出两套专用客户端：
 * <ul>
 *     <li>{@code statelessChatClient}：无记忆客户端，用于 SQL 报错自愈、结果摘要洞察等单轮独立任务，避免带入无关历史；</li>
 *     <li>{@code conversationalChatClient}：带 Redis 滑动窗口记忆的客户端，用于自然语言连续追问取数。</li>
 * </ul>
 */
@Configuration
public class SpringAiConfig {

    /**
     * 配置基于 Redis 持久化的滑动窗口记忆组件：每个会话仅保留最近 10 条消息，
     * 既能支撑「再按区域拆分一下」之类的多轮追问，又能控制 Prompt 长度避免 Token 失控。
     */
    @Bean
    @Primary
    public ChatMemory chatMemory(RedisChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    /**
     * 无状态单轮大模型客户端（默认主 Bean）：仅挂载日志 Advisor，不自动拼接历史对话。
     */
    @Bean("statelessChatClient")
    @Primary
    public ChatClient statelessChatClient(ChatClient.Builder builder) {
        return builder.clone()
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 有状态多轮会话大模型客户端：在调用模型前后自动通过 {@link MessageChatMemoryAdvisor}
     * 从 Redis 拉取和回写当前会话的最近问答历史。
     */
    @Bean("conversationalChatClient")
    public ChatClient conversationalChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder.clone()
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
