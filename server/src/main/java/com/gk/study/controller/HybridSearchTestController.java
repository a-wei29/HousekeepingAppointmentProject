package com.gk.study.controller;

import com.gk.study.retriever.BM25Retriever;
import com.gk.study.retriever.HybridRetriever;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/test/hybrid")
public class HybridSearchTestController {

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private BM25Retriever bm25Retriever;

    /**
     * 测试1：基础检索测试
     * http://localhost:8080/test/hybrid/search?q=发票怎么开
     */
    @GetMapping("/search")
    public Map<String, Object> testSearch(@RequestParam String q) {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();

        try {
            // 执行混合检索
            List<TextSegment> results = hybridRetriever.search(q, 5);

            result.put("success", true);
            result.put("query", q);
            result.put("time_ms", System.currentTimeMillis() - start);
            result.put("result_count", results.size());

            List<Map<String, String>> resultList = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                TextSegment segment = results.get(i);
                Map<String, String> item = new HashMap<>();
                item.put("rank", String.valueOf(i + 1));
                item.put("content", segment.text());
                if (segment.metadata() != null) {
                    item.put("metadata", segment.metadata().toString());
                }
                resultList.add(item);
            }
            result.put("results", resultList);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("测试失败", e);
        }

        return result;
    }

    /**
     * 测试2：对比三种检索方式
     * http://localhost:8080/test/hybrid/compare?q=月嫂多少钱
     */
    @GetMapping("/compare")
    public Map<String, Object> compareSearch(@RequestParam String q) {
        Map<String, Object> result = new HashMap<>();

        // 1. 向量检索（从两个库合并）
        long vStart = System.currentTimeMillis();
        List<TextSegment> vectorResults = hybridRetriever.vectorSearch(q, 5, "all");
        result.put("vector_time", System.currentTimeMillis() - vStart);
        result.put("vector_results", formatResults(vectorResults));

        // 2. BM25检索
        long bStart = System.currentTimeMillis();
        List<TextSegment> bm25Results = bm25Retriever.retrieve(q, 5);
        result.put("bm25_time", System.currentTimeMillis() - bStart);
        result.put("bm25_results", formatResults(bm25Results));

        // 3. 混合检索
        long hStart = System.currentTimeMillis();
        List<TextSegment> hybridResults = hybridRetriever.search(q, 5);
        result.put("hybrid_time", System.currentTimeMillis() - hStart);
        result.put("hybrid_results", formatResults(hybridResults));

        return result;
    }

    /**
     * 测试3：对比知识库和业务库的检索结果
     * http://localhost:8080/test/hybrid/compare-stores?q=月嫂
     */
    @GetMapping("/compare-stores")
    public Map<String, Object> compareStores(@RequestParam String q) {
        Map<String, Object> result = new HashMap<>();

        // 1. 只从知识库检索
        long kStart = System.currentTimeMillis();
        List<TextSegment> knowledgeResults = hybridRetriever.vectorSearch(q, 5, "knowledge");
        result.put("knowledge_time", System.currentTimeMillis() - kStart);
        result.put("knowledge_results", formatResults(knowledgeResults));

        // 2. 只从业务库检索
        long bStart = System.currentTimeMillis();
        List<TextSegment> businessResults = hybridRetriever.vectorSearch(q, 5, "business");
        result.put("business_time", System.currentTimeMillis() - bStart);
        result.put("business_results", formatResults(businessResults));

        // 3. 合并检索
        long mStart = System.currentTimeMillis();
        List<TextSegment> mergedResults = hybridRetriever.vectorSearch(q, 5, "all");
        result.put("merged_time", System.currentTimeMillis() - mStart);
        result.put("merged_results", formatResults(mergedResults));

        return result;
    }

    /**
     * 测试4：带分数的详细检索
     * http://localhost:8080/test/hybrid/scored?q=育儿嫂
     */
    @GetMapping("/scored")
    public Map<String, Object> scoredSearch(@RequestParam String q) {
        Map<String, Object> result = new HashMap<>();

        // 获取知识库的带分数结果
        List<EmbeddingMatch<TextSegment>> knowledgeMatches = hybridRetriever.vectorSearchWithScores(q, 10, "knowledge");
        result.put("knowledge_matches", formatMatches(knowledgeMatches));

        // 获取业务库的带分数结果
        List<EmbeddingMatch<TextSegment>> businessMatches = hybridRetriever.vectorSearchWithScores(q, 10, "business");
        result.put("business_matches", formatMatches(businessMatches));

        // 获取带分数的BM25结果
        List<BM25Retriever.ScoredResult> bm25Scored = bm25Retriever.retrieveWithScores(q, 10);
        result.put("bm25_matches", formatBM25Scored(bm25Scored));

        // 获取混合检索的分数
        Map<TextSegment, Double> hybridScores = hybridRetriever.searchWithScores(q);
        result.put("hybrid_scores", formatScores(hybridScores));

        return result;
    }

    /**
     * 测试5：批量测试用例
     * http://localhost:8080/test/hybrid/batch
     */
    @GetMapping("/batch")
    public Map<String, Object> batchTest() {
        // 预定义的测试用例
        String[] testQueries = {
                "发票怎么开",
                "月嫂多少钱",
                "育儿嫂要求",
                "钟点工怎么收费",
                "阿姨需要什么证书",
                "怎么退款",
                "保险怎么买",
                "我叫张三",
                "我想找保姆",
                "你们有什么服务"
        };

        Map<String, Object> results = new LinkedHashMap<>();

        for (String query : testQueries) {
            Map<String, Object> queryResult = new HashMap<>();

            // 分别获取三种检索的结果
            List<TextSegment> knowledgeResults = hybridRetriever.vectorSearch(query, 3, "knowledge");
            List<TextSegment> businessResults = hybridRetriever.vectorSearch(query, 3, "business");
            List<TextSegment> vectorResults = hybridRetriever.vectorSearch(query, 3, "all");
            List<TextSegment> bm25Results = bm25Retriever.retrieve(query, 3);
            List<TextSegment> hybridResults = hybridRetriever.search(query, 3);

            queryResult.put("knowledge_count", knowledgeResults.size());
            queryResult.put("business_count", businessResults.size());
            queryResult.put("vector_count", vectorResults.size());
            queryResult.put("bm25_count", bm25Results.size());
            queryResult.put("hybrid_count", hybridResults.size());

            results.put(query, queryResult);
        }

        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("total_queries", testQueries.length);
        finalResult.put("results", results);

        return finalResult;
    }

    /**
     * 测试6：语义相关性测试
     * http://localhost:8080/test/hybrid/relevance?q=我叫张三
     */
    @GetMapping("/relevance")
    public String testRelevance(@RequestParam String q) {
        StringBuilder sb = new StringBuilder();
        sb.append("查询：").append(q).append("\n\n");

        // 混合检索结果
        List<TextSegment> results = hybridRetriever.search(q, 5);

        sb.append("混合检索结果：\n");
        for (int i = 0; i < results.size(); i++) {
            TextSegment segment = results.get(i);
            String preview = segment.text().length() > 100
                    ? segment.text().substring(0, 100) + "..."
                    : segment.text();
            sb.append(i + 1).append(". ").append(preview).append("\n");

            // 判断来源
            String source = detectSource(segment.text());
            sb.append("   [来源: ").append(source).append("]\n");
        }

        return sb.toString().replace("\n", "<br>");
    }

    // 辅助方法：检测文本来源
    private String detectSource(String text) {
        if (text.contains("家政服务项目") || text.contains("服务分类") || text.contains("服务标签")) {
            return "知识库";
        } else if (text.contains("家政服务人员") || text.contains("历史订单") || text.contains("用户真实评价")) {
            return "业务库";
        } else {
            return "未知";
        }
    }

    // 辅助方法：格式化结果
    private List<Map<String, String>> formatResults(List<TextSegment> results) {
        List<Map<String, String>> formatted = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Map<String, String> item = new HashMap<>();
            item.put("rank", String.valueOf(i + 1));
            item.put("preview", results.get(i).text().substring(0, Math.min(50, results.get(i).text().length())));
            item.put("source", detectSource(results.get(i).text()));
            formatted.add(item);
        }
        return formatted;
    }

    private List<Map<String, Object>> formatMatches(List<EmbeddingMatch<TextSegment>> matches) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("score", match.score());
            item.put("preview", match.embedded().text().substring(0, Math.min(50, match.embedded().text().length())));
            item.put("source", detectSource(match.embedded().text()));
            formatted.add(item);
        }
        return formatted;
    }

    private List<Map<String, Object>> formatBM25Scored(List<BM25Retriever.ScoredResult> scored) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("score", scored.get(i).getScore());
            item.put("preview", scored.get(i).getSegment().text().substring(0, Math.min(50, scored.get(i).getSegment().text().length())));
            item.put("source", detectSource(scored.get(i).getSegment().text()));
            formatted.add(item);
        }
        return formatted;
    }

    private List<Map<String, Object>> formatScores(Map<TextSegment, Double> scores) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        int i = 1;
        for (Map.Entry<TextSegment, Double> entry : scores.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i++);
            item.put("score", entry.getValue());
            item.put("preview", entry.getKey().text().substring(0, Math.min(50, entry.getKey().text().length())));
            item.put("source", detectSource(entry.getKey().text()));
            formatted.add(item);
        }
        return formatted;
    }

    private <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.retainAll(set2);
        return result;
    }

    @GetMapping("/search-text")
    public String testSearchText(@RequestParam String q) {
        StringBuilder sb = new StringBuilder();
        sb.append("查询: ").append(q).append("\n\n");

        try {
            List<TextSegment> results = hybridRetriever.search(q, 5);
            sb.append("结果数: ").append(results.size()).append("\n\n");

            for (int i = 0; i < results.size(); i++) {
                TextSegment segment = results.get(i);
                sb.append(i + 1).append(". ").append(segment.text()).append("\n");
                sb.append("   [来源: ").append(detectSource(segment.text())).append("]\n\n");
            }
        } catch (Exception e) {
            sb.append("错误: ").append(e.getMessage());
        }

        return sb.toString().replace("\n", "<br>");
    }

    @GetMapping("/compare-text")
    public String testCompareText(@RequestParam String q) {
        StringBuilder sb = new StringBuilder();
        sb.append("查询: ").append(q).append("\n");
        sb.append("========================\n\n");

        // 1. 知识库检索
        long kStart = System.currentTimeMillis();
        List<TextSegment> knowledgeResults = hybridRetriever.vectorSearch(q, 5, "knowledge");
        sb.append("知识库检索 [").append(System.currentTimeMillis() - kStart).append("ms]:\n");
        for (int i = 0; i < knowledgeResults.size(); i++) {
            sb.append("  ").append(i + 1).append(". ")
                    .append(truncate(knowledgeResults.get(i).text(), 50)).append("\n");
        }
        sb.append("\n");

        // 2. 业务库检索
        long bStart = System.currentTimeMillis();
        List<TextSegment> businessResults = hybridRetriever.vectorSearch(q, 5, "business");
        sb.append("业务库检索 [").append(System.currentTimeMillis() - bStart).append("ms]:\n");
        for (int i = 0; i < businessResults.size(); i++) {
            sb.append("  ").append(i + 1).append(". ")
                    .append(truncate(businessResults.get(i).text(), 50)).append("\n");
        }
        sb.append("\n");

        // 3. 混合检索
        long hStart = System.currentTimeMillis();
        List<TextSegment> hybridResults = hybridRetriever.search(q, 5);
        sb.append("混合检索 [").append(System.currentTimeMillis() - hStart).append("ms]:\n");
        for (int i = 0; i < hybridResults.size(); i++) {
            sb.append("  ").append(i + 1).append(". ")
                    .append(truncate(hybridResults.get(i).text(), 50)).append("\n");
        }

        return sb.toString().replace("\n", "<br>");
    }

    private String truncate(String text, int length) {
        if (text.length() <= length) return text;
        return text.substring(0, length) + "...";
    }

    @GetMapping("/diagnose-bm25")
    public Map<String, Object> diagnoseBM25(@RequestParam String q) {
        Map<String, Object> result = new HashMap<>();

        // 1. 检查BM25检索器状态
        result.put("bm25_initialized", bm25Retriever != null);
        result.put("bm25_index_size", bm25Retriever.getIndexSize());

        // 2. 尝试BM25检索
        try {
            long start = System.currentTimeMillis();
            List<TextSegment> bm25Results = bm25Retriever.retrieve(q, 5);
            result.put("bm25_success", true);
            result.put("bm25_count", bm25Results.size());
            result.put("bm25_time_ms", System.currentTimeMillis() - start);

            List<String> previews = new ArrayList<>();
            for (TextSegment seg : bm25Results) {
                previews.add(seg.text().substring(0, Math.min(50, seg.text().length())));
            }
            result.put("bm25_previews", previews);

        } catch (Exception e) {
            result.put("bm25_success", false);
            result.put("bm25_error", e.getMessage());
            log.error("BM25诊断失败", e);
        }

        // 3. 向量检索对比（从两个库）
        try {
            long start = System.currentTimeMillis();
            List<TextSegment> knowledgeResults = hybridRetriever.vectorSearch(q, 5, "knowledge");
            List<TextSegment> businessResults = hybridRetriever.vectorSearch(q, 5, "business");
            result.put("knowledge_count", knowledgeResults.size());
            result.put("business_count", businessResults.size());
            result.put("vector_time_ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            result.put("vector_error", e.getMessage());
        }

        return result;
    }
}