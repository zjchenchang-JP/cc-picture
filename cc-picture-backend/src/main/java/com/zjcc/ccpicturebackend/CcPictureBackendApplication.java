package com.zjcc.ccpicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
// @SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class}) // 启动类排除依赖,关闭分库分表
// 开启 Spring 基于 AspectJ 注解的自动代理
// 加了 exposeProxy = true 后,Spring 会把当前正在执行的代理对象塞进一个 ThreadLocal(AopContext),在目标方法里就能手动把它"捞"出来:
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync // 启动@Async 异步方法
public class CcPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CcPictureBackendApplication.class, args);
    }

}
