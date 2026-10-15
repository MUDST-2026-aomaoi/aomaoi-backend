package com.aomaoi.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Exit the dedicated Kubernetes migration Job after Liquibase and seed complete. */
@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.migration-job", havingValue = "true")
public class MigrationJobRunner implements CommandLineRunner {
    private final ConfigurableApplicationContext context;

    @Override
    public void run(String... args) {
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
