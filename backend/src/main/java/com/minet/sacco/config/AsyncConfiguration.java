package com.minet.sacco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AsyncConfiguration enables asynchronous processing for email sending and other long-running tasks.
 * This configuration is critical for the email notification system to work properly.
 * 
 * Without @EnableAsync, methods decorated with @Async will execute synchronously in the calling thread,
 * blocking the API response until email sending completes.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);
    
    /**
     * Configure the thread pool for async email processing
     * - Processes emails without blocking API responses
     * - Handles burst of notifications efficiently
     * - Prevents thread exhaustion with queue capacity
     */
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core threads: always running (for welcome emails, password resets)
        executor.setCorePoolSize(5);
        
        // Max threads: scale up during burst notifications (loan approvals, bulk updates)
        executor.setMaxPoolSize(20);
        
        // Queue capacity: buffer for spike in notifications
        // If queue fills up, additional tasks will create new threads up to maxPoolSize
        executor.setQueueCapacity(100);
        
        // Thread naming for debugging (visible in logs and thread dumps)
        executor.setThreadNamePrefix("email-async-");
        
        // Wait for tasks to complete on shutdown (graceful shutdown)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Timeout for shutdown (wait up to 60 seconds for pending emails)
        executor.setAwaitTerminationSeconds(60);
        
        // RejectedExecutionHandler - use CallerRunsPolicy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        logger.info("✅ Async Task Executor configured: " +
            "corePoolSize=5, maxPoolSize=20, queueCapacity=100, " +
            "waitForCompletion=true, awaitTermination=60s");
        
        return executor;
    }
}
