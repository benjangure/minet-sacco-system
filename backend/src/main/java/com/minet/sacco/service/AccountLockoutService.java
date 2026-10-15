package com.minet.sacco.service;

import com.minet.sacco.entity.LoginAttempt;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.LoginAttemptRepository;
import com.minet.sacco.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountLockoutService {

    private static final Logger logger = LoggerFactory.getLogger(AccountLockoutService.class);

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    /** Internal system: lockout is off unless APP_LOCKOUT_ENABLED=true. Failed attempts are still recorded. */
    @Value("${app.security.lockout-enabled:false}")
    private boolean lockoutEnabled;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.attempt-window-minutes:15}")
    private int attemptWindowMinutes;

    /**
     * Record a login attempt
     */
    @Transactional
    public void recordLoginAttempt(User user, String username, boolean success, String ipAddress, String userAgent, String failureReason) {
        try {
            LoginAttempt attempt = new LoginAttempt(user, username, success, ipAddress, userAgent, failureReason);
            loginAttemptRepository.save(attempt);

            if (success) {
                // Reset failed attempts on successful login
                resetFailedAttempts(user);
                logger.info("Login successful for user: {}", username);
            } else {
                // Increment failed attempts
                incrementFailedAttempts(user);
                logger.warn("Failed login attempt for user: {} (reason: {})", username, failureReason);
            }
        } catch (Exception e) {
            logger.error("Error recording login attempt for user {}: {}", username, e.getMessage());
        }
    }

    /**
     * Check if account is locked
     */
    public boolean isAccountLocked(User user) {
        if (user.getAccountLocked() == null || !user.getAccountLocked()) {
            return false;
        }

        // Lockout disabled: release any account that was locked earlier
        if (!lockoutEnabled) {
            unlockAccount(user);
            return false;
        }

        // Check if lockout period has expired
        if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
            // Unlock the account
            unlockAccount(user);
            return false;
        }

        return true;
    }

    /**
     * Check if account should be locked based on failed attempts
     */
    @Transactional
    public boolean shouldLockAccount(User user) {
        if (!lockoutEnabled) {
            return false;
        }
        LocalDateTime attemptWindowStart = LocalDateTime.now().minusMinutes(attemptWindowMinutes);

        long failedAttempts = loginAttemptRepository.countFailedAttemptsSince(user.getId(), attemptWindowStart);

        if (failedAttempts >= maxFailedAttempts) {
            // Lock the account
            lockAccount(user);
            return true;
        }

        return false;
    }

    /**
     * Lock user account
     */
    @Transactional
    public void lockAccount(User user) {
        user.setAccountLocked(true);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
        user.setFailedLoginAttempts((int) (long) maxFailedAttempts);
        userRepository.save(user);
        logger.warn("Account locked for user: {} until {}", user.getUsername(), user.getLockedUntil());
    }

    /**
     * Unlock user account
     */
    @Transactional
    public void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        logger.info("Account unlocked for user: {}", user.getUsername());
    }

    /**
     * Increment failed login attempts
     */
    @Transactional
    public void incrementFailedAttempts(User user) {
        int currentAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        user.setFailedLoginAttempts(currentAttempts + 1);
        userRepository.save(user);

        // Check if should lock account
        if (shouldLockAccount(user)) {
            logger.warn("Account locked due to too many failed attempts: {}", user.getUsername());
        }
    }

    /**
     * Reset failed login attempts on successful login
     */
    @Transactional
    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    /**
     * Get account lockout status
     */
    public AccountLockoutStatus getAccountLockoutStatus(User user) {
        boolean isLocked = isAccountLocked(user);
        LocalDateTime attemptWindowStart = LocalDateTime.now().minusMinutes(attemptWindowMinutes);
        long failedAttemptsInWindow = loginAttemptRepository.countFailedAttemptsSince(user.getId(), attemptWindowStart);

        return new AccountLockoutStatus(
            isLocked,
            user.getLockedUntil(),
            (int) failedAttemptsInWindow,
            maxFailedAttempts - (int) failedAttemptsInWindow,
            maxFailedAttempts,
            lockoutDurationMinutes
        );
    }

    /**
     * Cleanup old login attempts (can be called periodically)
     */
    @Transactional
    public void cleanupOldAttempts() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            loginAttemptRepository.deleteOldAttempts(cutoffDate);
            logger.info("Cleaned up login attempts older than {}", cutoffDate);
        } catch (Exception e) {
            logger.error("Error cleaning up old login attempts: {}", e.getMessage());
        }
    }

    /**
     * DTO for account lockout status
     */
    public static class AccountLockoutStatus {
        private boolean locked;
        private LocalDateTime lockedUntil;
        private int failedAttempts;
        private int attemptsRemaining;
        private int maxAttempts;
        private int lockoutDurationMinutes;

        public AccountLockoutStatus(boolean locked, LocalDateTime lockedUntil, int failedAttempts, 
                                    int attemptsRemaining, int maxAttempts, int lockoutDurationMinutes) {
            this.locked = locked;
            this.lockedUntil = lockedUntil;
            this.failedAttempts = failedAttempts;
            this.attemptsRemaining = attemptsRemaining;
            this.maxAttempts = maxAttempts;
            this.lockoutDurationMinutes = lockoutDurationMinutes;
        }

        public boolean isLocked() {
            return locked;
        }

        public LocalDateTime getLockedUntil() {
            return lockedUntil;
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }

        public int getAttemptsRemaining() {
            return attemptsRemaining;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public int getLockoutDurationMinutes() {
            return lockoutDurationMinutes;
        }
    }
}
