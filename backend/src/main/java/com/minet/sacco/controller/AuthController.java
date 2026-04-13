package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.AuthRequest;
import com.minet.sacco.dto.AuthResponse;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.security.CustomUserDetailsService;
import com.minet.sacco.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
            
            // Only MEMBER role can login to member portal
            if (user.getRole() != User.Role.MEMBER) {
                System.err.println("ERROR: Non-member user attempting member login: " + authRequest.getUsername());
                throw new Exception("Staff users must use the staff login page. Please use the staff login.");
            }
            
            // Generate token with memberId for member users
            final String jwt = jwtUtil.generateTokenWithMemberId(userDetails, user.getMemberId());
            
            return ResponseEntity.ok(new AuthResponse(jwt));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to generate member JWT token: " + e.getMessage());
            throw new Exception(e.getMessage(), e);
        }
    }

    /**
     * Health check endpoint for APK to verify backend connectivity
     * No authentication required - used by APK settings to test connection
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(new ApiResponse(true, "Backend is healthy", null));
    }
}
