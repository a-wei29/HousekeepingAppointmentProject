package com.gk.study.service.impl;

import com.gk.study.retriever.HybridRetriever;
import com.gk.study.retriever.IntentAwareRetriever;
import com.gk.study.service.Assistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagAssistantService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> knowledgeStore;  // 知识库
    private final EmbeddingStore<TextSegment> businessStore;   // 业务库
    private final ChatMemoryProvider chatMemoryProvider;
    private final HybridRetriever hybridRetriever;  // 混合检索器
    private final IntentAwareRetriever intentAwareRetriever;  // 新增：意图感知检索器

    @Value("${ai.rag.max-results:3}")
    private int maxResults;

    private Assistant assistant;
    private Assistant ragAssistant;

    public RagAssistantService(
            ChatModel chatModel,
            EmbeddingModel embeddingModel,
            @Qualifier("knowledgeEmbeddingStore") EmbeddingStore<TextSegment> knowledgeStore,
            @Qualifier("businessEmbeddingStore") EmbeddingStore<TextSegment> businessStore,
            ChatMemoryProvider chatMemoryProvider,
            HybridRetriever hybridRetriever,
            IntentAwareRetriever intentAwareRetriever) {  // 新增参数
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.knowledgeStore = knowledgeStore;
        this.businessStore = businessStore;
        this.chatMemoryProvider = chatMemoryProvider;
        this.hybridRetriever = hybridRetriever;
        this.intentAwareRetriever = intentAwareRetriever;
    }

    @PostConstruct
    public void init() {
        log.info("========== 初始化 RAG Assistant ==========");
        log.info("配置参数: maxResults={}", maxResults);

        // 1. 纯对话助手
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();

        // 2. 使用意图感知检索器作为内容检索器
        ContentRetriever retriever = new ContentRetriever() {
            @Override
            public List<dev.langchain4j.rag.content.Content> retrieve(dev.langchain4j.rag.query.Query query) {
                String text = query.text();
                log.info("执行意图感知检索: {}", text);

                // 使用意图感知检索器进行检索
                List<TextSegment> segments = intentAwareRetriever.retrieve(text, maxResults);

                // 记录检索到的内容类型，用于调试
                if (!segments.isEmpty()) {
                    log.info("检索到 {} 个片段:", segments.size());
                    for (int i = 0; i < segments.size(); i++) {
                        String preview = segments.get(i).text().length() > 100
                                ? segments.get(i).text().substring(0, 100) + "..."
                                : segments.get(i).text();
                        String type = extractType(segments.get(i).text());
                        log.info("  [{}] 类型: {}, 预览: {}", i + 1, type, preview);
                    }
                } else {
                    log.warn("未检索到相关片段");
                }

                // 转换为Content
                return segments.stream()
                        .map(segment -> dev.langchain4j.rag.content.Content.from(segment.text()))
                        .collect(Collectors.toList());
            }

            // 辅助方法：提取内容类型
            private String extractType(String text) {
                if (text.contains("【类型:服务项目】")) return "服务项目";
                if (text.contains("【类型:阿姨信息】")) return "阿姨信息";
                if (text.contains("【类型:用户评价】")) return "用户评价";
                if (text.contains("【类型:服务标签】")) return "服务标签";
                if (text.contains("【类型:服务分类】")) return "服务分类";
                if (text.contains("【类型:历史订单】")) return "历史订单";
                return "未知类型";
            }
        };

        log.info("意图感知检索器创建成功");

        // 3. 带RAG的助手（使用优化后的系统提示词）
        this.ragAssistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(retriever)
                .chatMemoryProvider(chatMemoryProvider)
                .build();

        log.info("RAG Assistant 创建成功");
        log.info("========== RAG Assistant 初始化完成 ==========");
    }

    /**
     * 纯对话（用于测试）
     */
    public String chat(String message) {
        return assistant.chat(message);
    }

    /**
     * 带RAG的客服回答
     */
    public String assistantChat(String userId, String message) {
        return ragAssistant.assistantChat(userId, message);
    }

    /**
     * 特定服务的问答
     */
    public String serviceChat(String userId, String serviceType, String question) {
        return ragAssistant.serviceChat(userId, serviceType, question);
    }
}