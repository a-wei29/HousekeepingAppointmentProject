package com.gk.study.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final long DEFAULT_EXPIRE_HOURS = 24 * 7; // 7天

    private final RedisTemplate<String, String> redisTemplate;

    public RedisChatMemoryStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("RedisChatMemoryStore initialized with official LangChain4j serializers");
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = buildKey(memoryId);
        log.debug("Getting messages for memoryId: {}, key: {}", memoryId, key);

        // 从Redis获取存储的JSON字符串列表
        List<String> jsonMessages = redisTemplate.opsForList().range(key, 0, -1);

        if (jsonMessages == null || jsonMessages.isEmpty()) {
            return List.of();
        }

        // ✅ 正确方式：将每个JSON字符串单独反序列化
        List<ChatMessage> messages = jsonMessages.stream()
                .map(json -> {
                    try {
                        return ChatMessageDeserializer.messageFromJson(json);
                    } catch (Exception e) {
                        log.error("Failed to deserialize message: {}", json, e);
                        return null;
                    }
                })
                .filter(msg -> msg != null)
                .collect(Collectors.toList());

        log.info("Retrieved {} messages from Redis", messages.size());
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);
        log.info("Updating {} messages for memoryId: {}", messages.size(), memoryId);

        if (messages == null || messages.isEmpty()) {
            log.warn("No messages to update for memoryId: {}, deleting key", memoryId);
            redisTemplate.delete(key);
            return;
        }

        // ✅ 正确方式：将每个消息单独序列化为JSON字符串
        List<String> jsonMessages = messages.stream()
                .map(msg -> {
                    try {
                        return ChatMessageSerializer.messageToJson(msg);
                    } catch (Exception e) {
                        log.error("Failed to serialize message: {}", msg, e);
                        return null;
                    }
                })
                .filter(json -> json != null)
                .collect(Collectors.toList());

        if (jsonMessages.isEmpty()) {
            log.error("All messages failed to serialize for memoryId: {}", memoryId);
            return;
        }

        // 使用Redis事务确保原子性
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.multi();
            try {
                redisTemplate.delete(key);

                // ✅ 正确方式：将列表中的每个字符串作为独立元素存入Redis List
                if (!jsonMessages.isEmpty()) {
                    // 将列表转换为数组，每个元素成为List中的一个独立项
                    String[] messagesArray = jsonMessages.toArray(new String[0]);
                    redisTemplate.opsForList().rightPushAll(key, messagesArray);
                    redisTemplate.expire(key, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);

                    log.info("Stored {} messages in Redis list", messagesArray.length);
                }

                connection.exec();
                log.info("Successfully updated {} messages for memoryId: {}", jsonMessages.size(), memoryId);

            } catch (Exception e) {
                connection.discard();
                log.error("Redis transaction failed for memoryId: {}", memoryId, e);
                throw new RuntimeException("Failed to update messages", e);
            }
            return null;
        });
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = buildKey(memoryId);
        log.info("Deleting messages for memoryId: {}", memoryId);
        redisTemplate.delete(key);
    }

    private String buildKey(Object memoryId) {
        return KEY_PREFIX + memoryId.toString();
    }

    // 监控方法：获取当前用户的记忆条数
    public Long getMessageCount(Object memoryId) {
        String key = buildKey(memoryId);
        return redisTemplate.opsForList().size(key);
    }
}