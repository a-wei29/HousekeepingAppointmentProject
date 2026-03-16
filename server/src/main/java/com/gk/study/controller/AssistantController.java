package com.gk.study.controller;

import com.gk.study.service.impl.RagAssistantService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {
    @Data
    public static class ChatRequest {
        private String message;     // 用户问题
        private String userId;      // 用户ID，用于区分不同会话
    }

    private final RagAssistantService assistantService;

    /**
     * 普通对话（测试用）
     */
    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        log.info("收到普通对话请求: {}", request.getMessage());
        return assistantService.chat(request.getMessage());
    }

    /**
     * 智能客服（带RAG）
     */
    @PostMapping("/ask")
    public String ask(@RequestBody ChatRequest request) {
        log.info("收到客服咨询: {}", request.getMessage());
        String answer = assistantService.assistantChat(request.getUserId(), request.getMessage());
        log.info("客服回答: {}", answer);
        return answer;
    }

    /**
     * 服务咨询（指定服务类型）
     */
    @PostMapping("/service/{serviceType}")
    public String serviceChat(
            @PathVariable String serviceType,
            @RequestBody ChatRequest request) {
        log.info("收到{}服务咨询: {}", serviceType, request.getMessage());
        return assistantService.serviceChat(request.getUserId(), serviceType, request.getMessage());
    }
}
