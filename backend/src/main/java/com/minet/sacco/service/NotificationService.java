package com.minet.sacco.service;

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
        }
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
        if (notification.isPresent()) {
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
        notificationRepository.deleteById(notificationId);
    }
}
