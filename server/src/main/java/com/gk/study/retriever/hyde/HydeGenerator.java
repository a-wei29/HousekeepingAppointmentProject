package com.gk.study.retriever.hyde;

import com.gk.study.service.Assistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HydeGenerator {

    private final HydeAssistant hydeAssistant;

    public HydeGenerator(ChatModel chatModel) {
        this.hydeAssistant = AiServices.create(HydeAssistant.class, chatModel);
        log.info("HyDE生成器初始化完成");
    }

    /**
     * 根据用户问题生成假设性文档
     */
    public String generateHypotheticalDocument(String query) {
        log.info("生成假设文档，原始问题: {}", query);

        String hydeDoc = hydeAssistant.generateDocument(query);
        log.debug("生成的假设文档: {}", hydeDoc);

        return hydeDoc;
    }

    /**
     * HyDE专用接口
     */
    interface HydeAssistant {
        @dev.langchain4j.service.SystemMessage({
                "你是一个专业的家政知识库助手。",
                "请根据用户的问题，生成一段详细的假设性文档。",
                "",
                "【生成规则】",
                "1. 文档应该包含问题的答案形式",
                "2. 使用专业的家政服务术语",
                "3. 文档要详细、完整、结构化",
                "4. 不要包含任何解释或额外说明",
                "5. 直接输出文档内容",
                "",
                "【示例1】",
                "用户问题：你们有哪些服务？",
                "假设文档：",
                "【家政服务项目】",
                "1. 保洁服务：日常保洁、深度保洁、开荒保洁",
                "2. 育儿服务：月嫂、育儿嫂、早教育儿",
                "3. 养老服务：老人陪护、康复护理",
                "4. 钟点工服务：按小时计费，灵活安排",
                "",
                "【示例2】",
                "用户问题：月嫂多少钱？",
                "假设文档：",
                "【月嫂收费标准】",
                "- 初级月嫂：8000-10000元/月",
                "- 中级月嫂：10000-13000元/月",
                "- 高级月嫂：13000-15000元/月",
                "价格根据经验、证书、服务内容浮动",
                "",
                "【示例3】",
                "用户问题：推荐个育儿嫂",
                "假设文档：",
                "【育儿嫂推荐】",
                "姓名：李淑芬",
                "年龄：35岁",
                "经验：5年育儿嫂经验",
                "证书：高级育婴师证、健康证",
                "擅长：新生儿护理、辅食制作、早教启蒙",
                "用户评分：4.9/5.0",
                "服务状态：可接单"
        })
        String generateDocument(@dev.langchain4j.service.UserMessage String query);
    }
}
