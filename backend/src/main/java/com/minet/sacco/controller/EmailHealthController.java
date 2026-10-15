package com.minet.sacco.controller;

import com.minet.sacco.service.EmailNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * EmailHealthController provides endpoints to verify and monitor email system health.
 * Used for deployment verification, monitoring, and troubleshooting.
 */
@RestController
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/email/health")
@CrossOrigin(origins = "*")
public class EmailHealthController {

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    /**
     * Get comprehensive email system health status
     * GET /api/email/health
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getEmailHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Email System");
        
        if (emailNotificationService == null) {
            health.put("status", "UNAVAILABLE");
            health.put("message", "Email service bean not found in application context");
            health.put("smtpConfigured", false);
            health.put("details", Map.of(
                "issue", "EmailNotificationService not initialized",
                "recommendation", "Check if spring-boot-starter-mail dependency is present"
            ));
            return ResponseEntity.status(503).body(health);
        }

        try {
            boolean smtpConnected = emailNotificationService.testSmtpConnection();
            
            if (smtpConnected) {
                health.put("status", "HEALTHY");
                health.put("message", "Email system is fully operational");
                health.put("smtpConfigured", true);
                health.put("asyncEnabled", true);
                health.put("retryEnabled", true);
                health.put("details", Map.of(
                    "smtpServer", "smtp.office365.com:587",
                    "protocol", "TLS 1.2",
                    "asyncThreadPool", "5-20 threads",
                    "retryPolicy", "3 attempts with exponential backoff",
                    "lastTested", LocalDateTime.now()
                ));
                return ResponseEntity.ok(health);
            } else {
                health.put("status", "UNHEALTHY");
                health.put("message", "SMTP connection test failed");
                health.put("smtpConfigured", false);
                health.put("asyncEnabled", true);
                health.put("retryEnabled", true);
                health.put("details", Map.of(
                    "issue", "Cannot connect to SMTP server",
                    "smtpServer", "smtp.office365.com:587",
                    "recommendations", new String[]{
                        "1. Verify SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD environment variables",
                        "2. Check network connectivity to smtp.office365.com",
                        "3. Verify Office365 credentials are correct",
                        "4. Check if Office365 app password (not account password) is being used",
                        "5. Check backend logs for detailed error messages"
                    }
                ));
                return ResponseEntity.status(503).body(health);
            }
        } catch (Exception e) {
            health.put("status", "ERROR");
            health.put("message", "Error checking email system health: " + e.getMessage());
            health.put("smtpConfigured", false);
            health.put("details", Map.of(
                "error", e.getMessage(),
                "errorType", e.getClass().getSimpleName(),
                "recommendation", "Check backend logs for detailed error information"
            ));
            return ResponseEntity.status(500).body(health);
        }
    }

    /**
     * Quick status check (lighter weight than full health check)
     * GET /api/email/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getEmailStatus() {
        Map<String, String> status = new HashMap<>();
        
        if (emailNotificationService == null) {
            status.put("status", "UNAVAILABLE");
            status.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.status(503).body(status);
        }

        try {
            boolean connected = emailNotificationService.testSmtpConnection();
            status.put("status", connected ? "HEALTHY" : "UNHEALTHY");
            status.put("timestamp", LocalDateTime.now().toString());
            return connected ? ResponseEntity.ok(status) : ResponseEntity.status(503).body(status);
        } catch (Exception e) {
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
            status.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.status(500).body(status);
        }
    }

    /**
     * Get email configuration summary (safe to expose - no sensitive data)
     * GET /api/email/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getEmailConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("smtpServer", "smtp.office365.com");
        config.put("smtpPort", 587);
        config.put("protocol", "TLS 1.2");
        config.put("authentication", "Enabled");
        config.put("asyncProcessing", true);
        config.put("asyncThreadPoolSize", "5-20 threads");
        config.put("queueCapacity", 100);
        config.put("retryEnabled", true);
        config.put("maxRetryAttempts", 3);
        config.put("retryBackoffStrategy", "Exponential (2s base, 10s max)");
        config.put("fromAddress", "no_reply@minet.co.ke");
        config.put("fromName", "Minet SACCO");
        
        config.put("supportedNotificationTypes", new String[]{
            "LOAN_APPROVAL",
            "LOAN_REJECTED",
            "LOAN_DISBURSED",
            "DEPOSIT_CONFIRMATION",
            "PASSWORD_RESET",
            "REPAYMENT_REMINDER",
            "SECURITY_ALERT",
            "SYSTEM_ANNOUNCEMENT"
        });
        
        config.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(config);
    }

    /**
     * Detailed diagnostics for troubleshooting
     * GET /api/email/diagnostics
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<Map<String, Object>> getEmailDiagnostics() {
        Map<String, Object> diagnostics = new HashMap<>();
        
        diagnostics.put("timestamp", LocalDateTime.now());
        diagnostics.put("javaVersion", System.getProperty("java.version"));
        diagnostics.put("osName", System.getProperty("os.name"));
        diagnostics.put("osVersion", System.getProperty("os.version"));
        
        // Environment variables (redacted for security)
        Map<String, String> envVars = new HashMap<>();
        String mailUsername = System.getenv("SPRING_MAIL_USERNAME");
        String mailPassword = System.getenv("SPRING_MAIL_PASSWORD");
        
        envVars.put("SPRING_MAIL_USERNAME", mailUsername != null ? "✓ SET" : "✗ NOT SET");
        envVars.put("SPRING_MAIL_PASSWORD", mailPassword != null ? "✓ SET" : "✗ NOT SET");
        diagnostics.put("environmentVariables", envVars);
        
        // Service status
        Map<String, String> services = new HashMap<>();
        services.put("EmailNotificationService", emailNotificationService != null ? "✓ Available" : "✗ Not Available");
        
        try {
            boolean smtpOk = emailNotificationService != null && emailNotificationService.testSmtpConnection();
            services.put("SMTP Connection", smtpOk ? "✓ Connected" : "✗ Failed");
        } catch (Exception e) {
            services.put("SMTP Connection", "✗ Error: " + e.getMessage());
        }
        diagnostics.put("services", services);
        
        // Configuration paths
        Map<String, String> configs = new HashMap<>();
        configs.put("activeProfiles", "prod or dev");
        configs.put("applicationPropertiesPath", "classpath:application.properties");
        configs.put("profilePropertiesPath", "classpath:application-{profile}.properties");
        diagnostics.put("configurations", configs);
        
        // Troubleshooting tips
        Map<String, String> troubleshooting = new HashMap<>();
        troubleshooting.put("noEmailsBeingSent", 
            "1. Check SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD env vars\n" +
            "2. Verify @EnableAsync is active (check AsyncConfiguration bean)\n" +
            "3. Check application logs for email errors\n" +
            "4. Run /api/email/health to diagnose");
        troubleshooting.put("smtpConnectionFailed",
            "1. Verify Office365 credentials\n" +
            "2. Ensure app password is used (not account password)\n" +
            "3. Check network can reach smtp.office365.com:587\n" +
            "4. Verify TLS 1.2 is supported");
        diagnostics.put("troubleshooting", troubleshooting);
        
        return ResponseEntity.ok(diagnostics);
    }
}
