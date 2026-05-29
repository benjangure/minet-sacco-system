package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Email Service for sending notifications to members and staff.
 * Currently configured as a stub - actual SMTP configuration needed in application.properties
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private static final String SENDER_EMAIL = "noreply@minetsacco.com";
    private static final String SENDER_NAME = "Minet SACCO";

    /**
     * Send member onboarding email with login credentials and APK download link
     * Called when member is approved and activated
     */
    @Async
    public void sendMemberOnboardingEmail(Member member, String apkDownloadLink) {
        if (mailSender == null) {
            System.out.println("INFO: Email service not configured. Skipping onboarding email for member: " + member.getId());
            return;
        }

        try {
            String username = member.getEmployeeId() != null ? 
                member.getEmployeeId() : member.getMemberNumber();

            String htmlContent = buildOnboardingEmailHtml(
                member.getFirstName(),
                member.getLastName(),
                username,
                member.getNationalId(),
                member.getMemberNumber(),
                apkDownloadLink
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER_EMAIL);
            message.setTo(member.getEmail());
            message.setSubject("Welcome to Minet SACCO - Your Login Credentials");
            message.setText(htmlContent);

            mailSender.send(message);
            
            System.out.println("INFO: Onboarding email sent successfully to: " + member.getEmail());

        } catch (Exception e) {
            System.err.println("ERROR: Failed to send onboarding email to member " + 
                             member.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Send password change confirmation email
     */
    @Async
    public void sendPasswordChangeConfirmation(User user) {
        if (mailSender == null) {
            System.out.println("INFO: Email service not configured. Skipping password change confirmation for: " + user.getUsername());
            return;
        }

        try {
            String htmlContent = buildPasswordChangeEmailHtml(
                user.getUsername(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER_EMAIL);
            message.setTo(user.getEmail());
            message.setSubject("Password Changed - Minet SACCO");
            message.setText(htmlContent);

            mailSender.send(message);

            System.out.println("INFO: Password change confirmation email sent to: " + user.getEmail());

        } catch (Exception e) {
            System.err.println("ERROR: Failed to send password change confirmation to user " + 
                             user.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Send password reset notification (when support resets member password)
     */
    @Async
    public void sendPasswordResetNotification(User user, String temporaryPassword) {
        if (mailSender == null) {
            System.out.println("INFO: Email service not configured. Skipping password reset notification for: " + user.getUsername());
            return;
        }

        try {
            String htmlContent = buildPasswordResetEmailHtml(
                user.getUsername(),
                temporaryPassword
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER_EMAIL);
            message.setTo(user.getEmail());
            message.setSubject("Password Reset - Minet SACCO");
            message.setText(htmlContent);

            mailSender.send(message);

            System.out.println("INFO: Password reset notification sent to: " + user.getEmail());

        } catch (Exception e) {
            System.err.println("ERROR: Failed to send password reset notification to user " + 
                             user.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Build HTML content for member onboarding email
     */
    private String buildOnboardingEmailHtml(String firstName, String lastName, 
                                           String username, String password, 
                                           String memberNumber, String apkLink) {
        return String.format(
            "<html>" +
            "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">" +
            "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">" +
            "<h2 style=\"color: #2c3e50;\">Welcome to Minet SACCO, %s %s!</h2>" +
            "<p>Your account has been successfully approved and activated. You can now access the Minet SACCO mobile app.</p>" +
            "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 20px 0;\">" +
            "<h3 style=\"color: #34495e;\">📱 Mobile App Login Credentials:</h3>" +
            "<div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #3498db;\">" +
            "<p><strong>Username:</strong> %s<br/>" +
            "<strong>Password:</strong> %s (Your National ID)<br/>" +
            "<strong>Member Number:</strong> %s</p>" +
            "</div>" +
            "<p style=\"color: #e74c3c;\"><strong>⚠️ Important:</strong> Your password is your National ID for the first login. You <strong>MUST</strong> change it to a secure password after your first login.</p>" +
            "<h3 style=\"color: #34495e;\">📲 Next Steps:</h3>" +
            "<ol>" +
            "<li><a href=\"%s\" style=\"color: #3498db; text-decoration: none;\"><strong>Download the Minet SACCO Mobile App</strong></a></li>" +
            "<li>Launch the app and log in with your credentials above</li>" +
            "<li>Go to Settings → Security → Change Password</li>" +
            "<li>Enter your National ID as the current password</li>" +
            "<li>Create a new strong password and confirm it</li>" +
            "<li>Your new password will take effect immediately</li>" +
            "</ol>" +
            "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 20px 0;\">" +
            "<p style=\"color: #7f8c8d;\">If you have any questions or need assistance, please contact our Customer Support team.</p>" +
            "<p style=\"font-size: 12px; color: #95a5a6;\">This is an automated email. Please do not reply to this message.</p>" +
            "</div>" +
            "</body>" +
            "</html>",
            firstName, lastName, username, password, memberNumber, apkLink
        );
    }

    /**
     * Build HTML content for password change confirmation email
     */
    private String buildPasswordChangeEmailHtml(String username, String timestamp) {
        return String.format(
            "<html>" +
            "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">" +
            "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">" +
            "<h2 style=\"color: #27ae60;\">✓ Password Changed Successfully</h2>" +
            "<p>Your password for account <strong>%s</strong> has been changed.</p>" +
            "<div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #27ae60;\">" +
            "<p><strong>Account:</strong> %s<br/>" +
            "<strong>Changed at:</strong> %s</p>" +
            "</div>" +
            "<p style=\"color: #e74c3c;\"><strong>If you did not make this change,</strong> please contact Customer Support immediately.</p>" +
            "<p style=\"color: #7f8c8d;\">For security reasons, please remember to:</p>" +
            "<ul>" +
            "<li>Keep your password confidential</li>" +
            "<li>Use a strong password with uppercase, lowercase, numbers, and special characters</li>" +
            "<li>Don't share your credentials with anyone</li>" +
            "<li>Log out when you're done using the app</li>" +
            "</ul>" +
            "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 20px 0;\">" +
            "<p style=\"font-size: 12px; color: #95a5a6;\">This is an automated email. Please do not reply to this message.</p>" +
            "</div>" +
            "</body>" +
            "</html>",
            username, username, timestamp
        );
    }

    /**
     * Build HTML content for password reset notification
     */
    private String buildPasswordResetEmailHtml(String username, String temporaryPassword) {
        return String.format(
            "<html>" +
            "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">" +
            "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">" +
            "<h2 style=\"color: #f39c12;\">🔐 Your Password Has Been Reset</h2>" +
            "<p>Our Customer Support team has reset your password as requested.</p>" +
            "<div style=\"background-color: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #f39c12;\">" +
            "<p><strong>Account:</strong> %s<br/>" +
            "<strong>Temporary Password:</strong> %s</p>" +
            "</div>" +
            "<h3 style=\"color: #34495e;\">📱 What to do next:</h3>" +
            "<ol>" +
            "<li>Log in with your username: <strong>%s</strong></li>" +
            "<li>Use the temporary password: <strong>%s</strong></li>" +
            "<li>Go to Settings → Security → Change Password</li>" +
            "<li>Create a new strong password</li>" +
            "<li>Your new password will take effect immediately</li>" +
            "</ol>" +
            "<p style=\"color: #e74c3c;\"><strong>⚠️ Important:</strong> The temporary password will work for 24 hours. Please change it to a new password as soon as possible.</p>" +
            "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 20px 0;\">" +
            "<p style=\"font-size: 12px; color: #95a5a6;\">This is an automated email. Please do not reply to this message.</p>" +
            "</div>" +
            "</body>" +
            "</html>",
            username, temporaryPassword, username, temporaryPassword
        );
    }
}
