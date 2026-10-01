package com.minet.sacco.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * MailConfiguration sets up the email sender bean with proper SMTP configuration.
 * This configuration validates SMTP credentials on application startup and provides
 * detailed logging for troubleshooting email delivery issues.
 * 
 * Configuration is loaded from:
 * - Environment variables: SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD (highest priority)
 * - application.properties: spring.mail.* properties
 */
@Configuration
public class MailConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(MailConfiguration.class);
    
    @Value("${spring.mail.host:smtp.office365.com}")
    private String mailHost;
    
    @Value("${spring.mail.port:587}")
    private int mailPort;
    
    @Value("${spring.mail.username:}")
    private String mailUsername;
    
    @Value("${spring.mail.password:}")
    private String mailPassword;
    
    @Value("${spring.mail.from:no_reply@minet.co.ke}")
    private String mailFrom;
    
    @Value("${spring.mail.enabled:true}")
    private boolean mailEnabled;
    
    /**
     * Create and configure JavaMailSender bean with SMTP settings.
     * Validates connection on startup and logs configuration details.
     */
    @Bean
    public JavaMailSender javaMailSender() {
        if (!mailEnabled) {
            logger.warn("⚠️  Email is DISABLED. No emails will be sent. " +
                "Set spring.mail.enabled=true to enable email functionality.");
            return null;
        }
        
        // Validate required configuration
        if (mailUsername == null || mailUsername.isEmpty()) {
            logger.error("❌ Email configuration error: spring.mail.username is missing. " +
                "Set SPRING_MAIL_USERNAME environment variable or spring.mail.username in properties.");
            return null;
        }
        
        if (mailPassword == null || mailPassword.isEmpty()) {
            logger.error("❌ Email configuration error: spring.mail.password is missing. " +
                "Set SPRING_MAIL_PASSWORD environment variable or spring.mail.password in properties.");
            return null;
        }
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        mailSender.setDefaultEncoding("UTF-8");
        
        // Configure SMTP properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");                          // Enable SMTP authentication
        props.put("mail.smtp.starttls.enable", "true");               // Enable STARTTLS
        props.put("mail.smtp.starttls.required", "true");             // Require STARTTLS
        props.put("mail.smtp.starttls.protocols", "TLSv1.2");         // Use TLS 1.2 (secure)
        props.put("mail.smtp.connectiontimeout", "5000");             // 5 second connection timeout
        props.put("mail.smtp.timeout", "5000");                       // 5 second read timeout
        props.put("mail.smtp.writetimeout", "5000");                  // 5 second write timeout
        props.put("mail.smtp.quitwait", "false");                     // Don't wait for QUIT response
        props.put("mail.smtp.socketFactory.fallback", "true");        // Fallback on socket factory error
        props.put("mail.smtp.socketFactory.port", mailPort);          // Use same port for socket factory
        props.put("mail.smtp.writetimeout", "5000");
        
        // Test SMTP connection on startup
        try {
            logger.info("🔍 Testing SMTP connection to {}:{} as {}...", mailHost, mailPort, mailUsername);
            mailSender.testConnection();
            logger.info("✅ SMTP connection successful! Email service is ready.");
            logger.info("   Host: {} | Port: {} | From: {} | Authentication: Enabled",
                mailHost, mailPort, mailFrom);
        } catch (Exception e) {
            logger.error("❌ SMTP connection test FAILED: {}. Emails will NOT be sent. " +
                "Check your email configuration and network connectivity.", e.getMessage());
            logger.error("   Configuration: {}:{} | Username: {} | Protocol: TLSv1.2",
                mailHost, mailPort, mailUsername);
            
            // Log detailed error information
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Invalid Addresses")) {
                    logger.error("   Issue: Invalid email format in spring.mail.from or spring.mail.username");
                } else if (e.getMessage().contains("Could not connect")) {
                    logger.error("   Issue: Cannot reach SMTP server. Check host/port and network connectivity.");
                } else if (e.getMessage().contains("Authentication failed")) {
                    logger.error("   Issue: Username/password incorrect. Verify SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD");
                }
            }
        }
        
        return mailSender;
    }
}
