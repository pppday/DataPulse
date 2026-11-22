package com.pulseos.datapulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisChatMemoryRepositoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void redisReadWriteDeleteAndWindowTruncationWork() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        SetOperations<String, Object> setOps = mock(SetOperations.class);
        Map<String, Object> fakeStore = new HashMap<>();
        Set<Object> fakeIds = new HashSet<>();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        doAnswer(inv -> {
            fakeStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), any(), any(Duration.class));
        when(valueOps.get(anyString())).thenAnswer(inv -> fakeStore.get(inv.getArgument(0)));
        doAnswer(inv -> {
            fakeIds.add(inv.getArgument(1));
            return 1L;
        }).when(setOps).add(anyString(), any());
        when(setOps.members(anyString())).thenReturn(fakeIds);
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> fakeStore.remove(inv.getArgument(0)) != null);
        when(setOps.remove(anyString(), any())).thenAnswer(inv -> fakeIds.remove(inv.getArgument(1)) ? 1L : 0L);

        RedisChatMemoryRepository repo = new RedisChatMemoryRepository(redisTemplate, new ObjectMapper());
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(4)
                .build();

        for (int i = 1; i <= 3; i++) {
            memory.add("conv-1", List.of(new UserMessage("Q" + i), new AssistantMessage("A" + i)));
        }

        List<Message> window = memory.get("conv-1");
        assertEquals(4, window.size());
        assertEquals("Q2", window.get(0).getText());
        assertEquals("A3", window.get(3).getText());
        assertTrue(repo.findConversationIds().contains("conv-1"));

        memory.clear("conv-1");
        assertTrue(memory.get("conv-1").isEmpty());
        assertFalse(repo.findConversationIds().contains("conv-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void fallsBackToInMemoryStoreWhenRedisThrows() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doThrow(new RuntimeException("Redis connection refused")).when(valueOps).set(anyString(), any(), any(Duration.class));
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        RedisChatMemoryRepository repo = new RedisChatMemoryRepository(redisTemplate, new ObjectMapper());
        repo.saveAll("conv-offline", List.of(new UserMessage("你好"), new AssistantMessage("你好，请问分析什么指标？")));

        List<Message> loaded = repo.findByConversationId("conv-offline");
        assertEquals(2, loaded.size());
        assertEquals("你好", loaded.get(0).getText());
        assertTrue(repo.findConversationIds().contains("conv-offline"));

        repo.deleteByConversationId("conv-offline");
        assertTrue(repo.findByConversationId("conv-offline").isEmpty());
    }
}
