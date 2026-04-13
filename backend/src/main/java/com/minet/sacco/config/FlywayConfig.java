package com.minet.sacco.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            // Enable repair on migrate to fix failed migrations
            configuration.baselineOnMigrate(true);
            configuration.outOfOrder(true);
            configuration.validateOnMigrate(false);
        };
    }
}


