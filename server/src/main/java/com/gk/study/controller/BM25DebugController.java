package com.gk.study.controller;

import com.gk.study.retriever.BM25Retriever;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/debug/bm25")
public class BM25DebugController {

    @Autowired
    private BM25Retriever bm25Retriever;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("initialized", bm25Retriever.isInitialized());
        status.put("indexSize", bm25Retriever.getIndexSize());

        return status;
    }

    @GetMapping("/test-search")
    public Map<String, Object> testSearch(@RequestParam String q) {
        Map<String, Object> result = new HashMap<>();

        long start = System.currentTimeMillis();
        List<TextSegment> results = bm25Retriever.retrieve(q, 5);
        long cost = System.currentTimeMillis() - start;

        result.put("query", q);
        result.put("resultCount", results.size());
        result.put("timeMs", cost);

        if (!results.isEmpty()) {
            result.put("firstResult", results.get(0).text().substring(0,
                    Math.min(100, results.get(0).text().length())));
        }

        return result;
    }

    @GetMapping("/force-refresh")
    public String forceRefresh() {
        try {
            bm25Retriever.refreshIndex();
            return "刷新完成，索引大小: " + bm25Retriever.getIndexSize();
        } catch (Exception e) {
            return "刷新失败: " + e.getMessage();
        }
    }
}