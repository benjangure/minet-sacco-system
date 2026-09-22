package com.minet.sacco.service;

import com.minet.sacco.entity.PasswordResetToken;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.PasswordResetTokenRepository;
import com.minet.sacco.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Password Reset Service Tests")
class PasswordResetServiceTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("oldPassword123"));
        testUser.setRole(User.Role.ADMIN);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should generate secure token")
    void testGenerateSecureToken() {
        String token1 = passwordResetService.generateSecureToken();
        String token2 = passwordResetService.generateSecureToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2, "Generated tokens should be unique");
        assertTrue(token1.length() > 20, "Token should be sufficiently long");
    }

    @Test
    @DisplayName("Should hash token correctly")
    void testHashToken() {
        String token = "test_token_12345";
        String hash1 = passwordResetService.hashToken(token);
        String hash2 = passwordResetService.hashToken(token);

        assertEquals(hash1, hash2, "Same token should produce same hash");
        assertNotEquals(token, hash1, "Hash should be different from token");
    }

    @Test
    @DisplayName("Should validate reset token successfully")
    void testValidateResetToken() {
        String token = passwordResetService.generateSecureToken();
        String tokenHash = passwordResetService.hashToken(token);

        PasswordResetToken resetToken = new PasswordResetToken(
            testUser,
            token,
            tokenHash,
            PasswordResetToken.ResetType.STAFF,
            testUser.getEmail(),
            LocalDateTime.now().plusMinutes(30),
            null
        );
        tokenRepository.save(resetToken);

        boolean isValid = passwordResetService.validateResetToken(token);
        assertTrue(isValid, "Valid token should validate successfully");
    }

    @Test
    @DisplayName("Should reject expired token")
    void testValidateExpiredToken() {
        String token = passwordResetService.generateSecureToken();
        String tokenHash = passwordResetService.hashToken(token);

        PasswordResetToken resetToken = new PasswordResetToken(
            testUser,
            token,
            tokenHash,
            PasswordResetToken.ResetType.STAFF,
            testUser.getEmail(),
            LocalDateTime.now().minusMinutes(1), // Expired
            null
        );
        tokenRepository.save(resetToken);

        boolean isValid = passwordResetService.validateResetToken(token);
        assertFalse(isValid, "Expired token should not validate");
    }

    @Test
    @DisplayName("Should reject already used token")
    void testValidateUsedToken() {
        String token = passwordResetService.generateSecureToken();
        String tokenHash = passwordResetService.hashToken(token);

        PasswordResetToken resetToken = new PasswordResetToken(
            testUser,
            token,
            tokenHash,
            PasswordResetToken.ResetType.STAFF,
            testUser.getEmail(),
            LocalDateTime.now().plusMinutes(30),
            null
        );
        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);

        boolean isValid = passwordResetService.validateResetToken(token);
        assertFalse(isValid, "Used token should not validate");
    }

    @Test
    @DisplayName("Should reset password successfully")
    void testResetPasswordWithToken() {
        String token = passwordResetService.generateSecureToken();
        String tokenHash = passwordResetService.hashToken(token);

        PasswordResetToken resetToken = new PasswordResetToken(
            testUser,
            token,
            tokenHash,
            PasswordResetToken.ResetType.STAFF,
            testUser.getEmail(),
            LocalDateTime.now().plusMinutes(30),
            null
        );
        tokenRepository.save(resetToken);

        String newPassword = "newSecurePassword123!";
        boolean success = passwordResetService.resetPasswordWithToken(token, newPassword);

        assertTrue(success, "Password reset should succeed");
        
        // Verify password was changed
        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()), 
            "Password should be updated");

        // Verify token was marked as used
        PasswordResetToken usedToken = tokenRepository.findByTokenHash(tokenHash).orElse(null);
        assertNotNull(usedToken);
        assertTrue(usedToken.getIsUsed(), "Token should be marked as used");
    }

    @Test
    @DisplayName("Should fail password reset with invalid token")
    void testResetPasswordWithInvalidToken() {
        boolean success = passwordResetService.resetPasswordWithToken("invalid_token", "newPassword123");
        assertFalse(success, "Password reset with invalid token should fail");
    }

    @Test
    @DisplayName("Should fail password reset with weak password")
    void testResetPasswordWithWeakPassword() {
        String token = passwordResetService.generateSecureToken();
        String tokenHash = passwordResetService.hashToken(token);

        PasswordResetToken resetToken = new PasswordResetToken(
            testUser,
            token,
            tokenHash,
            PasswordResetToken.ResetType.STAFF,
            testUser.getEmail(),
            LocalDateTime.now().plusMinutes(30),
            null
        );
        tokenRepository.save(resetToken);

        boolean success = passwordResetService.resetPasswordWithToken(token, "weak");
        assertFalse(success, "Password reset with weak password should fail");
    }

    @Test
    @DisplayName("Should invalidate active tokens")
    void testInvalidateActiveTokens() {
        // Create multiple active tokens
        String token1 = passwordResetService.generateSecureToken();
        String token2 = passwordResetService.generateSecureToken();
        
        PasswordResetToken resetToken1 = new PasswordResetToken(
            testUser, token1, passwordResetService.hashToken(token1),
            PasswordResetToken.ResetType.STAFF, testUser.getEmail(),
            LocalDateTime.now().plusMinutes(30), null
        );
        PasswordResetToken resetToken2 = new PasswordResetToken(
            testUser, token2, passwordResetService.hashToken(token2),
            PasswordResetToken.ResetType.STAFF, testUser.getEmail(),
            LocalDateTime.now().plusMinutes(30), null
        );
        
        tokenRepository.save(resetToken1);
        tokenRepository.save(resetToken2);

        // Invalidate all active tokens
        passwordResetService.invalidateActiveTokens(testUser.getId());

        // Verify both tokens are marked as used
        assertTrue(tokenRepository.findByTokenHash(passwordResetService.hashToken(token1)).orElse(null).getIsUsed());
        assertTrue(tokenRepository.findByTokenHash(passwordResetService.hashToken(token2)).orElse(null).getIsUsed());
    }
}
