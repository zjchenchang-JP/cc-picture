package com.zjcc.ccpicturebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync // 启动@Async 异步方法
public class CcPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcPictureBackendApplication.class, args);
    }

}
