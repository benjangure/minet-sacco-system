package com.minet.sacco.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minet.sacco.dto.PasswordResetRequestDTO;
import com.minet.sacco.dto.PasswordResetConfirmDTO;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Password Reset Integration Tests")
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private com.minet.sacco.service.EmailService emailService;

    private User testStaffUser;
    private User testMemberUser;
    private String resetToken;

    @BeforeEach
    void setUp() {
        // Create test staff user
        testStaffUser = new User();
        testStaffUser.setUsername("staff_user");
        testStaffUser.setEmail("staff@example.com");
        testStaffUser.setPassword(passwordEncoder.encode("OldPassword123!"));
        testStaffUser.setRole(User.Role.ADMIN);
        testStaffUser.setEnabled(true);
        testStaffUser.setCreatedAt(LocalDateTime.now());
        testStaffUser = userRepository.save(testStaffUser);

        // Create test member user
        testMemberUser = new User();
        testMemberUser.setUsername("member_123");
        testMemberUser.setEmail("member@example.com");
        testMemberUser.setPassword(passwordEncoder.encode("InitialPassword123!"));
        testMemberUser.setRole(User.Role.MEMBER);
        testMemberUser.setEnabled(true);
        testMemberUser.setCreatedAt(LocalDateTime.now());
        testMemberUser = userRepository.save(testMemberUser);
    }

    @Test
    @DisplayName("Should request password reset for staff user")
    void testRequestPasswordResetStaff() throws Exception {
        PasswordResetRequestDTO request = new PasswordResetRequestDTO("staff@example.com");

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("reset link")));
    }

    @Test
    @DisplayName("Should request password reset for member user")
    void testRequestPasswordResetMember() throws Exception {
        PasswordResetRequestDTO request = new PasswordResetRequestDTO("member@example.com");

        mockMvc.perform(post("/api/auth/password-reset/member/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("reset link")));
    }

    @Test
    @DisplayName("Should handle non-existent email gracefully")
    void testRequestPasswordResetNonExistentEmail() throws Exception {
        PasswordResetRequestDTO request = new PasswordResetRequestDTO("nonexistent@example.com");

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("If an account exists")));
    }

    @Test
    @DisplayName("Should validate valid reset token")
    void testValidateValidResetToken() throws Exception {
        // Generate a valid token
        resetToken = passwordResetService.generateSecureToken();
        passwordResetService.requestPasswordResetForStaff(testStaffUser.getUsername());

        mockMvc.perform(post("/api/auth/password-reset/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"" + resetToken + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject invalid token")
    void testValidateInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"invalid_token_xyz\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("Should reset password successfully with valid token")
    void testResetPasswordSuccess() throws Exception {
        // First request a password reset
        passwordResetService.requestPasswordResetForStaff(testStaffUser.getUsername());

        // Get the token (in a real scenario, this would come from email)
        resetToken = passwordResetService.generateSecureToken();

        PasswordResetConfirmDTO resetRequest = new PasswordResetConfirmDTO(
            resetToken,
            "NewPassword123!"
        );

        mockMvc.perform(post("/api/auth/password-reset/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject weak password on reset")
    void testResetPasswordWithWeakPassword() throws Exception {
        resetToken = passwordResetService.generateSecureToken();

        PasswordResetConfirmDTO resetRequest = new PasswordResetConfirmDTO(
            resetToken,
            "weak" // Too weak
        );

        mockMvc.perform(post("/api/auth/password-reset/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require email for password reset request")
    void testPasswordResetRequiresEmail() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require token for validation")
    void testValidateTokenRequiresToken() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
