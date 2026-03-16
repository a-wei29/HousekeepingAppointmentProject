package com.gk.study.retriever.hyde;

import com.gk.study.retriever.BM25Retriever;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HyDERetriever {

    private final EmbeddingStore<TextSegment> knowledgeStore;
    private final EmbeddingStore<TextSegment> businessStore;
    private final EmbeddingModel embeddingModel;
    private final BM25Retriever bm25Retriever;
    private final HydeGenerator hydeGenerator;

    public HyDERetriever(
            @Qualifier("knowledgeEmbeddingStore") EmbeddingStore<TextSegment> knowledgeStore,
            @Qualifier("businessEmbeddingStore") EmbeddingStore<TextSegment> businessStore,
            EmbeddingModel embeddingModel,
            BM25Retriever bm25Retriever,
            HydeGenerator hydeGenerator) {
        this.knowledgeStore = knowledgeStore;
        this.businessStore = businessStore;
        this.embeddingModel = embeddingModel;
        this.bm25Retriever = bm25Retriever;
        this.hydeGenerator = hydeGenerator;

        log.info("HyDE检索器初始化完成");
    }

    /**
     * 使用HyDE增强的混合检索
     */
    public List<TextSegment> searchWithHyde(String query, int topK) {
        long startTime = System.currentTimeMillis();
        log.info("========== HyDE增强检索开始 ==========");
        log.info("原始查询: {}", query);

        // 1. 生成假设文档
        String hypotheticalDoc = hydeGenerator.generateHypotheticalDocument(query);
        log.info("假设文档: {}", hypotheticalDoc);

        // 2. 用假设文档进行向量检索
        List<TextSegment> hydeVectorResults = vectorSearchWithHyde(hypotheticalDoc, knowledgeStore, businessStore, 10);
        log.info("HyDE向量检索结果数: {}", hydeVectorResults.size());

        // 3. 用原始查询进行BM25检索（作为补充）
        List<TextSegment> bm25Results = bm25Retriever.retrieve(query, 5);
        log.info("BM25检索结果数: {}", bm25Results.size());

        // 4. 融合结果（HyDE结果权重更高）
        List<TextSegment> finalResults = fuseResults(hydeVectorResults, bm25Results, topK);

        long cost = System.currentTimeMillis() - startTime;
        log.info("HyDE检索完成, 最终结果数: {}, 耗时: {}ms", finalResults.size(), cost);

        return finalResults;
    }

    /**
     * 使用假设文档进行向量检索
     */
    private List<TextSegment> vectorSearchWithHyde(
            String hypotheticalDoc,
            EmbeddingStore<TextSegment> store1,
            EmbeddingStore<TextSegment> store2,
            int topK) {

        List<TextSegment> allResults = new ArrayList<>();

        try {
            // 生成假设文档的向量
            var embedding = embeddingModel.embed(hypotheticalDoc).content();

            // 在知识库中检索
            var request1 = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(topK)
                    .minScore(0.3)
                    .build();

            EmbeddingSearchResult<TextSegment> result1 = store1.search(request1);
            allResults.addAll(result1.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));

            // 在业务库中检索
            var request2 = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .maxResults(topK)
                    .minScore(0.3)
                    .build();

            EmbeddingSearchResult<TextSegment> result2 = store2.search(request2);
            allResults.addAll(result2.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            log.error("HyDE向量检索失败", e);
        }

        return allResults;
    }

    /**
     * 融合HyDE结果和BM25结果
     */
    private List<TextSegment> fuseResults(
            List<TextSegment> hydeResults,
            List<TextSegment> bm25Results,
            int topK) {

        // 简单加权融合：HyDE结果权重0.7，BM25结果权重0.3
        java.util.Map<TextSegment, Double> scores = new java.util.HashMap<>();

        for (int i = 0; i < hydeResults.size(); i++) {
            scores.put(hydeResults.get(i), 0.7 * (1.0 / (i + 1)));
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            scores.merge(bm25Results.get(i), 0.3 * (1.0 / (i + 1)), Double::sum);
        }

        return scores.entrySet().stream()
                .sorted(java.util.Map.Entry.<TextSegment, Double>comparingByValue().reversed())
                .limit(topK)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}