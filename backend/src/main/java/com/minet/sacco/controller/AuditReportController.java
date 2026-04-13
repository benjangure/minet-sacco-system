package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.AuditLog;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.AuditLogRepository;
import com.minet.sacco.service.DataAccessLogger;
import com.minet.sacco.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-reports")
@CrossOrigin
public class AuditReportController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DataAccessLogger dataAccessLogger;

    @Autowired
    private UserService userService;

    @GetMapping("/data-access")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getDataAccessLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<AuditLog> logs;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            logs = dataAccessLogger.getAccessLogsByDateRange(start, end);
        } else {
            logs = auditLogRepository.findByActionOrderByTimestampDesc("ACCESS");
        }

        return ResponseEntity.ok(ApiResponse.success("Data access logs retrieved", logs));
    }

    @GetMapping("/user-activity")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getUserActivityLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<AuditLog> logs;
        if (userId != null) {
            logs = auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
        } else if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            logs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc();
        }

        return ResponseEntity.ok(ApiResponse.success("User activity logs retrieved", logs));
    }

    @GetMapping("/member-access/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getMemberAccessLogs(
            @PathVariable Long memberId) {

        List<AuditLog> logs = dataAccessLogger.getEntityAccessLogs("MEMBER", memberId);
        return ResponseEntity.ok(ApiResponse.success("Member access logs retrieved", logs));
    }

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
        summary.totalAccess = logs.stream().filter(l -> "ACCESS".equals(l.getAction())).count();
        summary.totalModifications = logs.stream().filter(l -> !"ACCESS".equals(l.getAction())).count();
        summary.actionsByType = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.actionsByUser = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getUser().getUsername(), Collectors.counting()));

        return ResponseEntity.ok(ApiResponse.success("Audit summary retrieved", summary));
    }

    public static class AuditSummary {
        public Long totalActions;
        public Long totalAccess;
        public Long totalModifications;
        public Map<String, Long> actionsByType;
        public Map<String, Long> actionsByUser;

        public Long getTotalActions() { return totalActions; }
        public Long getTotalAccess() { return totalAccess; }
        public Long getTotalModifications() { return totalModifications; }
        public Map<String, Long> getActionsByType() { return actionsByType; }
        public Map<String, Long> getActionsByUser() { return actionsByUser; }
    }
}
