package com.gk.study.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag.hybrid")
public class HybridSearchConfig {
    private int vectorSearchResults = 10;      // 向量检索返回数量
    private int bm25SearchResults = 10;        // BM25检索返回数量
    private int finalResults = 5;               // 最终返回数量
    private int rrfK = 60;                      // RRF常数
    private double minScore = 0.5;              // 最低分数阈值
}