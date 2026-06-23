package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.AuthRequest;
import com.minet.sacco.dto.AuthResponse;
import com.minet.sacco.dto.SetupPasswordRequest;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.repository.MemberCredentialRepository;
import com.minet.sacco.security.CustomUserDetailsService;
import com.minet.sacco.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            System.out.println("DEBUG: Attempting authentication for user: " + authRequest.getUsername());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            System.out.println("DEBUG: Authentication successful");
        } catch (BadCredentialsException e) {
            System.err.println("ERROR: Bad credentials for user: " + authRequest.getUsername());
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
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            
            if (user.getRole() == User.Role.MEMBER) {
                System.err.println("ERROR: Member user attempting staff login: " + authRequest.getUsername());
                throw new Exception("Members must use the member portal. Please use the member login page.");
            }
            
            System.out.println("DEBUG: User details loaded, generating JWT token");
            final String jwt = jwtUtil.generateToken(userDetails);
            System.out.println("DEBUG: JWT token generated successfully");

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
    public ResponseEntity<?> memberLogin(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            System.out.println("DEBUG: Member login attempt for: " + authRequest.getUsername());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            System.out.println("DEBUG: Member authentication successful");
        } catch (BadCredentialsException e) {
            System.err.println("ERROR: Invalid member credentials for: " + authRequest.getUsername());
            throw new Exception("Invalid username or password", e);
        } catch (Exception e) {
            System.err.println("ERROR: Member authentication failed: " + e.getMessage());
            throw e;
        }

        try {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
            
            // Get the user to extract memberId and verify role
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
            
            System.out.println("DEBUG: User found - id=" + user.getId() + ", role=" + user.getRole() + ", firstLogin=" + user.isFirstLogin());
            
            // Only MEMBER role can login to member portal
            if (user.getRole() != User.Role.MEMBER) {
                System.err.println("ERROR: Non-member user attempting member login: " + authRequest.getUsername());
                throw new Exception("Staff users must use the staff login page. Please use the staff login.");
            }
            
            System.out.println("DEBUG: Generating JWT token with memberId=" + user.getMemberId() + ", firstLogin=" + user.isFirstLogin());
            
            // Generate token with memberId and first-login status for member users
            final String jwt = jwtUtil.generateTokenWithMemberId(userDetails, user.getMemberId(), user.isFirstLogin());
            
            System.out.println("DEBUG: JWT token generated successfully, first login flag: " + user.isFirstLogin());
            
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
    @GetMapping("/auth/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Backend is healthy", null));
    }
}
