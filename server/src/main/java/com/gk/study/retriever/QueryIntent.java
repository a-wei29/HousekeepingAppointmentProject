package com.gk.study.retriever;

public enum QueryIntent {
    SERVICE_LIST,      // 服务列表查询
    PRICE_QUERY,       // 价格查询
    PROVIDER_RECOMMEND, // 阿姨推荐
    PROVIDER_REVIEW,    // 阿姨评价
    FAQ_QUERY,         // 常见问题
    TAG_QUERY,         // 标签查询
    GREETING,          // 问候语
    SELF_INTRO,        // 自我介绍
    UNKNOWN
}
