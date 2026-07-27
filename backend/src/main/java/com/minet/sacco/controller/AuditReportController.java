package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.AuditLog;
import com.minet.sacco.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-reports")
@CrossOrigin
public class AuditReportController {

    // Financial actions — operations that involve money or loan status changes
    private static final List<String> FINANCIAL_ACTIONS = Arrays.asList(
        "APPROVE", "REJECT", "DISBURSE", "REPAY", "LOAN_REPAYMENT_APPROVED",
        "LOAN_REPAYMENT_REJECTED", "DEPOSIT", "WITHDRAWAL", "ACTIVATE",
        "GUARANTOR_PLEDGE_REDUCED", "GUARANTOR_DEFAULT_DEBIT"
    );

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Financial Actions tab — loan approvals, disbursements, repayments, deposits, withdrawals.
     * Filtered to actions that involve money or loan status changes.
     */
    @GetMapping("/financial-actions")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getFinancialActions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<AuditLog> allLogs;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            allLogs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
        } else {
            allLogs = auditLogRepository.findAllByOrderByTimestampDesc();
        }

        List<AuditLog> financialLogs = allLogs.stream()
                .filter(l -> FINANCIAL_ACTIONS.contains(l.getAction()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Financial action logs retrieved", financialLogs));
    }

    /**
     * All Activity tab — every audit log entry, with optional date filter.
     */
    @GetMapping("/all-activity")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAllActivity(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<AuditLog> logs;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            logs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc();
        }

        return ResponseEntity.ok(ApiResponse.success("All activity logs retrieved", logs));
    }

    /**
     * Summary tab — totals and breakdowns by action type and by user.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<AuditSummary>> getAuditSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<AuditLog> logs;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            logs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc();
        }

        AuditSummary summary = new AuditSummary();
        summary.totalActions = (long) logs.size();
        summary.totalFinancialActions = logs.stream()
                .filter(l -> FINANCIAL_ACTIONS.contains(l.getAction()))
                .count();
        summary.totalOtherActions = summary.totalActions - summary.totalFinancialActions;
        summary.successCount = logs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        summary.failureCount = logs.stream().filter(l -> "FAILURE".equals(l.getStatus())).count();
        summary.actionsByType = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.actionsByUser = logs.stream()
                .filter(l -> l.getUsername() != null)
                .collect(Collectors.groupingBy(AuditLog::getUsername, Collectors.counting()));

        return ResponseEntity.ok(ApiResponse.success("Audit summary retrieved", summary));
    }

    // Keep old endpoints for backward compatibility
    @GetMapping("/data-access")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getDataAccessLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return getFinancialActions(startDate, endDate);
    }

    @GetMapping("/user-activity")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getUserActivityLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return getAllActivity(startDate, endDate);
    }

    public static class AuditSummary {
        public Long totalActions;
        public Long totalFinancialActions;
        public Long totalOtherActions;
        public Long successCount;
        public Long failureCount;
        public Map<String, Long> actionsByType;
        public Map<String, Long> actionsByUser;

        public Long getTotalActions() { return totalActions; }
        public Long getTotalFinancialActions() { return totalFinancialActions; }
        public Long getTotalOtherActions() { return totalOtherActions; }
        public Long getSuccessCount() { return successCount; }
        public Long getFailureCount() { return failureCount; }
        public Map<String, Long> getActionsByType() { return actionsByType; }
        public Map<String, Long> getActionsByUser() { return actionsByUser; }
    }
}
