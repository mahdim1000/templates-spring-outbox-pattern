package com.github.mahdim1000.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mahdim1000.core.DefaultOutboxManager;
import com.github.mahdim1000.core.OutboxManager;


import com.github.mahdim1000.core.OutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.Executor;

/**
 * Auto-configuration for the Outbox Pattern library.
 * 
 * This configuration is automatically loaded when the library is on the classpath.
 * It sets up all necessary components with sensible defaults.
 */
@Configuration
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
@EnableConfigurationProperties(OutboxProperties.class)
@ComponentScan(basePackages = {
    "io.outboxpattern.core",
    "io.outboxpattern.publisher",
    "io.outboxpattern.domain"
})
public class OutboxAutoConfiguration {

    /**
     * ObjectMapper configured for outbox operations.
     * Includes JSR310 module for LocalDateTime serialization.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ObjectMapper outboxObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Thread pool executor for outbox processing.
     * Configured with reasonable defaults for concurrent processing.
     */
    @Bean(name = "outboxTaskExecutor")
    @ConditionalOnMissingBean(name = "outboxTaskExecutor")
    public Executor outboxTaskExecutor(OutboxProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("Outbox-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Main outbox manager facade.
     * This is the primary entry point for library users.
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxManager outboxManager(OutboxService outboxService,
                                       ObjectMapper objectMapper,
                                       OutboxProperties properties) {
        return new DefaultOutboxManager(outboxService, objectMapper, properties);
    }

}
