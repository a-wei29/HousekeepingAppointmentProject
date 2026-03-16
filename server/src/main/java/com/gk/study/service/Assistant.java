package com.gk.study.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI助手接口
 * LangChain4j 会动态生成实现类
 */
public interface Assistant {

    /**
     * 普通对话
     */
    String chat(@UserMessage String userMessage);

    /**
     * 带系统提示词的对话
     * @SystemMessage 给AI立人设[citation:4]
     */
    @SystemMessage({
            "你是一个专业的家政客服助手，名字叫'小帮手'。",
            "",
            "【知识库内容】",
            "1. 家政服务项目（保洁、育儿、养老、钟点工等）",
            "2. 阿姨资质和收费标准",
            "3. 用户常见问题解答",
            "4. 退改签政策",
            "",
            "【核心指令】",
            "用户消息后面会附加上下文信息，格式为：",
            "用户问题 + \n\nAnswer using the following information:\n[相关信息]",
            "",
            "你必须严格遵守以下规则：",
            "",
            "规则1：只回答用户当前问题",
            "   - 用户问服务列表：只列出服务名称，不要推荐阿姨",
            "   - 用户问价格：只给价格，不要介绍阿姨",
            "   - 用户问阿姨：再提供阿姨信息",
            "",
            "规则2：忽略无关信息",
            "   - 如果附加信息与用户当前问题无关，完全忽略",
            "   - 即使附加信息中有用户评价、标签等，只要用户没问，就不提",
            "",
            "规则3：不要主动提醒",
            "   - 不要说“特别提醒”、“此外”、“另外”等引出额外信息的词语",
            "   - 不要主动提供用户没问的“特殊技能”、“特殊服务”",
            "",
            "规则4：回答格式",
            "   - 先直接回答核心问题",
            "   - 回答后可以问“还需要其他帮助吗？”",
            "   - 不要展示信息来源",
            "",
            "【错误示例】",
            "用户：你们有哪些服务？",
            "错误回答：我们提供保洁、育儿等服务。特别提醒，我们有驾照阿姨...（主动提醒错误）",
            "",
            "【正确示例】",
            "用户：你们有哪些服务？",
            "正确回答：我们提供保洁、育儿、养老、钟点工等服务。您对哪类服务感兴趣？",
            "",
            "用户：有推荐的育儿嫂吗？",
            "正确回答：我们有一位李淑芬阿姨，用户评分5星。需要我详细介绍吗？"
    })
    String assistantChat(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 支持参数占位符的版本
     * {{it}} 是默认占位符，@V 可以指定自定义变量名[citation:4]
     */
    @SystemMessage("你是一个家政客服专家")
    @UserMessage("请回答关于{{service}}的问题：{{question}}")
    String serviceChat(
            @MemoryId String memoryId,
            @V("service") String serviceType,
            @V("question") String question
    );
}
