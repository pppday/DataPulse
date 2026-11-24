package com.pulseos.datapulse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Redis 的多轮分析对话记忆仓库（实现 Spring AI ChatMemoryRepository，TTL 24 小时）。
 * 当 Redis 偶发不可用时自动降级为进程内 ConcurrentHashMap，保证多轮追问不报错中断。
 */
@Repository
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryRepository.class);
    private static final String KEY_PREFIX = "datapulse:chat_memory:";
    private static final String IDS_KEY = "datapulse:chat_memory:ids";
    private static final Duration MEMORY_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, List<Message>> fallbackStore = new ConcurrentHashMap<>();

    @Override
    public List<String> findConversationIds() {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(IDS_KEY);
            if (members != null && !members.isEmpty()) {
                return members.stream().map(String::valueOf).toList();
            }
            return new ArrayList<>(fallbackStore.keySet());
        } catch (Exception e) {
            log.warn("[Redis ChatMemory 读取会话列表失败，降级内存] error={}", e.getMessage());
            return new ArrayList<>(fallbackStore.keySet());
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        try {
            Object raw = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
            if (raw == null) {
                return fallbackStore.getOrDefault(conversationId, List.of());
            }
            List<StoredMessage> stored;
            if (raw instanceof String json) {
                stored = objectMapper.readValue(json, new TypeReference<>() {});
            } else {
                stored = objectMapper.convertValue(raw, new TypeReference<>() {});
            }
            if (stored == null || stored.isEmpty()) {
                return List.of();
            }
            List<Message> messages = new ArrayList<>(stored.size());
            for (StoredMessage item : stored) {
                Message msg = toMessage(item);
                if (msg != null) {
                    messages.add(msg);
                }
            }
            return messages;
        } catch (Exception e) {
            log.warn("[Redis ChatMemory 读取会话失败，降级内存] id={}, error={}", conversationId, e.getMessage());
            return new ArrayList<>(fallbackStore.getOrDefault(conversationId, Collections.emptyList()));
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.isBlank() || messages == null) {
            return;
        }
        List<Message> snapshot = new ArrayList<>(messages);
        try {
            List<StoredMessage> stored = new ArrayList<>(snapshot.size());
            for (Message m : snapshot) {
                if (m == null || m.getMessageType() == null) {
                    continue;
                }
                String text = m.getText() != null ? m.getText() : "";
                stored.add(new StoredMessage(m.getMessageType().name(), text));
            }
            String json = objectMapper.writeValueAsString(stored);
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationId, json, MEMORY_TTL);
            redisTemplate.opsForSet().add(IDS_KEY, conversationId);
            fallbackStore.remove(conversationId);
        } catch (Exception e) {
            log.warn("[Redis ChatMemory 写入失败，降级内存] id={}, error={}", conversationId, e.getMessage());
            fallbackStore.put(conversationId, snapshot);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        fallbackStore.remove(conversationId);
        try {
            redisTemplate.delete(KEY_PREFIX + conversationId);
            redisTemplate.opsForSet().remove(IDS_KEY, conversationId);
        } catch (Exception e) {
            log.warn("[Redis ChatMemory 删除失败] id={}, error={}", conversationId, e.getMessage());
        }
    }

    private static Message toMessage(StoredMessage item) {
        if (item == null || item.getType() == null) {
            return null;
        }
        String text = item.getText() != null ? item.getText() : "";
        return switch (MessageType.valueOf(item.getType())) {
            case USER -> new UserMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            case SYSTEM -> new SystemMessage(text);
            case TOOL -> null;
        };
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class StoredMessage {
        private String type;
        private String text;
    }
}
