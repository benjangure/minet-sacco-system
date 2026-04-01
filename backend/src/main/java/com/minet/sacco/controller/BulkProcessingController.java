package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.BulkBatchDTO;
import com.minet.sacco.entity.BulkBatch;
import com.minet.sacco.entity.BulkLoanItem;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.BulkProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bulk")
@CrossOrigin(origins = "*")
public class BulkProcessingController {

    private static final Logger log = LoggerFactory.getLogger(BulkProcessingController.class);

    @Autowired
    private BulkProcessingService bulkProcessingService;

    @Autowired
    private com.minet.sacco.service.UserService userService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<BulkBatchDTO>> uploadBatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam("batchType") String batchType,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User uploader = getUserFromDetails(userDetails);
            BulkBatch batch = bulkProcessingService.parseAndValidate(file, batchType, uploader);
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Batch uploaded successfully",
                convertToDTO(batch)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Upload error: " + e.getMessage());
            e.getCause();
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                false,
                "Upload failed: " + e.getMessage() + (e.getCause() != null ? " | Cause: " + e.getCause().getMessage() : ""),
                null
            ));
        }
    }

    @GetMapping("/batches")
    @PreAuthorize("hasRole('ROLE_TREASURER') or hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<List<BulkBatchDTO>>> getAllBatches() {
        try {
            List<BulkBatch> batches = bulkProcessingService.getAllBatches();
            List<BulkBatchDTO> dtos = batches.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Batches retrieved successfully", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/batches/{id}")
    @PreAuthorize("hasRole('ROLE_TREASURER') or hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<BulkBatchDTO>> getBatchById(@PathVariable Long id) {
        try {
            BulkBatch batch = bulkProcessingService.getBatchById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Batch retrieved successfully", convertToDTO(batch)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/batches/{id}/items")
    @PreAuthorize("hasRole('ROLE_TREASURER') or hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<?>> getBatchItems(@PathVariable Long id) {
        try {
            BulkBatch batch = bulkProcessingService.getBatchById(id);
            
            Object items;
            switch (batch.getBatchType()) {
                case "MEMBER_REGISTRATION":
                    items = bulkProcessingService.getBatchMemberItems(id);
                    break;
                case "LOAN_APPLICATIONS":
                    items = bulkProcessingService.getBatchLoanItems(id);
                    break;
                case "LOAN_DISBURSEMENTS":
                    items = bulkProcessingService.getBatchDisbursementItems(id);
                    break;
                default:
                    items = bulkProcessingService.getBatchItems(id);
            }
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Batch items retrieved successfully", items));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/loan-items/approved")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<List<?>>> getApprovedLoanItems() {
        try {
            List<BulkLoanItem> items = bulkProcessingService.getApprovedLoanItems();
            return ResponseEntity.ok(new ApiResponse<>(true, "Approved loan items retrieved", items));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/loan-items/bulk-disburse")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDisburseLoanItems(
            @RequestBody List<Long> itemIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User treasurer = getUserFromDetails(userDetails);
            Map<String, Object> result = bulkProcessingService.bulkDisburseLoanItems(itemIds, treasurer);
            return ResponseEntity.ok(new ApiResponse<>(true, "Bulk disbursement completed", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/loan-items/{itemId}/disburse")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disburseLoanItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User treasurer = getUserFromDetails(userDetails);
            Map<String, Object> result = bulkProcessingService.bulkDisburseLoanItems(
                java.util.Arrays.asList(itemId), treasurer);
            return ResponseEntity.ok(new ApiResponse<>(true, "Loan disbursed successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/loan-items/bulk-approve")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkApproveLoanItems(
            @RequestBody List<Long> itemIds,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User approver = getUserFromDetails(userDetails);
            Map<String, Object> result = bulkProcessingService.bulkApproveLoanItems(itemIds, approver);
            return ResponseEntity.ok(new ApiResponse<>(true, "Bulk approval completed", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/loan-items/{itemId}/approve")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveLoanItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User approver = getUserFromDetails(userDetails);
            Map<String, Object> result = bulkProcessingService.approveLoanItem(itemId, approver);
            return ResponseEntity.ok(new ApiResponse<>(true, "Loan item approved", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/loan-items/{itemId}/reject")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectLoanItem(
            @PathVariable Long itemId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User rejector = getUserFromDetails(userDetails);
            Map<String, Object> result = bulkProcessingService.rejectLoanItem(itemId, rejector, reason);
            return ResponseEntity.ok(new ApiResponse<>(true, "Loan item rejected", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/loan-items/{itemId}/validate-guarantors")
    @PreAuthorize("hasAnyRole('ROLE_CREDIT_COMMITTEE', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateBulkLoanGuarantors(@PathVariable Long itemId) {
        try {
            log.debug("=== VALIDATE GUARANTORS ENDPOINT CALLED ===");
            log.debug("Item ID: {}", itemId);
            Map<String, Object> validationResult = bulkProcessingService.validateBulkLoanItemGuarantors(itemId);
            log.debug("Validation result keys: {}", validationResult.keySet());
            log.debug("allGuarantorsEligible in result: {}", validationResult.containsKey("allGuarantorsEligible"));
            log.debug("validationResults in result: {}", validationResult.containsKey("validationResults"));
            if (validationResult.containsKey("validationResults")) {
                List<?> results = (List<?>) validationResult.get("validationResults");
                log.debug("Number of validation results: {}", results.size());
                for (int i = 0; i < results.size(); i++) {
                    log.debug("Result {}: {}", i, results.get(i));
                }
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "Validation completed", validationResult));
        } catch (Exception e) {
            log.error("Error during validation", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/batches/{id}/approve")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE') or hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<BulkBatchDTO>> approveBatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User approver = getUserFromDetails(userDetails);
            BulkBatch batch = bulkProcessingService.getBatchById(id);
            
            System.out.println("DEBUG: Approving batch " + id + " by user " + approver.getUsername() + " with role " + approver.getRole());
            System.out.println("DEBUG: Batch type: " + batch.getBatchType());
            
            // Validate role-based access
            if ("LOAN_APPLICATIONS".equals(batch.getBatchType()) || "LOAN_DISBURSEMENTS".equals(batch.getBatchType())) {
                // Loan batches require CREDIT_COMMITTEE approval
                System.out.println("DEBUG: Loan batch - checking for CREDIT_COMMITTEE");
                if (approver.getRole() != User.Role.CREDIT_COMMITTEE) {
                    System.out.println("DEBUG: REJECTED - User role is " + approver.getRole() + ", not CREDIT_COMMITTEE");
                    return ResponseEntity.badRequest().body(new ApiResponse<>(
                        false,
                        "Only Credit Committee can approve loan batches",
                        null
                    ));
                }
            } else {
                // Other batches (contributions, member registration) require TREASURER approval
                System.out.println("DEBUG: Non-loan batch - checking for TREASURER");
                if (approver.getRole() != User.Role.TREASURER) {
                    System.out.println("DEBUG: REJECTED - User role is " + approver.getRole() + ", not TREASURER");
                    return ResponseEntity.badRequest().body(new ApiResponse<>(
                        false,
                        "Only Treasurer can approve this batch type",
                        null
                    ));
                }
            }
            
            System.out.println("DEBUG: Role check passed, proceeding with approval");
            batch = bulkProcessingService.approveBatch(id, approver);
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Batch approved and processing started",
                convertToDTO(batch)
            ));
        } catch (Exception e) {
            System.out.println("DEBUG: Exception during batch approval: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/batches/{id}/reject")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE') or hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<BulkBatchDTO>> rejectBatch(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User rejector = getUserFromDetails(userDetails);
            BulkBatch batch = bulkProcessingService.getBatchById(id);
            
            // Validate role-based access
            if ("LOAN_APPLICATIONS".equals(batch.getBatchType()) || "LOAN_DISBURSEMENTS".equals(batch.getBatchType())) {
                // Loan batches require CREDIT_COMMITTEE approval
                if (!rejector.getRole().equals("CREDIT_COMMITTEE")) {
                    return ResponseEntity.badRequest().body(new ApiResponse<>(
                        false,
                        "Only Credit Committee can reject loan batches",
                        null
                    ));
                }
            } else {
                // Other batches require TREASURER approval
                if (!rejector.getRole().equals("TREASURER")) {
                    return ResponseEntity.badRequest().body(new ApiResponse<>(
                        false,
                        "Only Treasurer can reject this batch type",
                        null
                    ));
                }
            }
            
            batch = bulkProcessingService.rejectBatch(id, rejector, reason);
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Batch rejected",
                convertToDTO(batch)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    private BulkBatchDTO convertToDTO(BulkBatch batch) {
        BulkBatchDTO dto = new BulkBatchDTO();
        dto.setId(batch.getId());
        dto.setBatchNumber(batch.getBatchNumber());
        dto.setBatchType(batch.getBatchType());
        dto.setFileName(batch.getFileName());
        dto.setTotalRecords(batch.getTotalRecords());
        dto.setSuccessfulRecords(batch.getSuccessfulRecords());
        dto.setFailedRecords(batch.getFailedRecords());
        dto.setTotalAmount(batch.getTotalAmount());
        dto.setStatus(batch.getStatus());
        dto.setUploadedByUsername(batch.getUploadedBy().getUsername());
        dto.setUploadedAt(batch.getUploadedAt());
        if (batch.getApprovedBy() != null) {
            dto.setApprovedByUsername(batch.getApprovedBy().getUsername());
        }
        dto.setApprovedAt(batch.getApprovedAt());
        dto.setProcessedAt(batch.getProcessedAt());
        return dto;
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}




