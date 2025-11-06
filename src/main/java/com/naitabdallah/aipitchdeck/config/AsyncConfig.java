package com.naitabdallah.aipitchdeck.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for background job processing.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "generationTaskExecutor")
    public Executor generationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("generation-");
        executor.initialize();
        return executor;
    }
}
