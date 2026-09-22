package com.minet.sacco.service;

import com.minet.sacco.entity.LoginAttempt;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.LoginAttemptRepository;
import com.minet.sacco.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Account Lockout Service Tests")
class AccountLockoutServiceTest {

    @Autowired
    private AccountLockoutService lockoutService;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(User.Role.ADMIN);
        testUser.setEnabled(true);
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should record successful login attempt")
    void testRecordSuccessfulLoginAttempt() {
        lockoutService.recordLoginAttempt(testUser, testUser.getUsername(), true, "127.0.0.1", "Mozilla/5.0", null);

        var attempts = loginAttemptRepository.findByUserId(testUser.getId());
        assertFalse(attempts.isEmpty());
        assertTrue(attempts.get(0).getSuccess());
    }

    @Test
    @DisplayName("Should record failed login attempt")
    void testRecordFailedLoginAttempt() {
        lockoutService.recordLoginAttempt(testUser, testUser.getUsername(), false, "127.0.0.1", "Mozilla/5.0", "Invalid credentials");

        var attempts = loginAttemptRepository.findByUserId(testUser.getId());
        assertFalse(attempts.isEmpty());
        assertFalse(attempts.get(0).getSuccess());
        assertEquals("Invalid credentials", attempts.get(0).getFailureReason());
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void testResetFailedAttemptsOnSuccess() {
        // Record some failed attempts
        for (int i = 0; i < 3; i++) {
            lockoutService.recordLoginAttempt(testUser, testUser.getUsername(), false, "127.0.0.1", "Mozilla/5.0", "Invalid");
        }

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertEquals(3, updatedUser.getFailedLoginAttempts());

        // Record successful login
        lockoutService.recordLoginAttempt(testUser, testUser.getUsername(), true, "127.0.0.1", "Mozilla/5.0", null);

        updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertEquals(0, updatedUser.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should lock account after max failed attempts")
    void testLockAccountAfterMaxFailedAttempts() {
        // Record 5 failed attempts (max)
        for (int i = 0; i < 5; i++) {
            lockoutService.recordLoginAttempt(testUser, testUser.getUsername(), false, "127.0.0.1", "Mozilla/5.0", "Invalid");
            testUser = userRepository.findById(testUser.getId()).orElse(testUser);
        }

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertTrue(updatedUser.getAccountLocked(), "Account should be locked after 5 failed attempts");
        assertNotNull(updatedUser.getLockedUntil());
    }

    @Test
    @DisplayName("Should check if account is locked")
    void testIsAccountLocked() {
        assertFalse(lockoutService.isAccountLocked(testUser));

        lockoutService.lockAccount(testUser);
        testUser = userRepository.findById(testUser.getId()).orElse(testUser);

        assertTrue(lockoutService.isAccountLocked(testUser));
    }

    @Test
    @DisplayName("Should unlock account when lockout period expires")
    void testUnlockAccountWhenLockoutExpires() {
        // Lock account with expired lockout
        testUser.setAccountLocked(true);
        testUser.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        testUser = userRepository.save(testUser);

        assertFalse(lockoutService.isAccountLocked(testUser), "Account should be unlocked when lockout period expires");
    }

    @Test
    @DisplayName("Should manually unlock account")
    void testManualUnlock() {
        lockoutService.lockAccount(testUser);
        testUser = userRepository.findById(testUser.getId()).orElse(testUser);
        assertTrue(testUser.getAccountLocked());

        lockoutService.unlockAccount(testUser);
        testUser = userRepository.findById(testUser.getId()).orElse(testUser);

        assertFalse(testUser.getAccountLocked());
        assertEquals(0, testUser.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should get accurate account lockout status")
    void testGetAccountLockoutStatus() {
        AccountLockoutService.AccountLockoutStatus status = lockoutService.getAccountLockoutStatus(testUser);

        assertFalse(status.isLocked());
        assertEquals(0, status.getFailedAttempts());
        assertEquals(5, status.getAttemptsRemaining());
        assertEquals(5, status.getMaxAttempts());
    }

    @Test
    @DisplayName("Should track IP address for security audit")
    void testTrackIPAddress() {
        String ipAddress = "192.168.1.100";
        lockoutService.recordLoginAttempt(testUser, testUser.getUsername(), false, ipAddress, "Mozilla/5.0", "Invalid");

        var attempts = loginAttemptRepository.findByUserId(testUser.getId());
        assertEquals(ipAddress, attempts.get(0).getIpAddress());
    }
}
