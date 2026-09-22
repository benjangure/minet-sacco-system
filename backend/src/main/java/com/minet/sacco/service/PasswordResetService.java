package com.minet.sacco.service;

import com.minet.sacco.entity.PasswordResetToken;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.PasswordResetTokenRepository;
import com.minet.sacco.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Value("${app.password.reset.token.expiration.minutes:30}")
    private long tokenExpirationMinutes;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Generate a secure random token for password reset
     */
    public String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hash the token using SHA-256 for storage in database
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing token: {}", e.getMessage());
            throw new RuntimeException("Error generating token hash", e);
        }
    }

    /**
     * Request password reset for staff user
     * Returns: Generated reset token or null if user not found
     */
    @Transactional
    public String requestPasswordResetForStaff(String username) {
        logger.info("Password reset requested for staff user: {}", username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("Password reset requested for non-existent staff user: {}", username);
            return null;
        }

        User user = userOpt.get();
        
        // Verify user is staff (not member)
        if (user.getRole() == User.Role.MEMBER) {
            logger.warn("Password reset requested for member user via staff endpoint: {}", username);
            return null;
        }

        // Generate token
        String token = generateSecureToken();
        String tokenHash = hashToken(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpirationMinutes);

        // Create reset token record
        PasswordResetToken resetToken = new PasswordResetToken(
            user,
            token,
            tokenHash,
            PasswordResetToken.ResetType.STAFF,
            user.getEmail(),
            expiresAt,
            null // No creator for user-initiated requests
        );

        tokenRepository.save(resetToken);
        logger.info("Password reset token created for staff user: {} (expires at: {})", username, expiresAt);

        // Send reset email
        String resetLink = buildResetLink(token, false);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink, false);

        return token;
    }

    /**
     * Request password reset for member
     * Can be initiated by member (forgot password) or admin (reset member password)
     */
    @Transactional
    public String requestPasswordResetForMember(String username, boolean isAdminInitiated) {
        logger.info("Password reset requested for member user: {} (admin initiated: {})", username, isAdminInitiated);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("Password reset requested for non-existent member user: {}", username);
            return null;
        }

        User user = userOpt.get();
        
        // Verify user is member
        if (user.getRole() != User.Role.MEMBER) {
            logger.warn("Password reset requested for non-member user via member endpoint: {}", username);
            return null;
        }

        // Generate token
        String token = generateSecureToken();
        String tokenHash = hashToken(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpirationMinutes);

        // Create reset token record
        PasswordResetToken resetToken = new PasswordResetToken(
            user,
            token,
            tokenHash,
            PasswordResetToken.ResetType.MEMBER,
            user.getEmail(),
            expiresAt,
            isAdminInitiated ? 0L : null // Set to 0 for admin-initiated, null for user-initiated
        );

        tokenRepository.save(resetToken);
        logger.info("Password reset token created for member user: {} (expires at: {})", username, expiresAt);

        // Send reset email
        String resetLink = buildResetLink(token, true);
        emailService.sendMemberPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);

        return token;
    }

    /**
     * Validate a password reset token
     */
    public boolean validateResetToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Validation attempted with empty token");
            return false;
        }

        String tokenHash = hashToken(token);
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (resetTokenOpt.isEmpty()) {
            logger.warn("Validation failed: token not found");
            return false;
        }

        PasswordResetToken resetToken = resetTokenOpt.get();

        // Check if token is expired
        if (resetToken.isExpired()) {
            logger.warn("Validation failed: token expired for user: {}", resetToken.getUser().getUsername());
            return false;
        }

        // Check if token is already used
        if (resetToken.getIsUsed()) {
            logger.warn("Validation failed: token already used for user: {}", resetToken.getUser().getUsername());
            return false;
        }

        logger.info("Token validation successful for user: {}", resetToken.getUser().getUsername());
        return true;
    }

    /**
     * Get password reset token details without exposing sensitive info
     */
    public PasswordResetTokenInfo getTokenInfo(String token) {
        if (!validateResetToken(token)) {
            return null;
        }

        String tokenHash = hashToken(token);
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (resetTokenOpt.isEmpty()) {
            return null;
        }

        PasswordResetToken resetToken = resetTokenOpt.get();
        return new PasswordResetTokenInfo(
            resetToken.getUser().getUsername(),
            resetToken.getUser().getEmail(),
            resetToken.getResetType().toString(),
            resetToken.getExpiresAt()
        );
    }

    /**
     * Reset password using valid token
     */
    @Transactional
    public boolean resetPasswordWithToken(String token, String newPassword) {
        if (!validateResetToken(token)) {
            logger.warn("Password reset attempted with invalid token");
            return false;
        }

        if (newPassword == null || newPassword.length() < 6) {
            logger.warn("Password reset attempted with invalid password");
            return false;
        }

        String tokenHash = hashToken(token);
        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (resetTokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = resetTokenOpt.get();
        User user = resetToken.getUser();

        try {
            // Update user password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Mark token as used
            resetToken.setIsUsed(true);
            resetToken.setUsedAt(LocalDateTime.now());
            tokenRepository.save(resetToken);

            // Log activity
            userService.logActivity(user, "PASSWORD_RESET", "Password reset using token");

            logger.info("Password successfully reset for user: {}", user.getUsername());
            return true;

        } catch (Exception e) {
            logger.error("Error resetting password for user {}: {}", user.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Invalidate all active tokens for a user (e.g., when they reset their password manually)
     */
    @Transactional
    public void invalidateActiveTokens(Long userId) {
        List<PasswordResetToken> activeTokens = tokenRepository.findActiveTokensByUserId(
            userId,
            PasswordResetToken.ResetType.STAFF,
            LocalDateTime.now()
        );

        // Also get member tokens
        List<PasswordResetToken> activeMemberTokens = tokenRepository.findActiveTokensByUserId(
            userId,
            PasswordResetToken.ResetType.MEMBER,
            LocalDateTime.now()
        );

        activeTokens.addAll(activeMemberTokens);

        for (PasswordResetToken token : activeTokens) {
            token.setIsUsed(true);
            token.setUsedAt(LocalDateTime.now());
            tokenRepository.save(token);
        }

        if (!activeTokens.isEmpty()) {
            logger.info("Invalidated {} active tokens for user: {}", activeTokens.size(), userId);
        }
    }

    /**
     * Clean up expired tokens (can be called periodically)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            List<PasswordResetToken> expiredTokens = tokenRepository.findExpiredTokens(LocalDateTime.now());
            if (!expiredTokens.isEmpty()) {
                tokenRepository.deleteAll(expiredTokens);
                logger.info("Cleaned up {} expired password reset tokens", expiredTokens.size());
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired tokens: {}", e.getMessage());
        }
    }

    /**
     * Build password reset link for email
     */
    private String buildResetLink(String token, boolean isMember) {
        String path = isMember ? "/member/reset-password" : "/staff/reset-password";
        return frontendUrl + path + "?token=" + token;
    }

    /**
     * DTO for password reset token information
     */
    public static class PasswordResetTokenInfo {
        private String username;
        private String email;
        private String resetType;
        private LocalDateTime expiresAt;

        public PasswordResetTokenInfo(String username, String email, String resetType, LocalDateTime expiresAt) {
            this.username = username;
            this.email = email;
            this.resetType = resetType;
            this.expiresAt = expiresAt;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getResetType() {
            return resetType;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
    }
}
