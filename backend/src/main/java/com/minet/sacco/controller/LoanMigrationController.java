package com.minet.sacco.controller;

import com.minet.sacco.entity.BulkBatch;
import com.minet.sacco.entity.LoanMigrationItem;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.service.LoanMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loan-migration")
public class LoanMigrationController {

    @Autowired
    private LoanMigrationService loanMigrationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Upload and process a loan migration Excel file.
     * Returns batch summary with per-row results.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadLoanMigration(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User uploader = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            BulkBatch batch = loanMigrationService.parseMigrateAndProcess(file, uploader);

            Map<String, Object> response = new HashMap<>();
            response.put("batchId", batch.getId());
            response.put("batchNumber", batch.getBatchNumber());
            response.put("totalRecords", batch.getTotalRecords());
            response.put("successfulRecords", batch.getSuccessfulRecords());
            response.put("failedRecords", batch.getFailedRecords());
            response.put("status", batch.getStatus());
            response.put("totalPrincipal", batch.getTotalAmount());

            String message;
            if ("COMPLETED".equals(batch.getStatus())) {
                message = "All " + batch.getSuccessfulRecords() + " loans imported successfully.";
            } else if ("PARTIALLY_COMPLETED".equals(batch.getStatus())) {
                message = batch.getSuccessfulRecords() + " loans imported, " + batch.getFailedRecords() + " failed. Check item details for errors.";
            } else {
                message = "Import failed. " + batch.getFailedRecords() + " errors found. No loans were imported.";
            }
            response.put("message", message);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get detailed results for a migration batch (per-row status and errors).
     */
    @GetMapping("/batch/{batchId}/items")
    public ResponseEntity<List<LoanMigrationItem>> getBatchItems(@PathVariable Long batchId) {
        return ResponseEntity.ok(loanMigrationService.getMigrationItems(batchId));
    }

    /**
     * Download the loan migration Excel template with proper column order.
     */
    @GetMapping("/template/download")
    public ResponseEntity<?> downloadLoanMigrationTemplate() {
        try {
            byte[] fileContent = loanMigrationService.generateLoanMigrationTemplate();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=loan_migration_template.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(fileContent);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate template: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
