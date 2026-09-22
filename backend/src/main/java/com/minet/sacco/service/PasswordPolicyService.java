package com.minet.sacco.service;

import com.minet.sacco.entity.PasswordHistory;
import com.minet.sacco.entity.PasswordPolicySettings;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.PasswordHistoryRepository;
import com.minet.sacco.repository.PasswordPolicySettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordPolicyService.class);

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private PasswordPolicySettingsRepository policySettingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Policy keys
    private static final String PASSWORD_EXPIRATION_DAYS = "PASSWORD_EXPIRATION_DAYS";
    private static final String PASSWORD_HISTORY_COUNT = "PASSWORD_HISTORY_COUNT";
    private static final String EXPIRATION_WARNING_DAYS = "EXPIRATION_WARNING_DAYS";

    /**
     * Get password policy setting
     */
    @Cacheable(value = "passwordPolicy", key = "#key")
    public PasswordPolicySettings getPolicy(String key) {
        return policySettingsRepository.findBySettingKey(key)
                .orElse(null);
    }

    /**
     * Get password expiration days
     */
    public int getPasswordExpirationDays() {
        PasswordPolicySettings setting = getPolicy(PASSWORD_EXPIRATION_DAYS);
        return setting != null ? setting.getAsInt() : 90;
    }

    /**
     * Get password history retention count
     */
    public int getPasswordHistoryCount() {
        PasswordPolicySettings setting = getPolicy(PASSWORD_HISTORY_COUNT);
        return setting != null ? setting.getAsInt() : 5;
    }

    /**
     * Get days before expiration to warn user
     */
    public int getExpirationWarningDays() {
        PasswordPolicySettings setting = getPolicy(EXPIRATION_WARNING_DAYS);
        return setting != null ? setting.getAsInt() : 14;
    }

    /**
     * Record password change in history
     */
    @Transactional
    public void recordPasswordChange(User user, String newPasswordHash, User changedByUser) {
        try {
            PasswordHistory history = new PasswordHistory(user, newPasswordHash, changedByUser);
            passwordHistoryRepository.save(history);

            // Clean up old password history entries
            int historyCount = getPasswordHistoryCount();
            cleanupOldPasswordHistory(user.getId(), historyCount);

            logger.info("Password change recorded for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error recording password change for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    /**
     * Check if password was recently used (in history)
     */
    public boolean isPasswordInHistory(User user, String newPassword) {
        try {
            int historyCount = getPasswordHistoryCount();
            List<PasswordHistory> recentPasswords = passwordHistoryRepository.findRecentPasswordHistory(user.getId(), historyCount);

            for (PasswordHistory history : recentPasswords) {
                if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                    logger.warn("User {} attempted to reuse a recent password", user.getUsername());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error checking password history for user {}: {}", user.getUsername(), e.getMessage());
        }

        return false;
    }

    /**
     * Set password expiration for a user
     */
    @Transactional
    public void setPasswordExpiration(User user) {
        int expirationDays = getPasswordExpirationDays();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expirationDays);
        
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(expiresAt);
        user.setPasswordExpirationWarningSent(false);

        logger.info("Password expiration set for user {} to {}", user.getUsername(), expiresAt);
    }

    /**
     * Check if password has expired
     */
    public boolean isPasswordExpired(User user) {
        if (user.getPasswordExpiresAt() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(user.getPasswordExpiresAt());
    }

    /**
     * Check if user should be warned about password expiration
     */
    public boolean shouldWarnAboutExpiration(User user) {
        if (user.getPasswordExpiresAt() == null || user.getPasswordExpirationWarningSent() == null || user.getPasswordExpirationWarningSent()) {
            return false;
        }

        int warningDays = getExpirationWarningDays();
        LocalDateTime warningDate = LocalDateTime.now().plusDays(warningDays);

        return user.getPasswordExpiresAt().isBefore(warningDate) && user.getPasswordExpiresAt().isAfter(LocalDateTime.now());
    }

    /**
     * Mark expiration warning as sent
     */
    @Transactional
    public void markExpirationWarningSent(User user) {
        user.setPasswordExpirationWarningSent(true);
        logger.info("Password expiration warning marked as sent for user: {}", user.getUsername());
    }

    /**
     * Get days until password expires
     */
    public int getDaysUntilExpiration(User user) {
        if (user.getPasswordExpiresAt() == null) {
            return -1;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(user.getPasswordExpiresAt())) {
            return 0; // Already expired
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(now, user.getPasswordExpiresAt());
        return (int) days;
    }

    /**
     * Cleanup old password history
     */
    @Transactional
    public void cleanupOldPasswordHistory(Long userId, int retentionCount) {
        try {
            passwordHistoryRepository.deleteOldPasswordHistory(userId, retentionCount);
            logger.info("Cleaned up old password history for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error cleaning up password history for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * DTO for password expiration status
     */
    public static class PasswordExpirationStatus {
        private boolean expired;
        private boolean shouldWarn;
        private int daysUntilExpiration;
        private LocalDateTime expiresAt;
        private int warningDays;

        public PasswordExpirationStatus(boolean expired, boolean shouldWarn, int daysUntilExpiration, 
                                       LocalDateTime expiresAt, int warningDays) {
            this.expired = expired;
            this.shouldWarn = shouldWarn;
            this.daysUntilExpiration = daysUntilExpiration;
            this.expiresAt = expiresAt;
            this.warningDays = warningDays;
        }

        public boolean isExpired() {
            return expired;
        }

        public boolean isShouldWarn() {
            return shouldWarn;
        }

        public int getDaysUntilExpiration() {
            return daysUntilExpiration;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public int getWarningDays() {
            return warningDays;
        }
    }

    /**
     * Get comprehensive password expiration status for a user
     */
    public PasswordExpirationStatus getPasswordExpirationStatus(User user) {
        boolean expired = isPasswordExpired(user);
        boolean shouldWarn = shouldWarnAboutExpiration(user);
        int daysUntilExpiration = getDaysUntilExpiration(user);
        int warningDays = getExpirationWarningDays();

        return new PasswordExpirationStatus(expired, shouldWarn, daysUntilExpiration, 
                user.getPasswordExpiresAt(), warningDays);
    }
}
