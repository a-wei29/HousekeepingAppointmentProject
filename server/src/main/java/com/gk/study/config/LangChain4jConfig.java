package com.gk.study.config;

import com.gk.study.service.RedisChatMemoryStore;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.zhipu.api-key}")
    private String apiKey;

    @Value("${langchain4j.zhipu.chat-model.model:glm-4-flash}")
    private String modelName;

    @Value("${langchain4j.zhipu.chat-model.temperature:0.7}")
    private Double temperature;

    @Value("${langchain4j.zhipu.chat-model.max-tokens:512}")
    private Integer maxTokens;

    @Value("${langchain4j.zhipu.chat-model.timeout:60}")
    private Long timeout;

    @Value("${chromadb.url:http://localhost:8000}")
    private String chromaUrl;

    // 知识库集合名称
    @Value("${chromadb.knowledge-collection:knowledge_qa}")
    private String knowledgeCollection;

    // 业务库集合名称
    @Value("${chromadb.business-collection:business_data}")
    private String businessCollection;

    /**
     * 智谱大模型客户端
     */
    @Bean
    public ChatModel zhipuChatModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(apiKey)
                .model(modelName)
                .temperature(temperature)
                .maxToken(maxTokens)
                .maxRetries(3)
                .connectTimeout(Duration.ofSeconds(timeout))
                .readTimeout(Duration.ofSeconds(timeout))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 嵌入模型（本地运行，384维）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 知识库向量存储
     */
    @Bean
    @Qualifier("knowledgeEmbeddingStore")
    public EmbeddingStore<TextSegment> knowledgeEmbeddingStore() {
        log.info("创建知识库向量存储，集合名称: {}", knowledgeCollection);
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(knowledgeCollection)
                .apiVersion(ChromaApiVersion.V2)
                .timeout(Duration.ofSeconds(30))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 业务库向量存储
     */
    @Bean
    @Qualifier("businessEmbeddingStore")
    public EmbeddingStore<TextSegment> businessEmbeddingStore() {
        log.info("创建业务库向量存储，集合名称: {}", businessCollection);
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(businessCollection)
                .apiVersion(ChromaApiVersion.V2)
                .timeout(Duration.ofSeconds(30))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * ========== Redis持久化记忆配置 ==========
     */

    /**
     * 聊天记忆存储 - Redis实现
     */
    @Bean
    @Primary
    public ChatMemoryStore redisChatMemoryStore(RedisTemplate<String, String> redisTemplate) {
        log.info("Creating RedisChatMemoryStore bean with StringRedisTemplate");
        return new RedisChatMemoryStore(redisTemplate);
    }

    /**
     * 备选：内存存储
     */
    @Bean
    public ChatMemoryStore inMemoryChatMemoryStore() {
        log.info("Using InMemoryChatMemoryStore (messages will be lost after restart)");
        return new dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore();
    }

    /**
     * 聊天记忆提供者
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore redisChatMemoryStore) {
        return memoryId -> {
            log.info("创建/获取 memoryId={} 的聊天记忆", memoryId);
            return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(20)
                    .chatMemoryStore(redisChatMemoryStore)
                    .build();
        };
    }
}