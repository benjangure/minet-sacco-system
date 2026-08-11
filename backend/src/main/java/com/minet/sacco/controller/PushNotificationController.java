package com.minet.sacco.controller;

import com.minet.sacco.dto.PushNotificationDTO;
import com.minet.sacco.dto.PushSubscriptionDTO;
import com.minet.sacco.entity.PushSubscription;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.service.PushNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Push Notification Management
 * Provides endpoints for subscribing, unsubscribing, and testing push notifications
 */
@RestController
@RequestMapping("/api/member/push")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PushNotificationController {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationController.class);

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Subscribe to push notifications
     * POST /api/member/push/subscribe
     */
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @RequestBody PushSubscriptionDTO subscriptionDTO,
            HttpServletRequest request) {
        
        log.info("Push notification subscription request received");

        try {
            // Check if push service is configured
            if (!pushNotificationService.isConfigured()) {
                log.error("Push notification service is not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(createErrorResponse("Push notification service is not configured"));
            }

            // Get authenticated user
            User user = getAuthenticatedUser();
            if (user == null) {
                log.error("User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            // Validate subscription data
            if (subscriptionDTO.getEndpoint() == null || subscriptionDTO.getEndpoint().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Endpoint is required"));
            }

            if (subscriptionDTO.getKeys() == null || 
                subscriptionDTO.getKeys().getP256dh() == null || 
                subscriptionDTO.getKeys().getAuth() == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Subscription keys are required"));
            }

            // Get user agent from request
            String userAgent = request.getHeader("User-Agent");

            // Subscribe user
            PushSubscription subscription = pushNotificationService.subscribe(
                    user, subscriptionDTO, userAgent);

            log.info("✅ User {} subscribed to push notifications successfully", user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully subscribed to push notifications");
            response.put("data", Map.of(
                    "subscriptionId", subscription.getId(),
                    "endpoint", subscription.getEndpoint()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error subscribing to push notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to subscribe to push notifications: " + e.getMessage()));
        }
    }

    /**
     * Unsubscribe from push notifications
     * POST /api/member/push/unsubscribe
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody PushSubscriptionDTO subscriptionDTO) {
        log.info("Push notification unsubscribe request received");

        try {
            // Get authenticated user
            User user = getAuthenticatedUser();
            if (user == null) {
                log.error("User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            // Validate endpoint
            if (subscriptionDTO.getEndpoint() == null || subscriptionDTO.getEndpoint().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Endpoint is required"));
            }

            // Unsubscribe user
            boolean unsubscribed = pushNotificationService.unsubscribe(
                    user, subscriptionDTO.getEndpoint());

            if (unsubscribed) {
                log.info("✅ User {} unsubscribed from push notifications", user.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Successfully unsubscribed from push notifications");

                return ResponseEntity.ok(response);
            } else {
                log.warn("No subscription found for user {}", user.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "No active subscription found");

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("❌ Error unsubscribing from push notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to unsubscribe: " + e.getMessage()));
        }
    }

    /**
     * Get subscription status
     * GET /api/member/push/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        log.info("Push notification status request received");

        try {
            // Get authenticated user
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            // Check if user has active subscriptions
            boolean hasSubscription = pushNotificationService.hasActiveSubscription(user);
            List<PushSubscription> subscriptions = pushNotificationService.getUserSubscriptions(user);

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("subscribed", hasSubscription);
            statusData.put("subscriptionCount", subscriptions.size());
            statusData.put("configured", pushNotificationService.isConfigured());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statusData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting subscription status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get status: " + e.getMessage()));
        }
    }

    /**
     * Send test notification
     * POST /api/member/push/test
     */
    @PostMapping("/test")
    public ResponseEntity<?> sendTestNotification() {
        log.info("Test push notification request received");

        try {
            // Get authenticated user
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("User not authenticated"));
            }

            // Check if push service is configured
            if (!pushNotificationService.isConfigured()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(createErrorResponse("Push notification service is not configured"));
            }

            // Check if user has subscriptions
            if (!pushNotificationService.hasActiveSubscription(user)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("No active push subscription found"));
            }

            // Create test notification
            PushNotificationDTO testNotification = new PushNotificationDTO.Builder(
                    "🎉 Test Notification",
                    "This is a test notification from Minet SACCO. Your push notifications are working!"
            )
                    .type("SYSTEM")
                    .url("/member/dashboard?tab=notifications")
                    .icon("/icon-512.png")
                    .badge("/icon-192.png")
                    .tag("test-notification")
                    .build();

            // Send notification
            Map<String, Object> result = pushNotificationService.sendNotificationToUser(
                    user, testNotification);

            log.info("✅ Test notification sent to user {}: {}", user.getId(), result);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.get("success"));
            response.put("message", "Test notification sent successfully");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error sending test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to send test notification: " + e.getMessage()));
        }
    }

    /**
     * Get VAPID public key (for frontend configuration)
     * GET /api/member/push/vapid-public-key
     */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<?> getVapidPublicKey() {
        try {
            String publicKey = pushNotificationService.getVapidPublicKey();

            if (publicKey == null || publicKey.isEmpty()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(createErrorResponse("VAPID public key is not configured"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of("publicKey", publicKey));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting VAPID public key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get VAPID public key"));
        }
    }

    /**
     * Cleanup old subscriptions (Admin only)
     * POST /api/member/push/cleanup
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupOldSubscriptions() {
        log.info("Push subscription cleanup request received");

        try {
            // Get authenticated user
            User user = getAuthenticatedUser();
            if (user == null || !isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Admin access required"));
            }

            // Perform cleanup
            pushNotificationService.cleanupOldSubscriptions();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Old subscriptions cleaned up successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error cleaning up subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to cleanup subscriptions: " + e.getMessage()));
        }
    }

    /**
     * Get authenticated user from security context
     */
    private User getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            String username = authentication.getName();
            return userRepository.findByUsername(username).orElse(null);

        } catch (Exception e) {
            log.error("Error getting authenticated user", e);
            return null;
        }
    }

    /**
     * Check if user is admin
     */
    private boolean isAdmin(User user) {
        return user.getRole() != null && 
               (user.getRole().name().equals("ADMIN") || 
                user.getRole().name().equals("SUPER_ADMIN"));
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
