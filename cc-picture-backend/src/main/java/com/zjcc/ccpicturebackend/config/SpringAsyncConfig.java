package com.zjcc.ccpicturebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程池
 * @author <a href="https://github.com/zjchenchang-JP">zjchenchang</a>
 */
@Configuration
public class SpringAsyncConfig {
    
    @Bean("ccPictureExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        // 优雅关闭：应用停机时等正在跑的任务跑完，不强制中断
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ccPicture-batch-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
