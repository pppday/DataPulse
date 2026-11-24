package com.pulseos.datapulse.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化与连接模板配置。
 * <p>
 * 默认的 JDK 序列化在 Redis 里是不可读的二进制乱码，这里改为「Key 用纯字符串、Value 用 Jackson JSON」，
 * 并注册 {@link JavaTimeModule} 支持 {@code LocalDateTime} 等 Java 8 日期时间类型，方便线上直接肉眼排查缓存。
 */
@Configuration
public class RedisConfig {

    /**
     * 构建全局共用的 {@link RedisTemplate}，供业务缓存、看板统计等模块读写 JSON 对象。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 自定义 ObjectMapper：开启全部字段可见并支持 LocalDateTime 等 JSR-310 时间类型
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.registerModule(new JavaTimeModule());

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(om);

        // Key / HashKey 统一走 UTF-8 字符串序列化，避免出现 \xac\xed 前缀乱码
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value / HashValue 统一走 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
