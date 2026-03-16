package com.trade.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.GsonChatMessageJsonCodec;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Redis 的对话记忆存储。
 *
 * 存储格式：key -> JSON(list of ChatMessage)
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final GsonChatMessageJsonCodec CODEC = new GsonChatMessageJsonCodec();

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Duration ttl;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate, String keyPrefix, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = (keyPrefix == null || keyPrefix.isBlank()) ? "ai:chat:memory:" : keyPrefix;
        this.ttl = ttl;
    }

    private String key(Object memoryId) {
        return keyPrefix + String.valueOf(memoryId);
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(key(memoryId));
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<ChatMessage> messages = CODEC.messagesFromJson(json);
            return (messages == null) ? new ArrayList<>() : new ArrayList<>(messages);
        } catch (Exception e) {
            // 兼容历史/脏数据：解析失败时直接返回空，避免影响主流程
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = CODEC.messagesToJson(messages == null ? List.of() : messages);
        String k = key(memoryId);
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.opsForValue().set(k, json, ttl);
        } else {
            redisTemplate.opsForValue().set(k, json);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(key(memoryId));
    }
}
