package com.gk.study.retriever;

import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class BM25Retriever {

    private List<TextSegment> allSegments;  // 去掉 final，允许更新
    private IndexSearcher indexSearcher;
    private Analyzer analyzer;
    private Directory directory;
    private IndexReader reader;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public BM25Retriever(List<TextSegment> allSegments) {
        this.allSegments = allSegments;
        log.info("========== BM25Retriever 构造 ==========");
        log.info("初始知识库大小: {}", allSegments != null ? allSegments.size() : 0);

        if (allSegments != null && !allSegments.isEmpty()) {
            log.info("知识库预览（前3条）:");
            for (int i = 0; i < Math.min(3, allSegments.size()); i++) {
                String text = allSegments.get(i).text();
                log.info("  [{}] {}", i, text.substring(0, Math.min(50, text.length())));
            }
        }
    }

    /**
     * 更新 allSegments 数据
     */
    public void updateSegments(List<TextSegment> newSegments) {
        log.info("更新 BM25 数据，新数据量: {}", newSegments != null ? newSegments.size() : 0);
        this.allSegments = newSegments;
    }

    /**
     * 刷新索引 - 在数据更新后调用
     */
    public synchronized void refreshIndex() {
        log.info("========== 主动刷新 BM25 索引 ==========");

        if (allSegments == null || allSegments.isEmpty()) {
            log.warn("allSegments 为空，无法刷新索引");
            return;
        }

        log.info("当前 allSegments 大小: {}", allSegments.size());

        try {
            // 清理旧索引
            cleanup();

            // 重置状态
            initialized.set(false);

            // 重建索引
            buildIndex();

            initialized.set(true);
            log.info("========== BM25 索引刷新成功 ==========");
            log.info("索引文档数: {}", reader != null ? reader.numDocs() : 0);

        } catch (Exception e) {
            log.error("刷新索引失败", e);
        }
    }

    /**
     * 构建索引
     */
    private void buildIndex() throws IOException {
        log.info("开始构建 BM25 索引，数据量: {}", allSegments.size());

        this.analyzer = new SmartChineseAnalyzer();
        this.directory = new RAMDirectory();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new BM25Similarity());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            int successCount = 0;
            for (int i = 0; i < allSegments.size(); i++) {
                TextSegment segment = allSegments.get(i);
                if (segment == null || segment.text() == null || segment.text().trim().isEmpty()) {
                    continue;
                }

                Document doc = new Document();
                doc.add(new TextField("content", segment.text(), Field.Store.YES));
                doc.add(new StoredField("index", i));
                writer.addDocument(doc);
                successCount++;
            }
            writer.commit();
            log.info("索引创建完成，成功索引 {} 个文档", successCount);
        }

        this.reader = DirectoryReader.open(directory);
        this.indexSearcher = new IndexSearcher(reader);
        this.indexSearcher.setSimilarity(new BM25Similarity());
    }

    /**
     * BM25检索
     */
    public List<TextSegment> retrieve(String query, int topK) {
        if (!initialized.get()) {
            log.warn("BM25 索引未初始化，尝试初始化");
            refreshIndex();
        }

        if (!initialized.get() || query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            QueryParser parser = new QueryParser("content", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);

            String processedQuery = preprocessChineseQuery(query);
            Query luceneQuery = parser.parse(processedQuery);

            TopDocs topDocs = indexSearcher.search(luceneQuery, topK);

            List<TextSegment> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                int index = Integer.parseInt(doc.get("index"));
                if (index < allSegments.size()) {
                    results.add(allSegments.get(index));
                }
            }

            return results;

        } catch (Exception e) {
            log.error("BM25检索失败: {}", query, e);
            return new ArrayList<>();
        }
    }

    /**
     * 带分数的检索
     */
    public List<ScoredResult> retrieveWithScores(String query, int topK) {
        if (!initialized.get()) {
            refreshIndex();
        }

        List<ScoredResult> results = new ArrayList<>();
        if (!initialized.get()) {
            return results;
        }

        try {
            QueryParser parser = new QueryParser("content", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);

            String processedQuery = preprocessChineseQuery(query);
            Query luceneQuery = parser.parse(processedQuery);

            TopDocs topDocs = indexSearcher.search(luceneQuery, topK);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                int index = Integer.parseInt(doc.get("index"));
                if (index < allSegments.size()) {
                    results.add(new ScoredResult(allSegments.get(index), scoreDoc.score));
                }
            }

        } catch (Exception e) {
            log.error("BM25带分数检索失败", e);
        }

        return results;
    }

    private String preprocessChineseQuery(String query) {
        return query.replaceAll("[^\u4e00-\u9fa5a-zA-Z0-9\\s]", " ");
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public int getIndexSize() {
        return reader != null ? reader.numDocs() : 0;
    }

    @lombok.Data
    public static class ScoredResult {
        private final TextSegment segment;
        private final float score;
        public ScoredResult(TextSegment segment, float score) {
            this.segment = segment;
            this.score = score;
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (reader != null) reader.close();
            if (directory != null) directory.close();
        } catch (IOException e) {
            log.error("清理资源失败", e);
        }
    }
}