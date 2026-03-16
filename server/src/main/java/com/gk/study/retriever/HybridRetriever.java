package com.gk.study.retriever;

import com.gk.study.retriever.hyde.HydeGenerator;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HybridRetriever {

    private final EmbeddingStore<TextSegment> knowledgeStore;  // 知识库
    private final EmbeddingStore<TextSegment> businessStore;   // 业务库
    private final EmbeddingModel embeddingModel;
    private final BM25Retriever bm25Retriever;
    private final HydeGenerator hydeGenerator;  // 新增 HyDE 生成器

    @Value("${rag.hybrid.vector-weight:0.5}")
    private double vectorWeight;

    @Value("${rag.hybrid.bm25-weight:0.5}")
    private double bm25Weight;

    @Value("${rag.hybrid.rrf-k:60}")
    private int rrfK;

    @Value("${rag.hybrid.vector-results:10}")
    private int vectorResultsCount;

    @Value("${rag.hybrid.bm25-results:10}")
    private int bm25ResultsCount;

    @Value("${rag.hybrid.final-results:5}")
    private int finalResultsCount;

    @Value("${rag.hybrid.knowledge-weight:0.6}")
    private double knowledgeWeight;

    @Value("${rag.hybrid.business-weight:0.4}")
    private double businessWeight;

    // HyDE 配置
    @Value("${rag.hyde.enabled:true}")
    private boolean hydeEnabled;

    @Value("${rag.hyde.weight:0.6}")
    private double hydeWeight;

    @Autowired
    public HybridRetriever(
            @Qualifier("knowledgeEmbeddingStore") EmbeddingStore<TextSegment> knowledgeStore,
            @Qualifier("businessEmbeddingStore") EmbeddingStore<TextSegment> businessStore,
            EmbeddingModel embeddingModel,
            BM25Retriever bm25Retriever,
            HydeGenerator hydeGenerator) {  // 新增参数
        this.knowledgeStore = knowledgeStore;
        this.businessStore = businessStore;
        this.embeddingModel = embeddingModel;
        this.bm25Retriever = bm25Retriever;
        this.hydeGenerator = hydeGenerator;

        log.info("========== 混合检索器初始化完成 ==========");
        log.info("知识库权重: {}, 业务库权重: {}", knowledgeWeight, businessWeight);
        log.info("HyDE增强: {}, HyDE权重: {}", hydeEnabled ? "启用" : "禁用", hydeWeight);
    }

    /**
     * 公共方法：向量检索（从指定存储）
     */
    public List<TextSegment> vectorSearch(String query, int topK, String storeType) {
        if ("knowledge".equals(storeType)) {
            return vectorSearchInStore(knowledgeStore, query, topK);
        } else if ("business".equals(storeType)) {
            return vectorSearchInStore(businessStore, query, topK);
        } else {
            // 默认从两个库检索并合并
            List<TextSegment> knowledgeResults = vectorSearchInStore(knowledgeStore, query, topK);
            List<TextSegment> businessResults = vectorSearchInStore(businessStore, query, topK);
            return mergeVectorResults(knowledgeResults, businessResults, knowledgeWeight, businessWeight);
        }
    }

    /**
     * 公共方法：带分数的向量检索（从指定存储）
     */
    public List<EmbeddingMatch<TextSegment>> vectorSearchWithScores(String query, int topK, String storeType) {
        EmbeddingStore<TextSegment> store = "knowledge".equals(storeType) ? knowledgeStore : businessStore;
        try {
            var queryEmbedding = embeddingModel.embed(query).content();
            var request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .minScore(0.3)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = store.search(request);
            return searchResult.matches();

        } catch (Exception e) {
            log.error("向量检索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 在指定存储中执行向量检索
     */
    private List<TextSegment> vectorSearchInStore(EmbeddingStore<TextSegment> store, String query, int topK) {
        try {
            var queryEmbedding = embeddingModel.embed(query).content();
            var request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .minScore(0.3)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = store.search(request);

            return searchResult.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 使用 HyDE 进行向量检索
     */
    private List<TextSegment> vectorSearchWithHyde(String query, int topK) {
        try {
            // 1. 生成假设文档
            String hypotheticalDoc = hydeGenerator.generateHypotheticalDocument(query);
            log.debug("HyDE假设文档: {}", hypotheticalDoc);

            // 2. 用假设文档生成向量
            var hydeEmbedding = embeddingModel.embed(hypotheticalDoc).content();

            // 3. 在两个库中检索
            List<TextSegment> allResults = new ArrayList<>();

            var request1 = EmbeddingSearchRequest.builder()
                    .queryEmbedding(hydeEmbedding)
                    .maxResults(topK)
                    .minScore(0.3)
                    .build();
            EmbeddingSearchResult<TextSegment> result1 = knowledgeStore.search(request1);
            allResults.addAll(result1.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));

            var request2 = EmbeddingSearchRequest.builder()
                    .queryEmbedding(hydeEmbedding)
                    .maxResults(topK)
                    .minScore(0.3)
                    .build();
            EmbeddingSearchResult<TextSegment> result2 = businessStore.search(request2);
            allResults.addAll(result2.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));

            return allResults;

        } catch (Exception e) {
            log.error("HyDE向量检索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 合并两个来源的向量检索结果
     */
    private List<TextSegment> mergeVectorResults(
            List<TextSegment> knowledgeResults,
            List<TextSegment> businessResults,
            double knowledgeWeight,
            double businessWeight) {

        Map<TextSegment, Double> scoredResults = new HashMap<>();

        // 知识库结果
        for (int i = 0; i < knowledgeResults.size(); i++) {
            double score = knowledgeWeight * (1.0 / (i + 1));
            scoredResults.put(knowledgeResults.get(i), score);
        }

        // 业务库结果
        for (int i = 0; i < businessResults.size(); i++) {
            double score = businessWeight * (1.0 / (i + 1));
            scoredResults.merge(businessResults.get(i), score, Double::sum);
        }

        return scoredResults.entrySet().stream()
                .sorted(Map.Entry.<TextSegment, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 判断是否应该使用 HyDE
     */
    private boolean shouldUseHyde(String query) {
        if (!hydeEnabled) return false;

        // 对于以下情况使用 HyDE：
        // 1. 短查询（<10个字符）
        // 2. 包含推荐类词语
        // 3. 包含抽象描述
        String lowerQuery = query.toLowerCase();
        return query.length() < 10 ||
                lowerQuery.contains("推荐") ||
                lowerQuery.contains("怎么样") ||
                lowerQuery.contains("如何") ||
                lowerQuery.contains("什么") ||
                lowerQuery.contains("哪些");
    }

    /**
     * 混合检索主方法
     */
    public List<TextSegment> search(String query) {
        return search(query, finalResultsCount);
    }

    /**
     * 混合检索（指定返回数量）
     */
    public List<TextSegment> search(String query, int topK) {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始混合检索 ==========");
        log.info("查询: '{}'", query);

        List<TextSegment> vectorResults;
        List<TextSegment> bm25Results;

        // 1. 判断是否使用 HyDE
        if (shouldUseHyde(query)) {
            log.info("启用 HyDE 增强检索");

            // 使用 HyDE 进行向量检索
            List<TextSegment> hydeResults = vectorSearchWithHyde(query, vectorResultsCount * 2);
            log.info("HyDE向量检索结果数: {}", hydeResults.size());

            // 普通向量检索（作为补充）
            List<TextSegment> knowledgeResults = vectorSearchInStore(knowledgeStore, query, vectorResultsCount);
            List<TextSegment> businessResults = vectorSearchInStore(businessStore, query, vectorResultsCount);
            List<TextSegment> normalResults = mergeVectorResults(knowledgeResults, businessResults, knowledgeWeight, businessWeight);

            // 融合 HyDE 结果和普通结果
            vectorResults = fuseHydeAndNormal(hydeResults, normalResults, hydeWeight, 1 - hydeWeight);

        } else {
            // 2. 从两个库分别进行向量检索
            List<TextSegment> knowledgeVectorResults = vectorSearchInStore(knowledgeStore, query, vectorResultsCount);
            List<TextSegment> businessVectorResults = vectorSearchInStore(businessStore, query, vectorResultsCount);

            log.info("知识库向量检索结果数: {}, 业务库向量检索结果数: {}",
                    knowledgeVectorResults.size(), businessVectorResults.size());

            // 合并向量检索结果（加权）
            vectorResults = mergeVectorResults(
                    knowledgeVectorResults, businessVectorResults,
                    knowledgeWeight, businessWeight
            );
        }

        log.info("向量检索最终结果数: {}", vectorResults.size());

        // 3. BM25检索
        bm25Results = bm25Retriever.retrieve(query, bm25ResultsCount);
        log.info("BM25检索结果数: {}", bm25Results.size());

        // 4. 如果BM25没结果，直接返回向量结果
        if (bm25Results.isEmpty()) {
            log.info("BM25无结果，直接返回向量检索结果");
            return vectorResults.stream().limit(topK).collect(Collectors.toList());
        }

        // 5. RRF融合
        List<TextSegment> finalResults = rrfFusion(vectorResults, bm25Results, topK);

        long cost = System.currentTimeMillis() - startTime;
        log.info("混合检索完成, 最终结果数: {}, 耗时: {}ms", finalResults.size(), cost);

        return finalResults;
    }

    /**
     * 融合 HyDE 结果和普通向量结果
     */
    private List<TextSegment> fuseHydeAndNormal(
            List<TextSegment> hydeResults,
            List<TextSegment> normalResults,
            double hydeWeight,
            double normalWeight) {

        Map<TextSegment, Double> scores = new HashMap<>();

        for (int i = 0; i < hydeResults.size(); i++) {
            scores.put(hydeResults.get(i), hydeWeight * (1.0 / (i + 1)));
        }

        for (int i = 0; i < normalResults.size(); i++) {
            scores.merge(normalResults.get(i), normalWeight * (1.0 / (i + 1)), Double::sum);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<TextSegment, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 带分数的混合检索（用于调试）
     */
    public Map<TextSegment, Double> searchWithScores(String query) {
        // 向量检索（从两个库）
        List<TextSegment> knowledgeResults = vectorSearchInStore(knowledgeStore, query, vectorResultsCount * 2);
        List<TextSegment> businessResults = vectorSearchInStore(businessStore, query, vectorResultsCount * 2);

        // BM25检索
        List<BM25Retriever.ScoredResult> bm25Scored = bm25Retriever.retrieveWithScores(query, bm25ResultsCount * 2);

        Map<TextSegment, Double> allScores = new LinkedHashMap<>();

        // 记录知识库向量分数
        for (int i = 0; i < knowledgeResults.size(); i++) {
            double score = 1.0 / (rrfK + i + 1) * vectorWeight * knowledgeWeight;
            allScores.put(knowledgeResults.get(i), score);
        }

        // 记录业务库向量分数
        for (int i = 0; i < businessResults.size(); i++) {
            double score = 1.0 / (rrfK + i + 1) * vectorWeight * businessWeight;
            allScores.merge(businessResults.get(i), score, Double::sum);
        }

        // 记录BM25分数
        for (int i = 0; i < bm25Scored.size(); i++) {
            BM25Retriever.ScoredResult sr = bm25Scored.get(i);
            allScores.merge(sr.getSegment(), 1.0 / (rrfK + i + 1) * bm25Weight, Double::sum);
        }

        return allScores.entrySet().stream()
                .sorted(Map.Entry.<TextSegment, Double>comparingByValue().reversed())
                .limit(finalResultsCount)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * RRF融合算法
     */
    private List<TextSegment> rrfFusion(
            List<TextSegment> vectorResults,
            List<TextSegment> bm25Results,
            int topK) {

        Map<TextSegment, Double> scores = new ConcurrentHashMap<>();

        // 计算向量结果的RRF分数
        for (int i = 0; i < vectorResults.size(); i++) {
            TextSegment segment = vectorResults.get(i);
            double score = 1.0 / (rrfK + i + 1);
            scores.merge(segment, score, Double::sum);
        }

        // 计算BM25结果的RRF分数
        for (int i = 0; i < bm25Results.size(); i++) {
            TextSegment segment = bm25Results.get(i);
            double score = 1.0 / (rrfK + i + 1);
            scores.merge(segment, score, Double::sum);
        }

        // 按总分排序并返回
        return scores.entrySet().stream()
                .sorted(Map.Entry.<TextSegment, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}