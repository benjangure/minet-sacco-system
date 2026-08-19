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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

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
            
            // Send email notification if email service is available and user has email
            if (emailNotificationService != null && user.get().getEmail() != null && !user.get().getEmail().isEmpty()) {
                try {
                    String subject = getEmailSubject(type);
                    emailNotificationService.sendNotificationEmail(
                        user.get().getEmail(),
                        subject,
                        message,
                        type
                    );
                } catch (Exception e) {
                    System.err.println("Failed to send email notification: " + e.getMessage());
                }
            }

            // Send push notification
            sendPushNotification(user.get(), message, type, null);
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
            
            // Send email notification
            if (emailNotificationService != null && user.get().getEmail() != null && !user.get().getEmail().isEmpty()) {
                try {
                    String subject = getEmailSubject(type, category);
                    emailNotificationService.sendNotificationEmail(
                        user.get().getEmail(),
                        subject,
                        message,
                        category != null ? category : type
                    );
                } catch (Exception e) {
                    System.err.println("Failed to send email notification: " + e.getMessage());
                }
            }

            // Send push notification
            sendPushNotification(user.get(), message, category != null ? category : type, null);
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
        for (User user : users) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage(message);
            notification.setType(type);
            notification.setTargetRole(role);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyUsersByRole(String role, String message, String type, Long loanId, Long memberId, String category) {
        List<User> users = userRepository.findByRole(User.Role.valueOf(role));
        for (User user : users) {
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
        }
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
