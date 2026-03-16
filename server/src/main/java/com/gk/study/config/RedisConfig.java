package com.gk.study.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * 注意：不要手动创建 stringRedisTemplate！
     * Spring Boot 会自动配置一个，可以直接在代码中 @Autowired 使用
     */

    /**
     * Object类型的RedisTemplate - 用于存储对象
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        log.info("RedisTemplate (Object) initialized successfully");
        return template;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 禁用空Bean失败特性
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 启用默认类型信息
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );

        // 添加MixIn解决LangChain4j消息类的构造函数问题
        objectMapper.addMixIn(SystemMessage.class, SystemMessageMixin.class);
        objectMapper.addMixIn(UserMessage.class, UserMessageMixin.class);
        objectMapper.addMixIn(AiMessage.class, AiMessageMixin.class);

        log.info("ObjectMapper created with default typing and mixins");
        return objectMapper;
    }

    /**
     * SystemMessage的MixIn接口
     */
    abstract static class SystemMessageMixin {
        @JsonCreator
        public SystemMessageMixin(@JsonProperty("text") String text) {}
    }

    /**
     * UserMessage的MixIn接口
     */
    abstract static class UserMessageMixin {
        @JsonCreator
        public UserMessageMixin(@JsonProperty("text") String text) {}
    }

    /**
     * AiMessage的MixIn接口
     */
    abstract static class AiMessageMixin {
        @JsonCreator
        public AiMessageMixin(@JsonProperty("text") String text) {}
    }
}