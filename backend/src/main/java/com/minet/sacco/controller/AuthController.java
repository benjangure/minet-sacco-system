package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.AuthRequest;
import com.minet.sacco.dto.AuthResponse;
import com.minet.sacco.dto.SetupPasswordRequest;
import com.minet.sacco.dto.PasswordResetRequestDTO;
import com.minet.sacco.dto.PasswordResetConfirmDTO;
import com.minet.sacco.dto.TokenValidationDTO;
import com.minet.sacco.entity.User;
import com.minet.sacco.entity.PasswordResetToken;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.repository.MemberCredentialRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.security.CustomUserDetailsService;
import com.minet.sacco.security.JwtUtil;
import com.minet.sacco.service.PasswordResetService;
import com.minet.sacco.service.AccountLockoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired(required = false)
    private com.minet.sacco.service.DeviceTrackingService deviceTrackingService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private AccountLockoutService accountLockoutService;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest, HttpServletRequest request) throws Exception {
        User user = null;
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            // Find user first to check lockout status
            Optional<User> userOpt = userRepository.findByUsername(authRequest.getUsername());
            if (userOpt.isPresent()) {
                user = userOpt.get();
                
                // Check if account is locked
                if (accountLockoutService.isAccountLocked(user)) {
                    AccountLockoutService.AccountLockoutStatus status = accountLockoutService.getAccountLockoutStatus(user);
                    accountLockoutService.recordLoginAttempt(user, authRequest.getUsername(), false, ipAddress, userAgent, "Account locked");
                    throw new Exception("Account is locked. Please try again after " + status.getLockoutDurationMinutes() + " minutes or contact support.");
                }
            }

            System.out.println("DEBUG: Attempting authentication for user: " + authRequest.getUsername());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            System.out.println("DEBUG: Authentication successful");
        } catch (BadCredentialsException e) {
            System.err.println("ERROR: Bad credentials for user: " + authRequest.getUsername());
            
            // Record failed login attempt
            if (user != null) {
                accountLockoutService.recordLoginAttempt(user, authRequest.getUsername(), false, ipAddress, userAgent, "Invalid credentials");
                
                // Track in device tracking service
                if (deviceTrackingService != null) {
                    try {
                        deviceTrackingService.trackLogin(user, request, false, "Invalid credentials");
                    } catch (Exception ex) {
                        System.err.println("Failed to track failed login: " + ex.getMessage());
                    }
                }
            }
            
            throw new Exception("Incorrect username or password", e);
        } catch (Exception e) {
            System.err.println("ERROR: Authentication failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        try {
            System.out.println("DEBUG: Loading user details");
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
            
            // Check if user is a MEMBER - members cannot login to staff portal
            user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            
            if (user.getRole() == User.Role.MEMBER) {
                System.err.println("ERROR: Member user attempting staff login: " + authRequest.getUsername());
                throw new Exception("Members must use the member portal. Please use the member login page.");
            }
            
            System.out.println("DEBUG: User details loaded, generating JWT token");
            final String jwt = jwtUtil.generateToken(userDetails);
            System.out.println("DEBUG: JWT token generated successfully");
            
            // Record successful login attempt
            accountLockoutService.recordLoginAttempt(user, authRequest.getUsername(), true, ipAddress, userAgent, null);
            
            // Track successful login and check for new device
            if (deviceTrackingService != null) {
                try {
                    deviceTrackingService.trackLogin(user, request, true, null);
                } catch (Exception ex) {
                    System.err.println("Failed to track login: " + ex.getMessage());
                }
            }

            return ResponseEntity.ok(new AuthResponse(jwt));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to generate JWT token: " + e.getMessage());
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
        }
    }

    /**
     * Member login endpoint - same as admin login but for members
     * Username: Phone number or Employee ID
     * Password: National ID (initial), then custom password after first login
     */
    @PostMapping("/member/login")
    public ResponseEntity<?> memberLogin(@RequestBody AuthRequest authRequest, HttpServletRequest request) throws Exception {
        User user = null;
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            // Find user first to check lockout status
            Optional<User> userOpt = userRepository.findByUsername(authRequest.getUsername());
            if (userOpt.isPresent()) {
                user = userOpt.get();
                
                // Check if account is locked
                if (accountLockoutService.isAccountLocked(user)) {
                    AccountLockoutService.AccountLockoutStatus status = accountLockoutService.getAccountLockoutStatus(user);
                    accountLockoutService.recordLoginAttempt(user, authRequest.getUsername(), false, ipAddress, userAgent, "Account locked");
                    throw new Exception("Account is locked. Please try again after " + status.getLockoutDurationMinutes() + " minutes or contact support.");
                }
            }

            System.out.println("DEBUG: Member login attempt for: " + authRequest.getUsername());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            System.out.println("DEBUG: Member authentication successful");
        } catch (BadCredentialsException e) {
            System.err.println("ERROR: Invalid member credentials for: " + authRequest.getUsername());
            
            // Record failed login attempt
            if (user != null) {
                accountLockoutService.recordLoginAttempt(user, authRequest.getUsername(), false, ipAddress, userAgent, "Invalid credentials");
                
                // Track in device tracking service
                if (deviceTrackingService != null) {
                    try {
                        deviceTrackingService.trackLogin(user, request, false, "Invalid credentials");
                    } catch (Exception ex) {
                        System.err.println("Failed to track failed login: " + ex.getMessage());
                    }
                }
            }
            
            throw new Exception("Invalid username or password", e);
        } catch (Exception e) {
            System.err.println("ERROR: Member authentication failed: " + e.getMessage());
            throw e;
        }

        try {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
            
            // Get the user to extract memberId and verify role
            user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            
            System.out.println("DEBUG: User found - id=" + user.getId() + ", role=" + user.getRole() + ", firstLogin=" + user.isFirstLogin());
            
            // Only MEMBER role can login to member portal
            if (user.getRole() != User.Role.MEMBER) {
                System.err.println("ERROR: Non-member user attempting member login: " + authRequest.getUsername());
                throw new Exception("Staff users must use the staff login page. Please use the staff login.");
            }

            com.minet.sacco.entity.Member member = memberRepository.findById(user.getMemberId())
                    .orElseThrow(() -> new Exception("Member record not found"));

            if (member.getStatus() == com.minet.sacco.entity.Member.Status.EXITED) {
                System.err.println("ERROR: Exited member attempting login: " + authRequest.getUsername());
                throw new Exception("This account has exited the SACCO and can no longer access the member portal.");
            }
            
            System.out.println("DEBUG: Generating JWT token with memberId=" + user.getMemberId() + ", firstLogin=" + user.isFirstLogin());
            
            // Generate token with memberId and first-login status for member users
            final String jwt = jwtUtil.generateTokenWithMemberId(userDetails, user.getMemberId(), user.isFirstLogin());
            
            System.out.println("DEBUG: JWT token generated successfully, first login flag: " + user.isFirstLogin());
            
            // Record successful login attempt
            accountLockoutService.recordLoginAttempt(user, authRequest.getUsername(), true, ipAddress, userAgent, null);
            
            // Track successful login and check for new device
            if (deviceTrackingService != null) {
                try {
                    deviceTrackingService.trackLogin(user, request, true, null);
                } catch (Exception ex) {
                    System.err.println("Failed to track login: " + ex.getMessage());
                }
            }
            
            return ResponseEntity.ok(new AuthResponse(jwt, user.getMemberId(), user.isFirstLogin()));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to generate member JWT token: " + e.getMessage());
            throw new Exception(e.getMessage(), e);
        }
    }

    /**
     * Setup password endpoint for first-time member login
     * Allows members to set their password using their temporary password
     * SECURITY: Purges temporary password from database after member sets new one
     */
    @PostMapping("/member/setup-password")
    public ResponseEntity<?> setupPassword(@RequestBody SetupPasswordRequest request) throws Exception {
        try {
            System.out.println("DEBUG: setup-password endpoint called for user: " + request.getUsername());
            
            // Find user by username
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            
            System.out.println("DEBUG: User found, id=" + user.getId() + ", firstLogin=" + user.isFirstLogin());
            
            // Verify user is a member
            if (user.getRole() != User.Role.MEMBER) {
                System.err.println("ERROR: Non-member user attempted password setup: " + request.getUsername());
                throw new Exception("Only members can use this endpoint");
            }
            
            // Verify user is on first login
            if (!user.isFirstLogin()) {
                System.err.println("ERROR: Password setup attempt by non-first-login user: " + request.getUsername());
                throw new Exception("Password setup is only allowed for first-time login");
            }
            
            // Verify current password matches
            System.out.println("DEBUG: Verifying current password");
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                System.err.println("ERROR: Current password mismatch for user: " + request.getUsername());
                throw new Exception("Current password is incorrect");
            }
            
            System.out.println("DEBUG: Current password verified, encoding new password");
            
            // Update password and mark first login as complete
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setFirstLogin(false);
            user.setUpdatedAt(LocalDateTime.now());
            User savedUser = userRepository.save(user);
            
            System.out.println("DEBUG: User password updated and firstLogin set to false. New firstLogin value: " + savedUser.isFirstLogin());
            
            // Update credential tracking record and PURGE temporary password for security
            memberCredentialRepository.findByMemberId(user.getMemberId())
                .ifPresent(credential -> {
                    System.out.println("DEBUG: Updating member credential record");
                    credential.setPasswordChanged(true);
                    credential.setPasswordChangedAt(LocalDateTime.now());
                    credential.setPassword(null); // SECURITY: Purge temporary password from database
                    memberCredentialRepository.save(credential);
                    System.out.println("DEBUG: Member credential record updated");
                });
            
            System.out.println("DEBUG: Password setup completed successfully for user: " + request.getUsername());
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Password setup successful", null));
        } catch (Exception e) {
            System.err.println("ERROR: Password setup failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Health check endpoint for APK to verify backend connectivity
     * No authentication required - used by APK settings to test connection
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Backend is healthy", null));
    }

    /**
     * Reset a member's password back to their national ID
     * Sets first_login = true so they must change it on next login
     * 
     * Usage: POST /api/auth/admin/reset-member-password
     * Body: { "username": "13121" }
     * 
     * No authentication required for initial setup
     */
    @PostMapping("/admin/reset-member-password")
    public ResponseEntity<?> resetMemberPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String username = request.get("username");
            if (username == null || username.isBlank()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Username is required", null)
                );
            }
            
            System.out.println("=== Resetting password for member: " + username + " ===");
            
            // Find user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new Exception("User not found with username: " + username));
            
            // Verify it's a member
            if (user.getRole() != User.Role.MEMBER) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Only member accounts can be reset with this endpoint", null)
                );
            }
            
            // Get member to find national_id
            com.minet.sacco.entity.Member member = memberRepository.findById(user.getMemberId())
                    .orElseThrow(() -> new Exception("Member record not found"));
            
            // Use national_id as new password, fallback to member_number
            String newPassword = member.getNationalId() != null && !member.getNationalId().isBlank()
                ? member.getNationalId()
                : member.getMemberNumber();
            
            // Reset password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setFirstLogin(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            System.out.println("✓ Password reset for " + username + " to national ID: " + newPassword);
            
            var result = new java.util.HashMap<String, Object>();
            result.put("username", username);
            result.put("memberName", member.getFullName());
            result.put("newPassword", newPassword);
            result.put("firstLogin", true);
            
            return ResponseEntity.ok(new ApiResponse<>(true, 
                "Password reset successfully to national ID. Member must change password on first login.", 
                result));
            
        } catch (Exception e) {
            System.err.println("ERROR resetting password: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "Failed to reset password: " + e.getMessage(), null)
            );
        }
    }

    /**
     * One-time admin endpoint to create user accounts for all members
     * Creates missing user accounts with BCrypt-encoded passwords
     * 
     * Usage: POST /api/auth/admin/initialize-member-users
     * No authentication required for initial setup
     * 
     * This should be called once, then the endpoint can be removed or secured
     */
    @PostMapping("/admin/initialize-member-users")
    public ResponseEntity<?> initializeMemberUsers() {
        try {
            System.out.println("=== Starting Member User Account Initialization ===");
            
            // Get all members
            var allMembers = memberRepository.findAll();
            System.out.println("Found " + allMembers.size() + " total members");
            
            int created = 0;
            int updated = 0;
            int skipped = 0;
            java.util.List<String> errors = new java.util.ArrayList<>();
            
            for (var member : allMembers) {
                String username = member.getMemberNumber();
                
                if (username == null || username.isBlank()) {
                    skipped++;
                    continue;
                }
                
                try {
                    // Check if user already exists
                    var existingUser = userRepository.findByUsername(username);
                    
                    if (existingUser.isPresent()) {
                        // User exists - check if needs linking to member
                        User user = existingUser.get();
                        if (user.getMemberId() == null) {
                            // Link user to member and update password
                            String password = member.getNationalId() != null && !member.getNationalId().isBlank() 
                                ? member.getNationalId() 
                                : member.getMemberNumber();
                            user.setPassword(passwordEncoder.encode(password));
                            user.setMemberId(member.getId());
                            user.setUpdatedAt(LocalDateTime.now());
                            userRepository.save(user);
                            updated++;
                            System.out.println("Updated user: " + username);
                        }
                        // If already linked, skip
                    } else {
                        // Create new user account
                        User user = new User();
                        user.setUsername(username);
                        user.setEmail(member.getEmail() != null && !member.getEmail().isBlank()
                            ? member.getEmail()
                            : username + "@minet.sacco");
                        
                        // Use national_id as password, fallback to member_number
                        String password = member.getNationalId() != null && !member.getNationalId().isBlank() 
                            ? member.getNationalId() 
                            : member.getMemberNumber();
                        user.setPassword(passwordEncoder.encode(password));
                        
                        user.setRole(User.Role.MEMBER);
                        user.setMemberId(member.getId());
                        user.setEnabled(true);
                        user.setFirstLogin(true);
                        user.setCreatedAt(LocalDateTime.now());
                        
                        userRepository.save(user);
                        created++;
                        System.out.println("Created user: " + username);
                    }
                } catch (Exception e) {
                    errors.add("Failed to process member " + username + ": " + e.getMessage());
                    System.err.println("ERROR processing member " + username + ": " + e.getMessage());
                }
            }
            
            System.out.println("=== Initialization Complete ===");
            System.out.println("Created: " + created);
            System.out.println("Updated: " + updated);
            System.out.println("Skipped: " + skipped);
            System.out.println("Errors: " + errors.size());
            
            var result = new java.util.HashMap<String, Object>();
            result.put("success", true);
            result.put("created", created);
            result.put("updated", updated);
            result.put("skipped", skipped);
            result.put("totalMembers", allMembers.size());
            result.put("errors", errors);
            result.put("message", "Successfully created " + created + " user accounts, updated " + updated + " accounts");
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Initialization complete", result));
        } catch (Exception e) {
            System.err.println("FATAL ERROR during member user initialization: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "Initialization failed: " + e.getMessage(), null)
            );
        }
    }

    // ======================== PASSWORD RESET ENDPOINTS ========================

    /**
     * Request password reset for staff user
     * Sends password reset email with secure token link
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDTO request) {
        try {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Email is required", null)
                );
            }

            // Find user by email
            User user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);

            if (user == null || user.getRole() == User.Role.MEMBER) {
                // Don't reveal whether user exists (security)
                return ResponseEntity.ok(new ApiResponse<>(true, 
                    "If an account exists with this email, a password reset link will be sent shortly", null));
            }

            // Generate reset token and send email
            String token = passwordResetService.requestPasswordResetForStaff(user.getUsername());

            if (token != null) {
                return ResponseEntity.ok(new ApiResponse<>(true, 
                    "Password reset link has been sent to your email address", null));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(true, 
                    "If an account exists with this email, a password reset link will be sent shortly", null));
            }

        } catch (Exception e) {
            System.err.println("Error requesting password reset: " + e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(true, 
                "If an account exists with this email, a password reset link will be sent shortly", null));
        }
    }

    /**
     * Request password reset for member user
     * Sends password reset email with secure token link
     */
    @PostMapping("/password-reset/member/request")
    public ResponseEntity<?> requestMemberPasswordReset(@RequestBody PasswordResetRequestDTO request) {
        try {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Email is required", null)
                );
            }

            // Find user by email
            User user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);

            if (user == null || user.getRole() != User.Role.MEMBER) {
                // Don't reveal whether user exists (security)
                return ResponseEntity.ok(new ApiResponse<>(true, 
                    "If a member account exists with this email, a password reset link will be sent shortly", null));
            }

            // Generate reset token and send email
            String token = passwordResetService.requestPasswordResetForMember(user.getUsername(), false);

            if (token != null) {
                return ResponseEntity.ok(new ApiResponse<>(true, 
                    "Password reset link has been sent to your email address", null));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(true, 
                    "If a member account exists with this email, a password reset link will be sent shortly", null));
            }

        } catch (Exception e) {
            System.err.println("Error requesting member password reset: " + e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(true, 
                "If a member account exists with this email, a password reset link will be sent shortly", null));
        }
    }

    /**
     * Validate a password reset token
     * Returns token information if valid, or error if invalid/expired
     */
    @PostMapping("/password-reset/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody java.util.Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new TokenValidationDTO(false, "Token is required")
                );
            }

            // Validate token
            if (!passwordResetService.validateResetToken(token)) {
                return ResponseEntity.ok(new TokenValidationDTO(false, 
                    "This password reset link has expired or is invalid. Please request a new one."));
            }

            // Get token info
            PasswordResetService.PasswordResetTokenInfo tokenInfo = passwordResetService.getTokenInfo(token);
            if (tokenInfo == null) {
                return ResponseEntity.ok(new TokenValidationDTO(false, 
                    "This password reset link is invalid"));
            }

            return ResponseEntity.ok(new TokenValidationDTO(true, 
                tokenInfo.getUsername(), 
                tokenInfo.getEmail(), 
                tokenInfo.getResetType(),
                tokenInfo.getExpiresAt(),
                "Token is valid"));

        } catch (Exception e) {
            System.err.println("Error validating token: " + e.getMessage());
            return ResponseEntity.status(500).body(
                new TokenValidationDTO(false, "Error validating token")
            );
        }
    }

    /**
     * Reset password using valid token
     * Updates the user's password and marks the token as used
     */
    @PostMapping("/password-reset/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetConfirmDTO request) {
        try {
            if (request.getToken() == null || request.getToken().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Token is required", null)
                );
            }

            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Password must be at least 6 characters long", null)
                );
            }

            // Reset password
            if (passwordResetService.resetPasswordWithToken(request.getToken(), request.getNewPassword())) {
                return ResponseEntity.ok(new ApiResponse<>(true, 
                    "Password has been reset successfully. You can now login with your new password.", null));
            } else {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, 
                    "Password reset failed. The link may have expired or already been used. Please request a new reset link.", null)
                );
            }

        } catch (Exception e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "An error occurred while resetting your password", null)
            );
        }
    }

    // ======================== HELPER METHODS ========================

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Return the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
