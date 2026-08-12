package com.minet.sacco.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller for Render Monitoring
 * Provides endpoints to check service and database connectivity
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Basic health check endpoint
     * Returns 200 OK if service is running
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Minet SACCO Backend");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("environment", System.getenv("SPRING_PROFILES_ACTIVE"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Detailed health check with database connectivity
     * Used by Render for monitoring
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> checks = new HashMap<>();
        
        response.put("service", "Minet SACCO Backend");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("environment", System.getenv("SPRING_PROFILES_ACTIVE"));
        
        // Check database connectivity
        boolean dbConnected = false;
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                dbConnected = connection.isValid(5); // 5 second timeout
                checks.put("database", dbConnected ? "UP" : "DOWN");
            } catch (Exception e) {
                checks.put("database", "DOWN");
                checks.put("database_error", e.getMessage());
            }
        } else {
            checks.put("database", "NOT_CONFIGURED");
        }
        
        response.put("checks", checks);
        response.put("status", dbConnected ? "UP" : "DEGRADED");
        
        HttpStatus status = dbConnected ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Simple ping endpoint for load balancers
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * Version information endpoint
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        Map<String, String> versionInfo = new HashMap<>();
        versionInfo.put("application", "Minet SACCO Backend");
        versionInfo.put("version", "1.1.0");
        versionInfo.put("build", "production");
        versionInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return ResponseEntity.ok(versionInfo);
    }
}
