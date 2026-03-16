package com.gk.study.controller;

import com.gk.study.service.sync.BusinessDataSync;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/sync")
public class DataSyncController {

    @Autowired
    private BusinessDataSync businessDataSync;

    // 根据需要选择注入哪个 Store
    @Autowired
    @Qualifier("knowledgeEmbeddingStore")
    private EmbeddingStore<TextSegment> knowledgeStore;

    @Autowired
    @Qualifier("businessEmbeddingStore")
    private EmbeddingStore<TextSegment> businessStore;

    @PostMapping("/business")
    public String syncBusinessData() {
        businessDataSync.syncAllBusinessData();
        return "业务数据同步已触发";
    }

    @GetMapping("/stats")
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("知识库状态: ").append(knowledgeStore != null ? "可用" : "不可用").append("\n");
        sb.append("业务库状态: ").append(businessStore != null ? "可用" : "不可用");
        return sb.toString();
    }
}