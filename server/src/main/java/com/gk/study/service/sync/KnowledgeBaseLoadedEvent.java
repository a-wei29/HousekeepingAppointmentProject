package com.gk.study.service.sync;

import org.springframework.context.ApplicationEvent;

/**
 * 知识库加载完成事件
 */
public class KnowledgeBaseLoadedEvent extends ApplicationEvent {

    private final int segmentCount;

    public KnowledgeBaseLoadedEvent(Object source, int segmentCount) {
        super(source);
        this.segmentCount = segmentCount;
    }

    public int getSegmentCount() {
        return segmentCount;
    }
}