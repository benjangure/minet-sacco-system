package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.MemberCredentialDTO;
import com.minet.sacco.entity.MemberCredential;
import com.minet.sacco.repository.MemberCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/member-credentials")
@CrossOrigin
@PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'CUSTOMER_SUPPORT')")
public class MemberCredentialsController {
    
    @Autowired
    private MemberCredentialRepository memberCredentialRepository;
    
    /**
     * Get all member credentials with password delivery status
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberCredentialDTO>>> getAllCredentials() {
        List<MemberCredential> credentials = memberCredentialRepository.findAll();
        
        List<MemberCredentialDTO> dtos = credentials.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(new ApiResponse<>(true, "Member credentials retrieved successfully", dtos));
    }
    
    /**
     * Get members awaiting password setup
     */
    @GetMapping("/pending-setup")
    public ResponseEntity<ApiResponse<List<MemberCredentialDTO>>> getPendingPasswordSetup() {
        List<MemberCredential> credentials = memberCredentialRepository.findMembersAwaitingPasswordSetup();
        
        List<MemberCredentialDTO> dtos = credentials.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(new ApiResponse<>(true, "Pending password setups retrieved", dtos));
    }
    
    /**
     * Get members who need their credentials delivered (no email sent)
     */
    @GetMapping("/pending-delivery")
    public ResponseEntity<ApiResponse<List<MemberCredentialDTO>>> getPendingDelivery() {
        List<MemberCredential> credentials = memberCredentialRepository.findByEmailSentFalse();
        
        List<MemberCredentialDTO> dtos = credentials.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(new ApiResponse<>(true, "Pending credential deliveries retrieved", dtos));
    }
    
    /**
     * Mark credentials as manually delivered to member
     */
    @PostMapping("/{credentialId}/mark-delivered")
    public ResponseEntity<ApiResponse<String>> markAsDelivered(@PathVariable Long credentialId) {
        MemberCredential credential = memberCredentialRepository.findById(credentialId)
            .orElseThrow(() -> new RuntimeException("Credential record not found"));
        
        credential.setEmailSent(true);
        credential.setEmailSentAt(java.time.LocalDateTime.now());
        memberCredentialRepository.save(credential);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Credential marked as delivered", null));
    }
    
    /**
     * Get statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<CredentialStatistics>> getStatistics() {
        long totalCredentials = memberCredentialRepository.count();
        long emailsSent = memberCredentialRepository.countEmailsSent();
        long passwordsChanged = memberCredentialRepository.countPasswordsChanged();
        long pendingDelivery = memberCredentialRepository.findByEmailSentFalse().size();
        long pendingPasswordSetup = memberCredentialRepository.findMembersAwaitingPasswordSetup().size();
        
        CredentialStatistics stats = new CredentialStatistics(
            totalCredentials, emailsSent, passwordsChanged, pendingDelivery, pendingPasswordSetup
        );
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Statistics retrieved", stats));
    }
    
    private MemberCredentialDTO convertToDTO(MemberCredential credential) {
        return new MemberCredentialDTO(
            credential.getId(),
            credential.getMemberId(),
            credential.getUsername(),
            credential.getMemberName(),
            credential.getEmail(),
            credential.isHasNationalId(),
            credential.isEmailSent(),
            credential.getEmailSentAt(),
            credential.isPasswordChanged(),
            credential.getPasswordChangedAt(),
            credential.getCreatedAt()
        );
    }
    
    // Statistics inner class
    public static class CredentialStatistics {
        private long totalCredentials;
        private long emailsSent;
        private long passwordsChanged;
        private long pendingDelivery;
        private long pendingPasswordSetup;
        
        public CredentialStatistics(long totalCredentials, long emailsSent, long passwordsChanged, 
                                  long pendingDelivery, long pendingPasswordSetup) {
            this.totalCredentials = totalCredentials;
            this.emailsSent = emailsSent;
            this.passwordsChanged = passwordsChanged;
            this.pendingDelivery = pendingDelivery;
            this.pendingPasswordSetup = pendingPasswordSetup;
        }
        
        // Getters
        public long getTotalCredentials() { return totalCredentials; }
        public long getEmailsSent() { return emailsSent; }
        public long getPasswordsChanged() { return passwordsChanged; }
        public long getPendingDelivery() { return pendingDelivery; }
        public long getPendingPasswordSetup() { return pendingPasswordSetup; }
    }
}