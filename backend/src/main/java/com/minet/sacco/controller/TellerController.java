package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.DepositRequestDTO;
import com.minet.sacco.entity.DepositRequest;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.DepositRequestService;
import com.minet.sacco.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teller")
@CrossOrigin
public class TellerController {

    @Autowired
    private DepositRequestService depositRequestService;

    @Autowired
    private UserService userService;

    /**
     * Get all pending deposit requests
     */
    @GetMapping("/deposit-requests/pending")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<?> getPendingDepositRequests() {
        try {
            List<DepositRequest> requests = depositRequestService.getPendingRequests();
            
            List<DepositRequestDTO> dtos = requests.stream()
                .map(r -> new DepositRequestDTO(
                    r.getId(),
                    r.getMember().getId(),
                    r.getMember().getMemberNumber(),
                    r.getMember().getFirstName() + " " + r.getMember().getLastName(),
                    r.getAccount().getId(),
                    r.getAccount().getAccountType().toString(),
                    r.getClaimedAmount(),
                    r.getConfirmedAmount(),
                    r.getDescription(),
                    r.getReceiptFileName(),
                    r.getStatus().toString(),
                    r.getApprovalNotes(),
                    r.getCreatedAt(),
                    r.getApprovedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Pending deposit requests retrieved", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get deposit request details
     */
    @GetMapping("/deposit-requests/{requestId}")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<?> getDepositRequest(@PathVariable Long requestId) {
        try {
            Optional<DepositRequest> requestOpt = depositRequestService.getDepositRequest(requestId);
            
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            DepositRequest r = requestOpt.get();
            DepositRequestDTO dto = new DepositRequestDTO(
                r.getId(),
                r.getMember().getId(),
                r.getMember().getMemberNumber(),
                r.getMember().getFirstName() + " " + r.getMember().getLastName(),
                r.getAccount().getId(),
                r.getAccount().getAccountType().toString(),
                r.getClaimedAmount(),
                r.getConfirmedAmount(),
                r.getDescription(),
                r.getReceiptFileName(),
                r.getStatus().toString(),
                r.getApprovalNotes(),
                r.getCreatedAt(),
                r.getApprovedAt()
            );
            
            return ResponseEntity.ok(ApiResponse.success("Deposit request retrieved", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Teller approves deposit request with confirmed amount
     */
    @PostMapping("/deposit-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<?> approveDepositRequest(
            @PathVariable Long requestId,
            @RequestParam BigDecimal confirmedAmount,
            @RequestParam(required = false) String approvalNotes,
            @RequestParam(required = false) String tellerMessage,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            DepositRequest request = depositRequestService.approveDepositRequest(
                requestId, confirmedAmount, approvalNotes != null ? approvalNotes : "", 
                tellerMessage != null ? tellerMessage : "", user
            );
            
            // Post-transaction notifications and audit logging (happens after transaction commits)
            depositRequestService.postApprovalNotificationsAndAudit(request, user, tellerMessage, 
                approvalNotes != null ? approvalNotes : "", confirmedAmount);
            
            return ResponseEntity.ok(ApiResponse.success("Deposit request approved successfully", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Teller rejects deposit request
     */
    @PostMapping("/deposit-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<?> rejectDepositRequest(
            @PathVariable Long requestId,
            @RequestParam String rejectionReason,
            @RequestParam(required = false) String tellerMessage,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            DepositRequest request = depositRequestService.rejectDepositRequest(
                requestId, rejectionReason, tellerMessage != null ? tellerMessage : "", user
            );
            
            // Post-transaction notifications and audit logging (happens after transaction commits)
            depositRequestService.postRejectionNotificationsAndAudit(request, user, tellerMessage, rejectionReason);
            
            return ResponseEntity.ok(ApiResponse.success("Deposit request rejected successfully", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Download deposit request receipt
     */
    @GetMapping("/deposit-requests/{requestId}/receipt/download")
    @PreAuthorize("hasRole('ROLE_TELLER')")
    public ResponseEntity<?> downloadReceipt(@PathVariable Long requestId) {
        try {
            Optional<DepositRequest> requestOpt = depositRequestService.getDepositRequest(requestId);
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            DepositRequest request = requestOpt.get();
            byte[] fileContent = depositRequestService.downloadReceipt(requestId);
            String filename = request.getReceiptFileName();
            
            // Determine content type based on file extension
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".pdf")) {
                    contentType = MediaType.APPLICATION_PDF;
                } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
                    contentType = MediaType.IMAGE_JPEG;
                } else if (lowerFilename.endsWith(".png")) {
                    contentType = MediaType.IMAGE_PNG;
                } else if (lowerFilename.endsWith(".txt")) {
                    contentType = MediaType.TEXT_PLAIN;
                } else if (lowerFilename.endsWith(".doc") || lowerFilename.endsWith(".docx")) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM;
                }
            }
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(contentType)
                    .body(fileContent);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("success", false, "message", "Failed to download receipt: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }
}
