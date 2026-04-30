package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Notification;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.NotificationService;
import com.minet.sacco.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    private Long getUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> user = userService.getUserByUsername(username);
        return user.map(User::getId).orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        // Filter notifications based on user role - admin sees all, others see only relevant notifications
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            String userRole = auth.getAuthorities().iterator().next().getAuthority();
            // Remove ROLE_ prefix for comparison with targetRole
            String roleWithoutPrefix = userRole.replace("ROLE_", "");
            notifications = notifications.stream()
                .filter(notification -> {
                    // Admin sees all notifications
                    if ("ROLE_ADMIN".equals(userRole)) {
                        return true;
                    }
                    // Staff only see notifications targeted to their role or without role targeting
                    return notification.getTargetRole() == null || 
                           notification.getTargetRole().equals(roleWithoutPrefix) ||
                           notification.getTargetRole().isEmpty();
                })
                .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notifications));
    }

    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        // Filter notifications based on user role - admin sees all, others see only relevant notifications
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            String userRole = auth.getAuthorities().iterator().next().getAuthority();
            // Remove ROLE_ prefix for comparison with targetRole
            String roleWithoutPrefix = userRole.replace("ROLE_", "");
            notifications = notifications.stream()
                .filter(notification -> {
                    // Admin sees all notifications
                    if ("ROLE_ADMIN".equals(userRole)) {
                        return true;
                    }
                    // Staff only see notifications targeted to their role or without role targeting
                    return notification.getTargetRole() == null || 
                           notification.getTargetRole().equals(roleWithoutPrefix) ||
                           notification.getTargetRole().isEmpty();
                })
                .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved", notifications));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        // Filter notifications based on user role - admin sees all, others see only relevant notifications
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            String userRole = auth.getAuthorities().iterator().next().getAuthority();
            // Remove ROLE_ prefix for comparison with targetRole
            String roleWithoutPrefix = userRole.replace("ROLE_", "");
            long count = notifications.stream()
                .filter(notification -> {
                    // Admin sees all notifications
                    if ("ROLE_ADMIN".equals(userRole)) {
                        return true;
                    }
                    // Staff only see notifications targeted to their role or without role targeting
                    return notification.getTargetRole() == null || 
                           notification.getTargetRole().equals(roleWithoutPrefix) ||
                           notification.getTargetRole().isEmpty();
                })
                .count();
            return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
        } else {
            long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
        }
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR', 'ROLE_TELLER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<String>> deleteNotification(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
    }
}
