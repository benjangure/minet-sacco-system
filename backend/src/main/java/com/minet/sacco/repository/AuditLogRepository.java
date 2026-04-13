package com.minet.sacco.repository;

import com.minet.sacco.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find all audit logs
    Page<AuditLog> findAll(Pageable pageable);

    // Find by user
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    // Find by action
    Page<AuditLog> findByAction(String action, Pageable pageable);

    // Find by entity type
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    // Find by entity ID
    List<AuditLog> findByEntityId(Long entityId);

    // Find by entity type and ID
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    // Find by date range
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate, 
                                    Pageable pageable);

    // Find by action and date range
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByActionAndDateRange(@Param("action") String action,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            Pageable pageable);

    // Find by entity type and date range
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEntityTypeAndDateRange(@Param("entityType") String entityType,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    // Complex query: filter by multiple criteria
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "a.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByFilters(@Param("userId") Long userId,
                                 @Param("action") String action,
                                 @Param("entityType") String entityType,
                                 @Param("status") String status,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate,
                                 Pageable pageable);

    // Find failed operations
    Page<AuditLog> findByStatus(String status, Pageable pageable);

    // Count by action
    long countByAction(String action);

    // Count by entity type
    long countByEntityType(String entityType);

    // Find by action (non-paginated)
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    // Find by user ID (non-paginated)
    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);

    // Find by user ID and action (non-paginated)
    List<AuditLog> findByUserIdAndActionOrderByTimestampDesc(Long userId, String action);

    // Find by entity type, entity ID, and action (non-paginated)
    List<AuditLog> findByEntityTypeAndEntityIdAndActionOrderByTimestampDesc(String entityType, Long entityId, String action);

    // Find by timestamp between (non-paginated)
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate);

    // Find by action and timestamp between (non-paginated)
    List<AuditLog> findByActionAndTimestampBetweenOrderByTimestampDesc(String action, LocalDateTime startDate, LocalDateTime endDate);

    // Find all ordered by timestamp (non-paginated)
    List<AuditLog> findAllByOrderByTimestampDesc();
}
