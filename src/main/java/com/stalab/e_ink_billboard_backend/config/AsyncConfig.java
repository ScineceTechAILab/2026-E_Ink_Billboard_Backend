package com.stalab.e_ink_billboard_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean("videoExecutor")
    public ThreadPoolTaskExecutor videoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据你的CPU核数设置，比如 4核CPU 就设 4
        executor.setCorePoolSize(4);
        // 最大线程数：突发流量时的最大线程
        executor.setMaxPoolSize(8);
        // 队列大小：允许多少个视频排队，超过了就拒绝
        executor.setQueueCapacity(100);
        // 线程名称前缀
        executor.setThreadNamePrefix("Video-Process-");
        // 拒绝策略：队列满了之后由调用者线程执行（防止丢任务）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
