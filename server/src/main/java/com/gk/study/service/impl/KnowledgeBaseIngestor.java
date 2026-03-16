package com.gk.study.service.impl;

import com.gk.study.retriever.BM25Retriever;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KnowledgeBaseIngestor implements CommandLineRunner {

    private final EmbeddingStore<TextSegment> knowledgeStore;  // 改名，明确是知识库
    private final EmbeddingModel embeddingModel;
    private final ResourceLoader resourceLoader;
    private final BM25Retriever bm25Retriever;

    // 缓存知识库数据，供 BusinessDataSync 使用
    private List<TextSegment> cachedKnowledgeSegments = new ArrayList<>();

    public KnowledgeBaseIngestor(
            @Qualifier("knowledgeEmbeddingStore") EmbeddingStore<TextSegment> knowledgeStore,  // 指定使用知识库 Store
            EmbeddingModel embeddingModel,
            ResourceLoader resourceLoader,
            BM25Retriever bm25Retriever) {
        this.knowledgeStore = knowledgeStore;
        this.embeddingModel = embeddingModel;
        this.resourceLoader = resourceLoader;
        this.bm25Retriever = bm25Retriever;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 开始加载知识库 ==========");

        try {
            log.info("清空知识库集合数据...");
            knowledgeStore.removeAll();  // 只清空知识库，不影响业务库
            cachedKnowledgeSegments.clear();

            // 1. 扫描所有知识库文件
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledge/*.{txt,md}");

            log.info("找到 {} 个知识库文件", resources.length);

            if (resources.length == 0) {
                log.warn("没有找到任何知识库文件，请检查 src/main/resources/knowledge/ 目录");
                return;
            }

            List<TextSegment> allSegments = new ArrayList<>();
            int totalSegments = 0;
            int totalAdded = 0;

            // 2. 遍历每个文件
            for (Resource resource : resources) {
                log.info("处理文件: {}", resource.getFilename());

                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    if (content.trim().isEmpty()) {
                        log.warn("文件 {} 内容为空，跳过", resource.getFilename());
                        continue;
                    }

                    Document document = Document.from(content);
                    DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
                    List<TextSegment> segments = splitter.split(document);

                    log.info("文件 {} 被切成 {} 段", resource.getFilename(), segments.size());
                    totalSegments += segments.size();

                    // 4. 处理每一段
                    for (TextSegment segment : segments) {
                        allSegments.add(segment);  // 收集到 allSegments

                        try {
                            Embedding embedding = embeddingModel.embed(segment.text()).content();
                            knowledgeStore.add(embedding, segment);  // 存入知识库
                            totalAdded++;
                        } catch (Exception e) {
                            log.error("段落添加失败: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("处理文件 {} 失败", resource.getFilename(), e);
                }
            }

            log.info("========== 知识库加载完成 ==========");
            log.info("总段数: {}, 成功添加: {}", totalSegments, totalAdded);

            // 缓存知识库数据，供 BusinessDataSync 使用
            cachedKnowledgeSegments = new ArrayList<>(allSegments);

            // 更新 BM25 的数据并刷新索引
            if (!allSegments.isEmpty()) {
                log.info("更新 BM25 数据，共 {} 条", allSegments.size());
                bm25Retriever.updateSegments(allSegments);
                bm25Retriever.refreshIndex();
            }

        } catch (Exception e) {
            log.error("知识库加载失败", e);
        }
    }

    /**
     * 获取所有知识库片段（供 BusinessDataSync 使用）
     */
    public List<TextSegment> getAllKnowledgeSegments() {
        return new ArrayList<>(cachedKnowledgeSegments);
    }
}