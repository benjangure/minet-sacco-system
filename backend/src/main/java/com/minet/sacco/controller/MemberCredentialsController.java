package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.MemberCredential;
import com.minet.sacco.repository.MemberCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member-credentials")
@CrossOrigin
public class MemberCredentialsController {

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private com.minet.sacco.service.MemberCredentialService memberCredentialService;

    /**
     * Repair: create missing credential records for members that have none and normalise names.
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> syncCredentials() {
        int renamed = memberCredentialService.normalizeStoredNames();
        java.util.Map<String, Object> result = memberCredentialService.syncMissingCredentials(null);
        result.put("namesNormalised", renamed);
        return ResponseEntity.ok(ApiResponse.success("Member credentials synchronised", result));
    }

    /**
     * Get all member credentials (admin/treasurer/customer support only)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<List<MemberCredential>>> getAllCredentials() {
        List<MemberCredential> credentials = memberCredentialRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Member credentials retrieved successfully", credentials));
    }

    /**
     * Get credential by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<MemberCredential>> getCredentialById(@PathVariable(name = "id") Long credentialId) {
        return memberCredentialRepository.findById(credentialId)
                .map(credential -> ResponseEntity.ok(ApiResponse.success("Credential found", credential)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Autowired
    private com.minet.sacco.repository.UserRepository userRepository;

    /**
     * Reset a member's password to a fresh temporary ("demo") password.
     * Use when a member cannot sign in or cannot receive the reset email.
     */
    /**
     * Irreversibly remove all stored member passwords (members then sign in through HR only).
     * ADMIN only; requires the confirmation text in the body.
     */
    @PostMapping("/purge-passwords")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> purgePasswords(
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String confirm = body == null ? null : body.get("confirm");
        if (!"DELETE MEMBER PASSWORDS".equals(confirm)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Confirmation text missing. Send {\"confirm\": \"DELETE MEMBER PASSWORDS\"}."));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success("Member passwords removed",
                    memberCredentialService.purgeLocalMemberPasswords()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<PasswordResponse>> resetPassword(
            @PathVariable(name = "id") Long credentialId,
            org.springframework.security.core.Authentication authentication) {
        try {
            MemberCredential credential = memberCredentialRepository.findById(credentialId)
                    .orElseThrow(() -> new RuntimeException("Credential not found"));
            Long performedBy = userRepository.findByUsername(authentication.getName())
                    .map(com.minet.sacco.entity.User::getId).orElse(null);
            String temporary = memberCredentialService.resetToTemporaryPassword(credential.getMemberId(), performedBy);
            PasswordResponse response = new PasswordResponse();
            response.setPassword(temporary);
            return ResponseEntity.ok(ApiResponse.success(
                    "Password reset. The member must set a new password at next login.", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get password for a specific credential (requires authorization)
     * Returns only the temporary password if it hasn't been changed
     */
    @GetMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<PasswordResponse>> getCredentialPassword(@PathVariable(name = "id") Long credentialId) {
        return memberCredentialRepository.findById(credentialId)
                .map(credential -> {
                    PasswordResponse response = new PasswordResponse();
                    // Only return password if it hasn't been changed by the member
                    if (!credential.isPasswordChanged()) {
                        response.setPassword(credential.getPassword());
                        return ResponseEntity.ok(ApiResponse.success("Password retrieved", response));
                    } else {
                        response.setPassword(null);
                        return ResponseEntity.ok(ApiResponse.success("Password has been changed by member", response));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Helper class for password response
     */
    public static class PasswordResponse {
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Search credentials by member name or username
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<List<MemberCredential>>> searchCredentials(@RequestParam String query) {
        List<MemberCredential> credentials = memberCredentialRepository.findByMemberNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                query, query
        );
        return ResponseEntity.ok(ApiResponse.success("Search results", credentials));
    }

    /**
     * Get credentials by member ID
     */
    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<MemberCredential>> getCredentialByMemberId(@PathVariable Long memberId) {
        return memberCredentialRepository.findByMemberId(memberId)
                .map(credential -> ResponseEntity.ok(ApiResponse.success("Credential found", credential)))
                .orElse(ResponseEntity.ok(ApiResponse.success("No credential found for this member", null)));
    }

    /**
     * Get credentials with email not sent (for reminder/follow-up)
     */
    @GetMapping("/pending-email")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberCredential>>> getPendingEmailCredentials() {
        List<MemberCredential> credentials = memberCredentialRepository.findByEmailSentFalse();
        return ResponseEntity.ok(ApiResponse.success("Credentials pending email send", credentials));
    }

    /**
     * Get credentials that haven't been changed by member (still using temporary password)
     */
    @GetMapping("/password-not-changed")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberCredential>>> getPasswordNotChangedCredentials() {
        List<MemberCredential> credentials = memberCredentialRepository.findByPasswordChangedFalse();
        return ResponseEntity.ok(ApiResponse.success("Credentials with unchanged passwords", credentials));
    }
}
