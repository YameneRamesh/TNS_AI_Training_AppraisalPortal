package com.tns.appraisal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for asynchronous email notification processing.
 * Configures a thread pool executor for handling background tasks.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Configures the thread pool executor for async email notifications.
     * - Core pool size: 2 threads (minimum active threads)
     * - Max pool size: 5 threads (maximum concurrent threads)
     * - Queue capacity: 100 (pending tasks before rejection)
     * - Thread name prefix: "async-email-" for easy identification in logs
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - minimum number of threads
        executor.setCorePoolSize(2);
        
        // Maximum pool size - max concurrent threads
        executor.setMaxPoolSize(5);
        
        // Queue capacity - pending tasks before rejection
        executor.setQueueCapacity(100);
        
        // Thread name prefix for logging
        executor.setThreadNamePrefix("async-email-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Timeout for shutdown (30 seconds)
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}
