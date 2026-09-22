package com.minet.sacco.service;

import com.minet.sacco.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${app.sacco.name:Minet SACCO}")
    private String saccoName;
    
    @Value("${app.sacco.support.email:support@minetsacco.com}")
    private String supportEmail;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    /**
     * Send welcome email with login credentials to new member
     */
    public boolean sendWelcomeEmail(String memberEmail, String memberName, String username, String temporaryPassword, boolean hasNationalId) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Cannot send welcome email to {}", memberEmail);
            return false;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(memberEmail);
            message.setSubject("Welcome to " + saccoName + " - Your Login Credentials");
            
            String emailBody = buildWelcomeEmailBody(memberName, username, temporaryPassword, hasNationalId);
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Welcome email sent successfully to {} ({})", memberName, memberEmail);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {} ({}): {}", memberName, memberEmail, e.getMessage());
            return false;
        }
    }
    
    /**
     * Send password reset email (deprecated - use sendPasswordResetEmail with link)
     */
    public boolean sendPasswordResetEmail(String memberEmail, String memberName, String temporaryPassword) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Cannot send password reset email to {}", memberEmail);
            return false;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(memberEmail);
            message.setSubject(saccoName + " - Password Reset");
            
            String emailBody = buildPasswordResetEmailBody(memberName, temporaryPassword);
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Password reset email sent successfully to {} ({})", memberName, memberEmail);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {} ({}): {}", memberName, memberEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Send password reset email with reset link (for staff)
     */
    public boolean sendPasswordResetEmail(String email, String username, String resetLink, boolean isStaff) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Cannot send password reset email to {}", email);
            return false;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject(saccoName + " - Password Reset Request");
            
            String emailBody = buildPasswordResetLinkEmailBody(username, resetLink, isStaff);
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Password reset email with link sent successfully to {} ({})", username, email);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {} ({}): {}", username, email, e.getMessage());
            return false;
        }
    }

    /**
     * Send password reset email to member with reset link
     */
    public boolean sendMemberPasswordResetEmail(String email, String username, String resetLink) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Cannot send member password reset email to {}", email);
            return false;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject(saccoName + " - Password Reset Request");
            
            String emailBody = buildMemberPasswordResetLinkEmailBody(username, resetLink);
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Member password reset email sent successfully to {} ({})", username, email);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send member password reset email to {} ({}): {}", username, email, e.getMessage());
            return false;
        }
    }
    
    private String buildWelcomeEmailBody(String memberName, String username, String temporaryPassword, boolean hasNationalId) {
        StringBuilder body = new StringBuilder();
        
        body.append("Dear ").append(memberName).append(",\n\n");
        body.append("Welcome to ").append(saccoName).append("!\n\n");
        body.append("Your member account has been successfully created. Below are your login credentials:\n\n");
        
        body.append("Mobile App Login Details:\n");
        body.append("------------------------\n");
        body.append("Username: ").append(username).append("\n");
        
        if (hasNationalId) {
            body.append("Initial Password: Your National ID\n");
            body.append("\nFor security, please use your National ID as the initial password.\n");
        } else {
            body.append("Temporary Password: ").append(temporaryPassword).append("\n");
            body.append("\nThis is a temporary password generated for your security.\n");
        }
        
        body.append("\nIMPORTANT NEXT STEPS:\n");
        body.append("1. Download the ").append(saccoName).append(" mobile app\n");
        body.append("2. Login using the credentials above\n");
        body.append("3. You will be prompted to create a new secure password\n");
        body.append("4. Complete your profile and start using SACCO services\n\n");
        
        body.append("SECURITY REMINDER:\n");
        body.append("- Keep your login credentials secure and confidential\n");
        body.append("- Never share your password with anyone\n");
        body.append("- Contact support if you suspect unauthorized access\n\n");
        
        body.append("Need Help?\n");
        body.append("If you have any questions or need assistance, please contact our support team at ").append(supportEmail).append("\n\n");
        
        body.append("Welcome aboard!\n\n");
        body.append("Best regards,\n");
        body.append(saccoName).append(" Team");
        
        return body.toString();
    }
    
    private String buildPasswordResetEmailBody(String memberName, String temporaryPassword) {
        StringBuilder body = new StringBuilder();
        
        body.append("Dear ").append(memberName).append(",\n\n");
        body.append("Your password has been reset as requested.\n\n");
        
        body.append("New Temporary Password: ").append(temporaryPassword).append("\n\n");
        
        body.append("NEXT STEPS:\n");
        body.append("1. Login using your username and the temporary password above\n");
        body.append("2. You will be prompted to create a new secure password\n");
        body.append("3. Choose a strong password that you can remember\n\n");
        
        body.append("If you did not request this password reset, please contact our support team immediately at ").append(supportEmail).append("\n\n");
        
        body.append("Best regards,\n");
        body.append(saccoName).append(" Team");
        
        return body.toString();
    }

    private String buildPasswordResetLinkEmailBody(String username, String resetLink, boolean isStaff) {
        StringBuilder body = new StringBuilder();
        
        body.append("Hello ").append(username).append(",\n\n");
        body.append("We received a request to reset your password for your ").append(saccoName).append(" account.\n\n");
        
        body.append("CLICK THE LINK BELOW TO RESET YOUR PASSWORD:\n");
        body.append(resetLink).append("\n\n");
        
        body.append("IMPORTANT SECURITY INFORMATION:\n");
        body.append("- This password reset link will expire in 30 minutes\n");
        body.append("- The link can only be used once\n");
        body.append("- If the link has expired, request a new password reset\n\n");
        
        body.append("If you did not request this password reset, please:\n");
        body.append("1. Ignore this email\n");
        body.append("2. Contact our support team immediately at ").append(supportEmail).append("\n\n");
        
        body.append("PASSWORD SECURITY REMINDER:\n");
        body.append("- Use a strong password (mix of uppercase, lowercase, numbers, and special characters)\n");
        body.append("- Never share your password with anyone\n");
        body.append("- Do not reuse passwords from other accounts\n\n");
        
        body.append("Best regards,\n");
        body.append(saccoName).append(" Team");
        
        return body.toString();
    }

    private String buildMemberPasswordResetLinkEmailBody(String username, String resetLink) {
        StringBuilder body = new StringBuilder();
        
        body.append("Hello ").append(username).append(",\n\n");
        body.append("We received a request to reset your password for your ").append(saccoName).append(" member account.\n\n");
        
        body.append("CLICK THE LINK BELOW TO RESET YOUR PASSWORD:\n");
        body.append(resetLink).append("\n\n");
        
        body.append("IMPORTANT SECURITY INFORMATION:\n");
        body.append("- This password reset link will expire in 30 minutes\n");
        body.append("- The link can only be used once\n");
        body.append("- If the link has expired, request a new password reset\n\n");
        
        body.append("If you did not request this password reset, please:\n");
        body.append("1. Do not click the link\n");
        body.append("2. Contact our support team immediately at ").append(supportEmail).append("\n\n");
        
        body.append("PASSWORD SECURITY REMINDER:\n");
        body.append("- Use a strong password (mix of uppercase, lowercase, numbers, and special characters)\n");
        body.append("- Never share your password with anyone\n");
        body.append("- Do not reuse passwords from other accounts\n\n");
        
        body.append("Best regards,\n");
        body.append(saccoName).append(" Team");
        
        return body.toString();
    }
    
    /**
     * Send password change confirmation email
     */
    public boolean sendPasswordChangeConfirmation(User user) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Cannot send password change confirmation to user: {}", user.getUsername());
            return false;
        }
        
        try {
            // Get member details if available
            String memberName = user.getUsername();
            String memberEmail = null;
            
            if (user.getMemberId() != null) {
                // This would typically get member details, but for now we'll use what we have
                memberName = "Member " + user.getMemberId();
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            
            if (memberEmail != null) {
                message.setTo(memberEmail);
            } else {
                // If no email available, log it and return false
                logger.warn("No email address available for user: {}", user.getUsername());
                return false;
            }
            
            message.setSubject(saccoName + " - Password Changed Successfully");
            
            String emailBody = buildPasswordChangeConfirmationBody(memberName);
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Password change confirmation email sent successfully to user: {}", user.getUsername());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send password change confirmation to user {}: {}", user.getUsername(), e.getMessage());
            return false;
        }
    }
    
    private String buildPasswordChangeConfirmationBody(String memberName) {
        StringBuilder body = new StringBuilder();
        
        body.append("Dear ").append(memberName).append(",\n\n");
        body.append("This is to confirm that your password has been changed successfully.\n\n");
        
        body.append("SECURITY DETAILS:\n");
        body.append("- Date: ").append(new java.util.Date()).append("\n");
        body.append("- Action: Password Update\n");
        body.append("- Status: Successful\n\n");
        
        body.append("If you did not make this change, please contact our support team immediately at ").append(supportEmail).append("\n\n");
        
        body.append("For your security:\n");
        body.append("- Keep your new password secure and confidential\n");
        body.append("- Never share your password with anyone\n");
        body.append("- Use a strong, unique password\n\n");
        
        body.append("Best regards,\n");
        body.append(saccoName).append(" Team");
        
        return body.toString();
    }
    
    /**
     * Check if email service is properly configured
     */
    public boolean isEmailConfigured() {
        return emailEnabled && mailSender != null;
    }
}