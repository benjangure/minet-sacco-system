package com.minet.sacco.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.from:no_reply@minet.co.ke}")
    private String fromEmail;
    
    @Value("${spring.mail.from-name:Minet SACCO}")
    private String fromName;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send notification email asynchronously
     */
    @Async
    public void sendNotificationEmail(String toEmail, String subject, String notificationMessage, String notificationType) {
        try {
            String htmlContent = buildEmailTemplate(subject, notificationMessage, notificationType);
            sendHtmlEmail(toEmail, subject, htmlContent);
            System.out.println("Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }

    /**
     * Build beautiful red-themed email template
     */
    private String buildEmailTemplate(String title, String message, String type) {
        String iconHtml = getNotificationIcon(type);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        int currentYear = java.time.Year.now().getValue();
        
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>""" + title + """
</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background-color: #f5f5f5;
        }
        .email-container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
        }
        .header {
            background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
            padding: 40px 30px;
            text-align: center;
        }
        .logo {
            color: #ffffff;
            font-size: 28px;
            font-weight: bold;
            margin: 0;
            letter-spacing: 1px;
        }
        .tagline {
            color: #fecaca;
            font-size: 14px;
            margin: 8px 0 0 0;
        }
        .content {
            padding: 40px 30px;
        }
        .icon-container {
            text-align: center;
            margin-bottom: 24px;
        }
        .notification-icon {
            display: inline-block;
            width: 64px;
            height: 64px;
            background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
            border-radius: 50%;
            padding: 16px;
            box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
        }
        .title {
            color: #1f2937;
            font-size: 24px;
            font-weight: 600;
            margin: 0 0 16px 0;
            text-align: center;
        }
        .message {
            color: #4b5563;
            font-size: 16px;
            line-height: 1.6;
            margin: 0 0 24px 0;
            padding: 20px;
            background-color: #fef2f2;
            border-left: 4px solid #dc2626;
            border-radius: 4px;
        }
        .button-container {
            text-align: center;
            margin: 32px 0;
        }
        .cta-button {
            display: inline-block;
            padding: 14px 32px;
            background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
            color: #ffffff;
            text-decoration: none;
            border-radius: 6px;
            font-weight: 600;
            font-size: 16px;
            box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
            transition: transform 0.2s;
        }
        .cta-button:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(220, 38, 38, 0.4);
        }
        .info-box {
            background-color: #f9fafb;
            border-radius: 8px;
            padding: 20px;
            margin: 24px 0;
        }
        .info-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid #e5e7eb;
        }
        .info-row:last-child {
            border-bottom: none;
        }
        .info-label {
            color: #6b7280;
            font-size: 14px;
        }
        .info-value {
            color: #1f2937;
            font-weight: 600;
            font-size: 14px;
        }
        .footer {
            background-color: #1f2937;
            color: #9ca3af;
            padding: 30px;
            text-align: center;
            font-size: 14px;
        }
        .footer-links {
            margin: 16px 0;
        }
        .footer-link {
            color: #dc2626;
            text-decoration: none;
            margin: 0 12px;
        }
        .divider {
            height: 1px;
            background: linear-gradient(to right, transparent, #e5e7eb, transparent);
            margin: 24px 0;
        }
        .timestamp {
            color: #9ca3af;
            font-size: 12px;
            text-align: center;
            margin-top: 16px;
        }
        @media only screen and (max-width: 600px) {
            .content {
                padding: 24px 16px;
            }
            .header {
                padding: 24px 16px;
            }
            .title {
                font-size: 20px;
            }
            .message {
                font-size: 14px;
                padding: 16px;
            }
        }
    </style>
</head>
<body>
    <div class="email-container">
        <!-- Header -->
        <div class="header">
            <h1 class="logo">MINET SACCO</h1>
            <p class="tagline">Your Financial Partner</p>
        </div>

        <!-- Content -->
        <div class="content">
            <div class="icon-container">
                """ + iconHtml + """
            </div>

            <h2 class="title">""" + title + """
</h2>

            <div class="message">
                """ + message + """
            </div>

            <div class="button-container">
                <a href="http://localhost:3000" class="cta-button">
                    View in Dashboard
                </a>
            </div>

            <div class="divider"></div>

            <div class="info-box">
                <div class="info-row">
                    <span class="info-label">Notification Type</span>
                    <span class="info-value">""" + formatType(type) + """
</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Date & Time</span>
                    <span class="info-value">""" + timestamp + """
</span>
                </div>
            </div>

            <p class="timestamp">
                This is an automated notification from Minet SACCO Management System
            </p>
        </div>

        <!-- Footer -->
        <div class="footer">
            <p style="margin: 0 0 12px 0; font-weight: 600; color: #ffffff;">Minet SACCO</p>
            <p style="margin: 0 0 16px 0;">Your trusted financial cooperative</p>
            
            <div class="footer-links">
                <a href="http://localhost:3000" class="footer-link">Dashboard</a>
                <a href="mailto:support@minet.co.ke" class="footer-link">Support</a>
            </div>

            <p style="margin: 16px 0 0 0; font-size: 12px;">
                © %d Minet SACCO. All rights reserved.
            </p>
            <p style="margin: 8px 0 0 0; font-size: 11px; color: #6b7280;">
                You received this email because you are a member of Minet SACCO.
            </p>
        </div>
    </div>
</body>
</html>
""".formatted(currentYear);
    }

    private String getNotificationIcon(String type) {
        // SVG icons based on notification type
        String iconColor = "#ffffff";
        
        if (type == null) type = "INFO";
        type = type.toUpperCase();
        
        if (type.contains("LOAN")) {
            return """
                <div class="notification-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="WHITE" stroke-width="2">
                        <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                        <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                    </svg>
                </div>
                """;
        } else if (type.contains("DEPOSIT") || type.contains("SAVINGS")) {
            return """
                <div class="notification-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="WHITE" stroke-width="2">
                        <line x1="12" y1="1" x2="12" y2="23"></line>
                        <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                    </svg>
                </div>
                """;
        } else if (type.contains("APPROVAL") || type.contains("SUCCESS")) {
            return """
                <div class="notification-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="WHITE" stroke-width="2">
                        <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                </div>
                """;
        } else {
            return """
                <div class="notification-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="WHITE" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                    </svg>
                </div>
                """;
        }
    }

    private String formatType(String type) {
        if (type == null) return "General Notification";
        return type.replace("_", " ").toUpperCase();
    }

    /**
     * Send test email with beautiful template
     */
    @Async
    public void sendTestEmail(String toEmail) {
        try {
            String subject = "Welcome to Minet SACCO - Test Notification";
            String message = """
                <p>Hello!</p>
                <p>This is a <strong>test notification</strong> from the Minet SACCO Management System.</p>
                <p>Our email notification system is now active and ready to keep you updated with:</p>
                <ul style="color: #4b5563; line-height: 1.8;">
                    <li>✅ Loan application updates and approvals</li>
                    <li>✅ Deposit and withdrawal confirmations</li>
                    <li>✅ Payment reminders and receipts</li>
                    <li>✅ Account activity notifications</li>
                    <li>✅ Important system announcements</li>
                </ul>
                <p>If you received this email, it means our notification system is working perfectly!</p>
                """;
            
            String htmlContent = buildEmailTemplate(subject, message, "SYSTEM_TEST");
            sendHtmlEmail(toEmail, subject, htmlContent);
            System.out.println("✅ Test email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send test email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test SMTP connection
     */
    public boolean testSmtpConnection() {
        try {
            // Cast to JavaMailSenderImpl to access testConnection method
            if (mailSender instanceof JavaMailSenderImpl mailSenderImpl) {
                mailSenderImpl.testConnection();
                System.out.println("✅ SMTP connection test successful");
                return true;
            } else {
                System.err.println("❌ Cannot test connection - mailSender is not JavaMailSenderImpl");
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ SMTP connection test failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
