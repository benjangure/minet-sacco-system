package com.minet.sacco.service;

import com.minet.sacco.dto.PushNotificationDTO;
import com.minet.sacco.entity.Notification;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.NotificationRepository;
import com.minet.sacco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId")
    public void notifyUser(Long userId, String message, String type) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            Notification notification = new Notification();
            notification.setUser(user.get());
            notification.setMessage(message);
            notification.setType(type);
            notification.setTargetRole(user.get().getRole().toString());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            logger.debug("📬 Saved notification to database for user: {} (Type: {})", user.get().getUsername(), type);
            
            // Send email notification
            sendEmailNotification(user.get(), message, type, null, null);

            // Send push notification
            sendPushNotification(user.get(), message, type, null);
        } else {
            logger.warn("⚠️  User not found for notification: userId={}, type={}", userId, type);
        }
    }

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId")
    public void notifyUser(Long userId, String message, String type, Long loanId, Long memberId, String category) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            Notification notification = new Notification();
            notification.setUser(user.get());
            notification.setMessage(message);
            notification.setType(type);
            notification.setTargetRole(user.get().getRole().toString());
            notification.setLoanId(loanId);
            notification.setMemberId(memberId);
            notification.setCategory(category);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            logger.debug("📬 Saved notification to database for user: {} (Type: {}, Category: {})", 
                user.get().getUsername(), type, category);
            
            // Send email notification
            sendEmailNotification(user.get(), message, type, category, loanId);

            // Send push notification
            sendPushNotification(user.get(), message, category != null ? category : type, null);
        } else {
            logger.warn("⚠️  User not found for notification: userId={}, type={}, category={}", userId, type, category);
        }
    }
    
    /**
     * Helper method to send email notification with proper error handling and logging
     */
    private void sendEmailNotification(User user, String message, String type, String category, Long loanId) {
        if (emailNotificationService == null) {
            logger.debug("⚠️  Email service not available, skipping email for type: {}", type);
            return;
        }
        
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            logger.warn("⚠️  User {} has no email configured, cannot send notification email", user.getUsername());
            return;
        }
        
        try {
            String subject = getEmailSubject(type, category);
            String notificationType = category != null ? category : type;
            
            logger.info("📧 Sending email notification to: {} (Subject: {})", user.getEmail(), subject);
            
            emailNotificationService.sendNotificationEmail(
                user.getEmail(),
                subject,
                message,
                notificationType
            );
            
            logger.info("✅ Email sent successfully to: {} for user: {}", user.getEmail(), user.getUsername());
        } catch (Exception e) {
            logger.error("❌ Failed to send email notification to {} ({}): {}", 
                user.getUsername(), user.getEmail(), e.getMessage());
        }
    }

    /**
     * Send a Web Push notification to a user if they have an active subscription.
     * Non-critical: failures are logged but never bubble up.
     */
    private void sendPushNotification(User user, String message, String type, String url) {
        if (pushNotificationService == null || !pushNotificationService.isConfigured()) {
            return;
        }
        try {
            if (!pushNotificationService.hasActiveSubscription(user)) {
                return;
            }
            String title = getPushTitle(type);
            String resolvedUrl = url != null ? url : "/member/dashboard?tab=notifications";
            PushNotificationDTO pushDTO = new PushNotificationDTO.Builder(title, message)
                    .type(type)
                    .url(resolvedUrl)
                    .icon("/icon-512.png")
                    .badge("/icon-192.png")
                    .tag(type != null ? type.toLowerCase() : "notification")
                    .build();
            pushNotificationService.sendNotificationToUser(user, pushDTO);
        } catch (Exception e) {
            System.err.println("Failed to send push notification to user " + user.getId() + ": " + e.getMessage());
        }
    }

    private String getPushTitle(String type) {
        if (type == null) return "Minet SACCO";
        switch (type.toUpperCase()) {
            case "LOAN":
            case "LOAN_APPROVAL":
            case "LOAN_APPROVED":
            case "LOAN_REJECTED":
            case "LOAN_DISBURSED":       return "Loan Update";
            case "GUARANTOR":
            case "GUARANTOR_REQUEST":    return "Guarantor Request";
            case "DEPOSIT":
            case "DEPOSIT_REQUEST":
            case "DEPOSIT_APPROVED":     return "Deposit Update";
            case "REPAYMENT":            return "Repayment Recorded";
            case "BULK_PROCESSING":      return "Bulk Processing Update";
            case "SECURITY_ALERT":
            case "NEW_DEVICE_LOGIN":     return "Security Alert";
            default:                     return "Minet SACCO";
        }
    }

    /**
     * Generate appropriate email subject based on notification type
     */
    private String getEmailSubject(String type) {
        return getEmailSubject(type, null);
    }

    private String getEmailSubject(String type, String category) {
        if (category != null && !category.isEmpty()) {
            switch (category.toUpperCase()) {
                case "LOAN_APPROVAL": return "Loan Application Update - Minet SACCO";
                case "LOAN": return "Loan Notification - Minet SACCO";
                case "GUARANTOR": return "Guarantor Request - Minet SACCO";
                case "DEPOSIT": return "Deposit Notification - Minet SACCO";
                case "REPAYMENT": return "Repayment Notification - Minet SACCO";
                case "DEPOSIT_REQUEST": return "Deposit Request - Minet SACCO";
                case "DEPOSIT_APPROVED": return "Deposit Approved - Minet SACCO";
                case "BULK_PROCESSING": return "Bulk Processing Update - Minet SACCO";
            }
        }
        
        if (type != null && !type.isEmpty()) {
            if (type.toUpperCase().contains("LOAN")) return "Loan Notification - Minet SACCO";
            if (type.toUpperCase().contains("DEPOSIT")) return "Deposit Notification - Minet SACCO";
            if (type.toUpperCase().contains("GUARANTOR")) return "Guarantor Request - Minet SACCO";
            if (type.toUpperCase().contains("APPROVAL")) return "Approval Notification - Minet SACCO";
        }
        
        return "Notification from Minet SACCO";
    }

    @Transactional
    public void notifyUsers(List<Long> userIds, String message, String type) {
        for (Long userId : userIds) {
            notifyUser(userId, message, type);
        }
    }

    @Transactional
    public void notifyUsers(List<Long> userIds, String message, String type, Long loanId, Long memberId, String category) {
        for (Long userId : userIds) {
            notifyUser(userId, message, type, loanId, memberId, category);
        }
    }

    @Transactional
    public void notifyUsersByRole(String role, String message, String type) {
        List<User> users = userRepository.findByRole(User.Role.valueOf(role));
        logger.info("📢 Sending {} notifications to {} users with role: {}", type, users.size(), role);
        
        for (User user : users) {
            // Save notification to database
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage(message);
            notification.setType(type);
            notification.setTargetRole(role);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            // Send email notification
            sendEmailNotification(user, message, type, null, null);
            
            // Send push notification
            sendPushNotification(user, message, type, null);
        }
        
        logger.info("✅ Completed {} notifications to {} users", type, users.size());
    }

    @Transactional
    public void notifyUsersByRole(String role, String message, String type, Long loanId, Long memberId, String category) {
        List<User> users = userRepository.findByRole(User.Role.valueOf(role));
        logger.info("📢 Sending {} notifications to {} users with role: {} (Category: {})", 
            type, users.size(), role, category);
        
        for (User user : users) {
            // Save notification to database
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage(message);
            notification.setType(type);
            notification.setTargetRole(role);
            notification.setLoanId(loanId);
            notification.setMemberId(memberId);
            notification.setCategory(category);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            // Send email notification
            sendEmailNotification(user, message, type, category, loanId);
            
            // Send push notification
            sendPushNotification(user, message, category != null ? category : type, null);
        }
        
        logger.info("✅ Completed {} notifications to {} users", type, users.size());
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Cacheable(value = "unreadCount", key = "#userId")
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId", allEntries = false)
    public void markAsRead(Long notificationId, Long userId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent() && notification.get().getUser() != null &&
            notification.get().getUser().getId().equals(userId)) {
            notification.get().setRead(true);
            notificationRepository.save(notification.get());
        }
    }

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId")
    public void markAllAsRead(Long userId) {
        List<Notification> unread = getUnreadNotifications(userId);
        for (Notification notification : unread) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    @CacheEvict(value = "unreadCount", key = "#userId", allEntries = false)
    public void deleteNotification(Long notificationId, Long userId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent() && notification.get().getUser() != null &&
            notification.get().getUser().getId().equals(userId)) {
            notificationRepository.deleteById(notificationId);
        }
    }
}
