package com.gk.study.retriever;

import com.gk.study.retriever.QueryIntent;
import com.gk.study.retriever.QueryIntentClassifier;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IntentAwareRetriever {

    private final HybridRetriever hybridRetriever;
    private final QueryIntentClassifier intentClassifier;

    public IntentAwareRetriever(HybridRetriever hybridRetriever,
                                QueryIntentClassifier intentClassifier) {
        this.hybridRetriever = hybridRetriever;
        this.intentClassifier = intentClassifier;
    }

    public List<TextSegment> retrieve(String query, int topK) {
        // 1. 识别查询意图
        QueryIntent intent = intentClassifier.classify(query);
        String intentDesc = intentClassifier.getIntentDescription(intent);
        log.info("识别到查询意图: [{}] {}, 原始查询: {}", intent, intentDesc, query);

        // 2. 如果是不需要检索的意图，直接返回空列表
        if (intentClassifier.isNonRetrievalIntent(intent)) {
            log.info("意图无需检索，返回空列表");
            return List.of();
        }

        // 3. 根据意图进行检索
        List<TextSegment> results = hybridRetriever.search(query, topK * 2);

        // 4. 根据意图过滤结果
        List<TextSegment> filteredResults = filterByIntent(results, intent);

        log.info("意图过滤前: {}, 过滤后: {}", results.size(), filteredResults.size());

        // 5. 记录过滤后的内容类型
        if (!filteredResults.isEmpty()) {
            log.info("过滤后的内容类型分布:");
            filteredResults.stream()
                    .map(segment -> extractType(segment.text()))
                    .collect(Collectors.groupingBy(type -> type, Collectors.counting()))
                    .forEach((type, count) -> log.info("  {}: {}条", type, count));
        }

        return filteredResults.stream().limit(topK).collect(Collectors.toList());
    }

    private List<TextSegment> filterByIntent(List<TextSegment> results, QueryIntent intent) {
        return results.stream()
                .filter(segment -> {
                    String text = segment.text();

                    switch (intent) {
                        case SERVICE_LIST:
                            // 只保留服务项目相关的内容
                            return text.contains("【类型:服务项目】") ||
                                    text.contains("【类型:服务分类】");

                        case PRICE_QUERY:
                            // 只保留价格相关的内容
                            return text.contains("价格") ||
                                    text.contains("收费") ||
                                    text.contains("元/小时") ||
                                    text.contains("多少钱");

                        case PROVIDER_RECOMMEND:
                            // 只保留阿姨相关信息
                            return text.contains("【类型:阿姨信息】") ||
                                    text.contains("家政服务人员");

                        case PROVIDER_REVIEW:
                            // 只保留评价相关内容
                            return text.contains("【类型:用户评价】");

                        case FAQ_QUERY:
                            // 只保留FAQ相关内容
                            return text.contains("Q:") ||
                                    text.contains("常见问题") ||
                                    text.contains("怎么") ||
                                    text.contains("如何");

                        case TAG_QUERY:
                            // 只保留标签相关内容
                            return text.contains("【类型:服务标签】");

                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 提取文本类型
     */
    private String extractType(String text) {
        if (text.contains("【类型:服务项目】")) return "服务项目";
        if (text.contains("【类型:阿姨信息】")) return "阿姨信息";
        if (text.contains("【类型:用户评价】")) return "用户评价";
        if (text.contains("【类型:服务标签】")) return "服务标签";
        if (text.contains("【类型:服务分类】")) return "服务分类";
        if (text.contains("【类型:历史订单】")) return "历史订单";
        return "未知类型";
    }
}