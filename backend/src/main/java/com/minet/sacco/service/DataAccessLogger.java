package com.minet.sacco.service;

import com.minet.sacco.entity.AuditLog;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataAccessLogger {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log data access (read operations)
     */
    @Transactional
    public void logDataAccess(User user, String dataType, Long dataId, String description) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction("ACCESS");
        log.setEntityType(dataType);
        log.setEntityId(dataId);
        log.setComments(description);
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());

        auditLogRepository.save(log);
    }

    /**
     * Log member data access
     */
    public void logMemberAccess(User user, Long memberId, String purpose) {
        logDataAccess(user, "MEMBER", memberId, "Member data accessed. Purpose: " + purpose);
    }

    /**
     * Log loan data access
     */
    public void logLoanAccess(User user, Long loanId, String purpose) {
        logDataAccess(user, "LOAN", loanId, "Loan data accessed. Purpose: " + purpose);
    }

    /**
     * Log transaction data access
     */
    public void logTransactionAccess(User user, Long transactionId, String purpose) {
        logDataAccess(user, "TRANSACTION", transactionId, "Transaction data accessed. Purpose: " + purpose);
    }

    /**
     * Log account data access
     */
    public void logAccountAccess(User user, Long accountId, String purpose) {
        logDataAccess(user, "ACCOUNT", accountId, "Account data accessed. Purpose: " + purpose);
    }

    /**
     * Get access logs for a user
     */
    public List<AuditLog> getUserAccessLogs(Long userId) {
        return auditLogRepository.findByUserIdAndActionOrderByTimestampDesc(userId, "ACCESS");
    }

    /**
     * Get access logs for a data entity
     */
    public List<AuditLog> getEntityAccessLogs(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdAndActionOrderByTimestampDesc(entityType, entityId, "ACCESS");
    }

    /**
     * Get access logs within date range
     */
    public List<AuditLog> getAccessLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByActionAndTimestampBetweenOrderByTimestampDesc("ACCESS", startDate, endDate);
    }
}
