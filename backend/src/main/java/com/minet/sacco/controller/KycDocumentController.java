package com.minet.sacco.controller;

import com.minet.sacco.dto.KycDocumentDTO;
import com.minet.sacco.dto.MemberKycStatusDTO;
import com.minet.sacco.entity.KycDocument;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.KycDocumentService;
import com.minet.sacco.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc-documents")
@CrossOrigin(origins = "*", maxAge = 3600)
public class KycDocumentController {

    @Autowired
    private KycDocumentService kycDocumentService;

    @Autowired
    private UserService userService;

    /**
     * Upload a KYC document (Customer Service role)
     */
    @PostMapping("/upload/{memberId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long memberId,
            @RequestParam("documentType") KycDocument.DocumentType documentType,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User uploadedBy = getUserFromDetails(userDetails);
            KycDocumentDTO document = kycDocumentService.uploadDocument(memberId, documentType, file, uploadedBy.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document uploaded successfully");
            response.put("data", document);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to upload document: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get KYC status for a member
     */
    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER_SUPPORT', 'ROLE_TELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getMemberKycStatus(@PathVariable Long memberId) {
        try {
            MemberKycStatusDTO status = kycDocumentService.getMemberKycStatus(memberId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get all documents uploaded by the current user (Customer Support role)
     */
    @GetMapping("/my-uploads")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<?> getMyUploadedDocuments(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = getUserFromDetails(userDetails);
            List<KycDocumentDTO> documents = kycDocumentService.getDocumentsUploadedByUser(currentUser.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", documents.size());
            response.put("data", documents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch uploaded documents"));
        }
    }

    /**
     * Get all pending documents for verification (Teller role)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getPendingDocuments() {
        try {
            List<KycDocumentDTO> documents = kycDocumentService.getPendingDocuments();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", documents.size());
            response.put("data", documents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch pending documents"));
        }
    }

    /**
     * Get members with incomplete KYC
     */
    @GetMapping("/incomplete-members")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getMembersWithIncompleteKyc() {
        try {
            List<MemberKycStatusDTO> members = kycDocumentService.getMembersWithIncompleteKyc();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", members.size());
            response.put("data", members);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch members with incomplete KYC"));
        }
    }

    /**
     * Verify a KYC document (Teller role)
     */
    @PutMapping("/{documentId}/verify")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> verifyDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User verifiedBy = getUserFromDetails(userDetails);
            KycDocumentDTO document = kycDocumentService.verifyDocument(documentId, verifiedBy.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document verified successfully");
            response.put("data", document);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Reject a KYC document (Teller role)
     */
    @PutMapping("/{documentId}/reject")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> rejectDocument(
            @PathVariable Long documentId,
            @RequestParam("reason") String rejectionReason,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User rejectedBy = getUserFromDetails(userDetails);
            KycDocumentDTO document = kycDocumentService.rejectDocument(documentId, rejectionReason, rejectedBy.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document rejected successfully");
            response.put("data", document);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Bulk approve members for activation (Teller role)
     */
    @PostMapping("/bulk-approve")
    @PreAuthorize("hasAnyRole('ROLE_TELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> bulkApproveMembersForActivation(
            @RequestBody List<Long> memberIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User approvedBy = getUserFromDetails(userDetails);
            List<Long> approvedMembers = kycDocumentService.bulkApproveMembersForActivation(memberIds, approvedBy.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", approvedMembers.size() + " members approved for activation");
            response.put("approvedCount", approvedMembers.size());
            response.put("approvedMemberIds", approvedMembers);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Download a KYC document
     */
    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER_SUPPORT', 'ROLE_TELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> downloadDocument(@PathVariable Long documentId) {
        try {
            byte[] fileContent = kycDocumentService.downloadDocument(documentId);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document.pdf\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to download document"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Delete a KYC document
     */
    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User deletedBy = getUserFromDetails(userDetails);
            kycDocumentService.deleteDocument(documentId, deletedBy.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to delete document"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Helper method to extract user from UserDetails
    private User getUserFromDetails(UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}




