package com.minet.sacco.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            configuration.baselineOnMigrate(true);
            configuration.outOfOrder(true);
            configuration.validateOnMigrate(false);
        };
    }

    /**
     * Repair before migrate to clear any failed migration entries from schema history.
     * This handles cases where a migration failed and left a broken entry in flyway_schema_history.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
