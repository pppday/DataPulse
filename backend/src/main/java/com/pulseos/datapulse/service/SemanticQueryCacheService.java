package com.pulseos.datapulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseos.datapulse.dto.AgentQueryResultVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的自然语言查询结果缓存服务（按数据源 ID + 规范化提问 MD5 缓存）。
 * 同一数据源的重复分析提问直接毫秒级命中缓存返回，避免重复消耗大模型 Token 与底层数据库算力。
 */
@Service
@RequiredArgsConstructor
public class SemanticQueryCacheService {

    private static final Logger log = LoggerFactory.getLogger(SemanticQueryCacheService.class);
    private static final String CACHE_PREFIX = "datapulse:query_cache:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public AgentQueryResultVO get(Long datasourceId, String prompt) {
        try {
            String key = buildCacheKey(datasourceId, prompt);
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj != null) {
                log.info("[Redis 缓存命中] key={}", key);
                AgentQueryResultVO vo = objectMapper.convertValue(obj, AgentQueryResultVO.class);
                return vo;
            }
        } catch (Exception e) {
            log.warn("[Redis 缓存读取异常，降级直查] error={}", e.getMessage());
        }
        return null;
    }

    public void put(Long datasourceId, String prompt, AgentQueryResultVO result, long timeoutSeconds) {
        if (result == null || result.getIsSafe() != 1 || result.getRowCount() == null || result.getRowCount() == 0) return;
        try {
            String key = buildCacheKey(datasourceId, prompt);
            redisTemplate.opsForValue().set(key, result, timeoutSeconds, TimeUnit.SECONDS);
            log.info("[Redis 写入查询缓存] key={}, ttl={}s", key, timeoutSeconds);
        } catch (Exception e) {
            log.warn("[Redis 缓存写入失败] error={}", e.getMessage());
        }
    }

    public void evict(Long datasourceId, String prompt) {
        try {
            String key = buildCacheKey(datasourceId, prompt);
            redisTemplate.delete(key);
            log.info("[Redis 清除查询缓存] key={}", key);
        } catch (Exception e) {
            log.warn("[Redis 缓存清除失败] error={}", e.getMessage());
        }
    }

    private String buildCacheKey(Long datasourceId, String prompt) {
        String clean = prompt == null ? "" : prompt.trim().toLowerCase();
        return CACHE_PREFIX + (datasourceId == null ? 1L : datasourceId) + ":" + md5Hex(clean);
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
