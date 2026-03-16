package com.gk.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class MySpringApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MySpringApplication.class);

        // 1) 打印所有 Spring 事件（你已经有了）
        app.addListeners((ApplicationListener<ApplicationEvent>) ev ->
                System.out.println(">>> Spring Event: " + ev.getClass().getSimpleName())
        );

        // 2) 如果启动失败，打印异常堆栈
        app.addListeners((ApplicationListener<ApplicationFailedEvent>) ev -> {
            System.err.println(">>> Application failed with exception:");
            ev.getException().printStackTrace();
        });

        app.run(args);
    }

}
