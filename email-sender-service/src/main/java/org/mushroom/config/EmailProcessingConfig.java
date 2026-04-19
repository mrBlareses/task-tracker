package org.mushroom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EmailProcessingConfig {

    @Bean(name = "emailProcessingExecutor")
    public ThreadPoolTaskExecutor emailProcessingExecutor(
            @Value("${email-processing.worker-threads:4}") int workerThreads
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workerThreads);
        executor.setMaxPoolSize(workerThreads);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-processing-");
        executor.initialize();
        return executor;
    }
}