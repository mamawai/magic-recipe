package com.mawai.mrcrawler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置类
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 配置异步任务线程池
     * 实现AsyncConfigurer接口的getAsyncExecutor方法
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 获取CPU核心数
        int processors = Runtime.getRuntime().availableProcessors();
        log.info("当前系统CPU核心数: {}", processors);
        
        // IO密集型任务，核心线程数 = CPU核心数 + 1
        executor.setCorePoolSize(processors + 1);
        // 最大线程数 = 核心线程数 * 2
        executor.setMaxPoolSize((processors + 1) * 2);
        // 队列容量适中，避免占用过多内存
        executor.setQueueCapacity(150);
        // 线程名称前缀
        executor.setThreadNamePrefix("crawler-async-");
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待终止的时间
        executor.setAwaitTerminationSeconds(60);
        // 初始化线程池
        executor.initialize();
        return executor;
    }
    
    /**
     * 异常处理
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("异步任务执行异常，方法：{}，异常：{}", method.getName(), ex.getMessage(), ex);
        };
    }
} 