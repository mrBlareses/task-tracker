package org.mushroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class EmailProcessingConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService emailProcessingVirtualThreadExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "emailProcessingExecutor")
    public AsyncTaskExecutor emailProcessingExecutor(
            ExecutorService emailProcessingVirtualThreadExecutorService
    ) {
        return new TaskExecutorAdapter(emailProcessingVirtualThreadExecutorService);
    }
}