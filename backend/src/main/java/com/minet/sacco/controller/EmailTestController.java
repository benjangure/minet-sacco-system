package com.minet.sacco.controller;

import com.minet.sacco.service.EmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailTestController {

    private final EmailNotificationService emailService;

    public EmailTestController(EmailNotificationService emailService) {
        this.emailService = emailService;
    }

    /**
     * Send a test email to verify configuration
     * POST /api/email/test
     * Body: { "email": "victorgathecha@gmail.com" }
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email address is required"));
            }
            
            // Send test email
            emailService.sendTestEmail(email);
            
            return ResponseEntity.ok(Map.of(
                "message", "Test email sent successfully!",
                "email", email,
                "status", "Email is being sent in the background. Check your inbox in a few moments."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to send email: " + e.getMessage()));
        }
    }

    /**
     * Send a custom notification email
     * POST /api/email/notify
     * Body: { "email": "user@example.com", "subject": "...", "message": "...", "type": "LOAN_APPROVAL" }
     */
    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> sendNotificationEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String subject = request.get("subject");
            String message = request.get("message");
            String type = request.getOrDefault("type", "INFO");
            
            if (email == null || subject == null || message == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email, subject, and message are required"));
            }
            
            emailService.sendNotificationEmail(email, subject, message, type);
            
            return ResponseEntity.ok(Map.of(
                "message", "Notification email sent successfully!",
                "email", email
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to send notification: " + e.getMessage()));
        }
    }

    /**
     * Test SMTP connection
     * GET /api/email/test-connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            boolean isConnected = emailService.testSmtpConnection();
            if (isConnected) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "SMTP connection successful!",
                    "host", "smtp.office365.com",
                    "port", 587
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "SMTP connection failed",
                    "host", "smtp.office365.com",
                    "port", 587
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error testing connection: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }
}
