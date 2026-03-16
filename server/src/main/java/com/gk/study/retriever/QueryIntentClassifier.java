package com.gk.study.retriever;

import com.gk.study.retriever.QueryIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class QueryIntentClassifier {

    /**
     * 分类查询意图
     * @param query 用户查询字符串
     * @return 查询意图枚举
     */
    public QueryIntent classify(String query) {
        if (query == null || query.trim().isEmpty()) {
            return QueryIntent.UNKNOWN;
        }

        String lowerQuery = query.toLowerCase().trim();

        // 1. 问候语识别
        if (containsAny(lowerQuery, "你好", "您好", "在吗", "hello", "hi", "在不在")) {
            return QueryIntent.GREETING;
        }

        // 2. 自我介绍识别
        if (containsAny(lowerQuery, "我叫", "我是", "本人", "name is", "i am")) {
            return QueryIntent.SELF_INTRO;
        }

        // 3. 服务列表查询
        if (containsAny(lowerQuery,
                "有哪些服务",
                "什么服务",
                "服务项目",
                "服务类型",
                "你们提供什么",
                "有什么服务",
                "服务有哪些",
                "提供服务",
                "业务范围")) {
            return QueryIntent.SERVICE_LIST;
        }

        // 4. 价格查询
        if (containsAny(lowerQuery,
                "多少钱",
                "价格",
                "收费",
                "价位",
                "怎么收费",
                "费用",
                "多少",
                "价格表",
                "收费标准")) {
            return QueryIntent.PRICE_QUERY;
        }

        // 5. 阿姨推荐
        if (containsAny(lowerQuery,
                "推荐阿姨",
                "找阿姨",
                "找个保姆",
                "育儿嫂",
                "月嫂",
                "推荐",
                "介绍个",
                "有阿姨吗",
                "找保姆",
                "找月嫂",
                "找育儿嫂",
                "好阿姨")) {
            return QueryIntent.PROVIDER_RECOMMEND;
        }

        // 6. 阿姨评价
        if (containsAny(lowerQuery,
                "评价",
                "怎么样",
                "好不好",
                "口碑",
                "用户评价",
                "评分",
                "反馈",
                "用户反馈",
                "评价如何")) {
            return QueryIntent.PROVIDER_REVIEW;
        }

        // 7. 常见问题
        if (containsAny(lowerQuery,
                "怎么",
                "如何",
                "发票",
                "退款",
                "取消",
                "签约",
                "流程",
                "多久",
                "需要什么",
                "怎么办",
                "如何操作")) {
            return QueryIntent.FAQ_QUERY;
        }

        // 8. 标签查询
        if (containsAny(lowerQuery,
                "有驾照",
                "有证书",
                "有经验",
                "技能",
                "标签",
                "会英语",
                "会开车",
                "特殊技能",
                "擅长")) {
            return QueryIntent.TAG_QUERY;
        }

        return QueryIntent.UNKNOWN;
    }

    /**
     * 判断字符串是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    /**
     * 获取意图的中文描述（用于日志）
     */
    public String getIntentDescription(QueryIntent intent) {
        if (intent == null) {
            return "未知意图";
        }

        switch (intent) {
            case SERVICE_LIST:
                return "服务列表查询";
            case PRICE_QUERY:
                return "价格查询";
            case PROVIDER_RECOMMEND:
                return "阿姨推荐";
            case PROVIDER_REVIEW:
                return "阿姨评价";
            case FAQ_QUERY:
                return "常见问题";
            case TAG_QUERY:
                return "标签查询";
            case GREETING:
                return "问候语";
            case SELF_INTRO:
                return "自我介绍";
            default:
                return "未知意图";
        }
    }

    /**
     * 判断是否为不需要检索的意图
     */
    public boolean isNonRetrievalIntent(QueryIntent intent) {
        return intent == QueryIntent.GREETING ||
                intent == QueryIntent.SELF_INTRO ||
                intent == QueryIntent.UNKNOWN;
    }
}