package com.minet.sacco.service;

import com.minet.sacco.entity.AuditLog;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log an action with all details
     * Uses REQUIRES_NEW to ensure audit log is saved independently
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, String action, String entityType, Long entityId, 
                         String entityDetails, String comments, String status) {
        try {
            System.out.println("DEBUG: logAction called - action=" + action + ", entityType=" + entityType + ", user=" + (user != null ? user.getUsername() : "NULL"));
            
            if (user == null) {
                System.err.println("ERROR: User is null, cannot save audit log");
                return;
            }
            
            AuditLog log = new AuditLog();
            log.setUser(user);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setEntityDetails(entityDetails);
            log.setComments(comments);
            log.setStatus(status != null ? status : "SUCCESS");
            log.setTimestamp(LocalDateTime.now());
            
            // Get request info
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    log.setIpAddress(getClientIpAddress(request));
                    String userAgent = request.getHeader("User-Agent");
                    // Truncate user_agent to fit column size (VARCHAR(100))
                    if (userAgent != null && userAgent.length() > 100) {
                        userAgent = userAgent.substring(0, 100);
                    }
                    log.setUserAgent(userAgent);
                }
            } catch (Exception e) {
                // If not in HTTP context, skip request details
                System.out.println("DEBUG: Could not get request info: " + e.getMessage());
            }
            
            AuditLog saved = auditLogRepository.save(log);
            System.out.println("DEBUG: Audit log saved successfully - ID=" + saved.getId() + ", action=" + action);
        } catch (Exception e) {
            // Log to console but don't fail the calling transaction
            System.err.println("ERROR: Failed to save audit log: " + e.getMessage());
            System.err.println("ERROR: Exception type: " + e.getClass().getName());
            e.printStackTrace();
        }
    }

    /**
     * Log an action with error
     * Uses REQUIRES_NEW to ensure audit log is saved independently
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActionWithError(User user, String action, String entityType, Long entityId,
                                   String entityDetails, String comments, String errorMessage) {
        try {
            AuditLog log = new AuditLog();
            log.setUser(user);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setEntityDetails(entityDetails);
            log.setComments(comments);
            log.setStatus("FAILURE");
            log.setErrorMessage(errorMessage);
            log.setTimestamp(LocalDateTime.now());
            
            // Get request info
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    log.setIpAddress(getClientIpAddress(request));
                    String userAgent = request.getHeader("User-Agent");
                    if (userAgent != null && userAgent.length() > 100) {
                        userAgent = userAgent.substring(0, 100);
                    }
                    log.setUserAgent(userAgent);
                }
            } catch (Exception e) {
                // If not in HTTP context, skip request details
            }
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Log to console but don't fail the calling transaction
            System.err.println("ERROR: Failed to save audit log with error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all audit logs with pagination
     */
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Get audit logs by user
     */
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get audit logs by action
     */
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    /**
     * Get audit logs by entity type
     */
    public Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable);
    }

    /**
     * Get audit logs for a specific entity
     */
    public java.util.List<AuditLog> getAuditLogsForEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get audit logs by date range
     */
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Get audit logs with multiple filters
     */
    public Page<AuditLog> getAuditLogsByFilters(Long userId, String action, String entityType, 
                                                String status, LocalDateTime startDate, 
                                                LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByFilters(userId, action, entityType, status, startDate, endDate, pageable);
    }

    /**
     * Get failed operations
     */
    public Page<AuditLog> getFailedOperations(Pageable pageable) {
        return auditLogRepository.findByStatus("FAILURE", pageable);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
