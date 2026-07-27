package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.MigrationBatch;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.DataMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin/migration")
public class DataMigrationController {

    @Autowired
    private DataMigrationService dataMigrationService;

    /**
     * Upload migration file (TREASURER - Maker)
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMigrationFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "File is empty", null));
            }

            User user = (User) authentication.getPrincipal();
            MigrationBatch batch = dataMigrationService.parseAndValidateMigration(file, user);

            Map<String, Object> response = Map.of(
                    "batchId", batch.getId(),
                    "totalRecords", batch.getTotalRecords(),
                    "successfulRecords", batch.getSuccessfulRecords(),
                    "failedRecords", batch.getFailedRecords(),
                    "verificationStatus", batch.getVerificationStatus(),
                    "approvalStatus", batch.getApprovalStatus(),
                    "errorMessage", batch.getErrorMessage() != null ? batch.getErrorMessage() : ""
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "File validated successfully", response));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error processing file: " + e.getMessage(), null));
        }
    }

    /**
     * Get pending migrations for approval (ADMIN - Checker)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MigrationBatch>>> getPendingMigrations() {
        try {
            List<MigrationBatch> batches = dataMigrationService.getPendingMigrations();
            return ResponseEntity.ok(new ApiResponse<>(true, "Pending migrations retrieved", batches));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Download batch file (ADMIN - Checker)
     */
    @GetMapping("/{batchId}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> downloadBatchFile(@PathVariable Long batchId) {
        try {
            byte[] fileContent = dataMigrationService.getBatchFileContent(batchId);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=migration_batch_" + batchId + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileContent);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Preview batch data (ADMIN - Checker)
     */
    @GetMapping("/{batchId}/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewBatchData(@PathVariable Long batchId) {
        try {
            Map<String, Object> preview = dataMigrationService.getBatchPreview(batchId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Batch preview retrieved", preview));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Approve migration (ADMIN - Checker)
     */
    @PostMapping("/{batchId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveMigration(
            @PathVariable Long batchId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String notes = request.get("notes");
            User user = (User) authentication.getPrincipal();
            MigrationBatch batch = dataMigrationService.approveBatch(batchId, notes, user);

            Map<String, Object> response = Map.of(
                    "batchId", batch.getId(),
                    "approvalStatus", batch.getApprovalStatus(),
                    "verifiedBy", batch.getVerifiedBy().getUsername(),
                    "verifiedAt", batch.getVerifiedAt()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Migration approved successfully", response));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Reject migration (ADMIN - Checker)
     */
    @PostMapping("/{batchId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectMigration(
            @PathVariable Long batchId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Rejection reason is required", null));
            }

            User user = (User) authentication.getPrincipal();
            MigrationBatch batch = dataMigrationService.rejectBatch(batchId, reason, user);

            Map<String, Object> response = Map.of(
                    "batchId", batch.getId(),
                    "approvalStatus", batch.getApprovalStatus(),
                    "verificationNotes", batch.getVerificationNotes()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Migration rejected", response));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Execute migration (ADMIN - Checker, after approval)
     */
    @PostMapping("/{batchId}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeMigration(
            @PathVariable Long batchId,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            MigrationBatch batch = dataMigrationService.executeMigration(batchId, user);

            Map<String, Object> response = Map.of(
                    "batchId", batch.getId(),
                    "migrationExecuted", batch.getMigrationExecuted(),
                    "successfulRecords", batch.getSuccessfulRecords(),
                    "failedRecords", batch.getFailedRecords(),
                    "executedAt", batch.getExecutedAt()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Migration executed successfully", response));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get migration report
     */
    @GetMapping("/{batchId}/report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMigrationReport(
            @PathVariable Long batchId) {

        try {
            Map<String, Object> report = dataMigrationService.getMigrationReport(batchId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Migration report retrieved", report));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Download migration template (TREASURER - Maker)
     */
    @GetMapping("/template/download")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<?> downloadTemplate() {
        try {
            byte[] fileContent = dataMigrationService.generateTemplate();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=migration_template.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileContent);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
