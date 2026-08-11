package com.minet.sacco.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minet.sacco.dto.PushNotificationDTO;
import com.minet.sacco.dto.PushSubscriptionDTO;
import com.minet.sacco.entity.PushSubscription;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing Web Push notifications
 * Uses the web-push library to send push notifications to subscribed users
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${push.vapid.public.key:}")
    private String vapidPublicKey;

    @Value("${push.vapid.private.key:}")
    private String vapidPrivateKey;

    @Value("${push.vapid.subject:mailto:admin@minetsacco.co.ke}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        try {
            // Add BouncyCastle as security provider
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            // Validate VAPID configuration
            if (vapidPublicKey == null || vapidPublicKey.isEmpty()) {
                log.warn("VAPID public key is not configured. Push notifications will not work.");
                return;
            }

            if (vapidPrivateKey == null || vapidPrivateKey.isEmpty()) {
                log.warn("VAPID private key is not configured. Push notifications will not work.");
                return;
            }

            // Initialize Push Service
            pushService = new PushService();
            pushService.setPublicKey(vapidPublicKey);
            pushService.setPrivateKey(vapidPrivateKey);
            pushService.setSubject(vapidSubject);

            log.info("✅ Push Notification Service initialized successfully");
            log.info("VAPID Subject: {}", vapidSubject);

        } catch (GeneralSecurityException e) {
            log.error("❌ Failed to initialize Push Notification Service", e);
        }
    }

    /**
     * Subscribe a user to push notifications
     */
    @Transactional
    public PushSubscription subscribe(User user, PushSubscriptionDTO subscriptionDTO, String userAgent) {
        log.info("Subscribing user {} to push notifications", user.getId());

        try {
            // Check if subscription already exists
            PushSubscription existingSubscription = pushSubscriptionRepository
                    .findByUserAndEndpoint(user, subscriptionDTO.getEndpoint())
                    .orElse(null);

            if (existingSubscription != null) {
                // Update existing subscription
                existingSubscription.setP256dhKey(subscriptionDTO.getKeys().getP256dh());
                existingSubscription.setAuthKey(subscriptionDTO.getKeys().getAuth());
                existingSubscription.setUserAgent(userAgent);
                existingSubscription.setIsActive(true);
                existingSubscription.setLastUsedAt(LocalDateTime.now());
                
                log.info("Updated existing subscription for user {}", user.getId());
                return pushSubscriptionRepository.save(existingSubscription);
            }

            // Create new subscription
            PushSubscription subscription = new PushSubscription();
            subscription.setUser(user);
            subscription.setEndpoint(subscriptionDTO.getEndpoint());
            subscription.setP256dhKey(subscriptionDTO.getKeys().getP256dh());
            subscription.setAuthKey(subscriptionDTO.getKeys().getAuth());
            subscription.setUserAgent(userAgent);
            subscription.setIsActive(true);

            PushSubscription saved = pushSubscriptionRepository.save(subscription);
            log.info("✅ Created new subscription for user {}", user.getId());
            
            return saved;

        } catch (Exception e) {
            log.error("❌ Error subscribing user {} to push notifications", user.getId(), e);
            throw new RuntimeException("Failed to subscribe to push notifications", e);
        }
    }

    /**
     * Unsubscribe a user from push notifications
     */
    @Transactional
    public boolean unsubscribe(User user, String endpoint) {
        log.info("Unsubscribing user {} from push notifications (endpoint: {})", user.getId(), endpoint);

        try {
            PushSubscription subscription = pushSubscriptionRepository
                    .findByUserAndEndpoint(user, endpoint)
                    .orElse(null);

            if (subscription != null) {
                pushSubscriptionRepository.delete(subscription);
                log.info("✅ Unsubscribed user {} successfully", user.getId());
                return true;
            }

            log.warn("No subscription found for user {} and endpoint", user.getId());
            return false;

        } catch (Exception e) {
            log.error("❌ Error unsubscribing user {}", user.getId(), e);
            throw new RuntimeException("Failed to unsubscribe from push notifications", e);
        }
    }

    /**
     * Check if user has active push subscriptions
     */
    public boolean hasActiveSubscription(User user) {
        return pushSubscriptionRepository.existsByUserAndIsActiveTrue(user);
    }

    /**
     * Get all active subscriptions for a user
     */
    public List<PushSubscription> getUserSubscriptions(User user) {
        return pushSubscriptionRepository.findByUserAndIsActiveTrue(user);
    }

    /**
     * Send push notification to a specific user
     */
    public Map<String, Object> sendNotificationToUser(User user, PushNotificationDTO notificationDTO) {
        log.info("Sending push notification to user {}: {}", user.getId(), notificationDTO.getTitle());

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserAndIsActiveTrue(user);
        
        if (subscriptions.isEmpty()) {
            log.warn("No active subscriptions found for user {}", user.getId());
            return createResponse(false, "No active subscriptions found", 0, 0);
        }

        return sendNotificationToSubscriptions(subscriptions, notificationDTO);
    }

    /**
     * Send push notification to multiple users
     */
    public Map<String, Object> sendNotificationToUsers(List<Long> userIds, PushNotificationDTO notificationDTO) {
        log.info("Sending push notification to {} users: {}", userIds.size(), notificationDTO.getTitle());

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findActiveByUserIds(userIds);
        
        if (subscriptions.isEmpty()) {
            log.warn("No active subscriptions found for the specified users");
            return createResponse(false, "No active subscriptions found", 0, 0);
        }

        return sendNotificationToSubscriptions(subscriptions, notificationDTO);
    }

    /**
     * Send push notification to all active subscriptions (broadcast)
     */
    public Map<String, Object> sendBroadcastNotification(PushNotificationDTO notificationDTO) {
        log.info("Broadcasting push notification: {}", notificationDTO.getTitle());

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByIsActiveTrue();
        
        if (subscriptions.isEmpty()) {
            log.warn("No active subscriptions found for broadcast");
            return createResponse(false, "No active subscriptions found", 0, 0);
        }

        return sendNotificationToSubscriptions(subscriptions, notificationDTO);
    }

    /**
     * Core method to send notifications to a list of subscriptions
     */
    private Map<String, Object> sendNotificationToSubscriptions(
            List<PushSubscription> subscriptions, 
            PushNotificationDTO notificationDTO) {

        if (pushService == null) {
            log.error("Push service is not initialized. Check VAPID configuration.");
            return createResponse(false, "Push service not configured", 0, 0);
        }

        int successCount = 0;
        int failureCount = 0;
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // Set default icon and badge if not provided
        if (notificationDTO.getIcon() == null) {
            notificationDTO.setIcon("/icon-512.png");
        }
        if (notificationDTO.getBadge() == null) {
            notificationDTO.setBadge("/icon-192.png");
        }

        for (PushSubscription subscription : subscriptions) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Convert notification DTO to JSON payload
                    String payload = objectMapper.writeValueAsString(notificationDTO);

                    // Create Web Push notification with string-based keys
                    Notification notification = new Notification(
                            subscription.getEndpoint(),
                            subscription.getP256dhKey(),
                            subscription.getAuthKey(),
                            payload.getBytes()
                    );

                    // Send the notification
                    HttpResponse response = pushService.send(notification);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 201 || statusCode == 200) {
                        // Success - update last used timestamp
                        updateLastUsed(subscription.getId());
                        log.debug("✅ Notification sent successfully to subscription {}", subscription.getId());
                        return true;
                    } else if (statusCode == 410 || statusCode == 404) {
                        // Subscription expired or invalid - deactivate it
                        deactivateSubscription(subscription.getId());
                        log.warn("Subscription {} is no longer valid (status: {}). Deactivated.", 
                                subscription.getId(), statusCode);
                        return false;
                    } else {
                        log.error("Failed to send notification to subscription {}: HTTP {}", 
                                subscription.getId(), statusCode);
                        return false;
                    }

                } catch (Exception e) {
                    log.error("Error sending notification to subscription {}", subscription.getId(), e);
                    return false;
                }
            });

            futures.add(future);
        }

        // Wait for all notifications to complete
        for (CompletableFuture<Boolean> future : futures) {
            try {
                if (future.get()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error waiting for notification result", e);
                failureCount++;
            }
        }

        log.info("Push notification sent: {} successful, {} failed out of {} total", 
                successCount, failureCount, subscriptions.size());

        boolean success = successCount > 0;
        String message = String.format("Sent to %d subscriptions (%d successful, %d failed)", 
                subscriptions.size(), successCount, failureCount);

        return createResponse(success, message, successCount, failureCount);
    }

    /**
     * Update last used timestamp for a subscription
     */
    @Transactional
    public void updateLastUsed(Long subscriptionId) {
        try {
            pushSubscriptionRepository.updateLastUsed(subscriptionId, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error updating last used timestamp for subscription {}", subscriptionId, e);
        }
    }

    /**
     * Deactivate a subscription (e.g., when it expires)
     */
    @Transactional
    public void deactivateSubscription(Long subscriptionId) {
        try {
            PushSubscription subscription = pushSubscriptionRepository.findById(subscriptionId).orElse(null);
            if (subscription != null) {
                subscription.setIsActive(false);
                pushSubscriptionRepository.save(subscription);
                log.info("Deactivated subscription {}", subscriptionId);
            }
        } catch (Exception e) {
            log.error("Error deactivating subscription {}", subscriptionId, e);
        }
    }

    /**
     * Cleanup old and inactive subscriptions
     */
    @Transactional
    public void cleanupOldSubscriptions() {
        try {
            // Deactivate subscriptions not used in 90 days
            LocalDateTime threshold = LocalDateTime.now().minusDays(90);
            int deactivated = pushSubscriptionRepository.deactivateOldSubscriptions(threshold);
            log.info("Deactivated {} old subscriptions", deactivated);

            // Delete inactive subscriptions older than 180 days
            LocalDateTime deleteThreshold = LocalDateTime.now().minusDays(180);
            int deleted = pushSubscriptionRepository.deleteInactiveSubscriptions(deleteThreshold);
            log.info("Deleted {} inactive subscriptions", deleted);

        } catch (Exception e) {
            log.error("Error cleaning up old subscriptions", e);
        }
    }

    /**
     * Helper method to create response map
     */
    private Map<String, Object> createResponse(boolean success, String message, 
                                               int successCount, int failureCount) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("successCount", successCount);
        response.put("failureCount", failureCount);
        return response;
    }

    /**
     * Get VAPID public key for frontend
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    /**
     * Check if push service is properly configured
     */
    public boolean isConfigured() {
        return pushService != null && 
               vapidPublicKey != null && !vapidPublicKey.isEmpty() &&
               vapidPrivateKey != null && !vapidPrivateKey.isEmpty();
    }
}
