package com.gk.study.common;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CloseLogger {
    @PostConstruct
    public void init() {
        System.out.println(">>> CloseLogger bean has been initialized");
    }

    @EventListener
    public void onClose(ContextClosedEvent ev) {
        // 这里打印堆栈，IDEA 控制台就能看到是哪行代码触发的 close()
        new Exception(">>> ApplicationContext is closing <<<").printStackTrace();
    }
}