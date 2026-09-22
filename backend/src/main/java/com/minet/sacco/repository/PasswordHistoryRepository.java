package com.minet.sacco.repository;

import com.minet.sacco.entity.PasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * Find recent password history for a user (for duplicate checking)
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user.id = :userId ORDER BY ph.changedAt DESC LIMIT :count")
    List<PasswordHistory> findRecentPasswordHistory(@Param("userId") Long userId, @Param("count") int count);

    /**
     * Get all password history for a user
     */
    List<PasswordHistory> findByUserId(Long userId);

    /**
     * Find password history ordered by most recent
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user.id = :userId ORDER BY ph.changedAt DESC")
    List<PasswordHistory> findUserPasswordHistoryOrderByRecent(@Param("userId") Long userId);

    /**
     * Delete old password history (older than specified count per user)
     */
    @Query(nativeQuery = true, value = "DELETE FROM password_history WHERE id IN (" +
        "SELECT id FROM (" +
        "SELECT id FROM password_history WHERE user_id = :userId ORDER BY changed_at DESC LIMIT -1 OFFSET :retentionCount" +
        ") temp)")
    void deleteOldPasswordHistory(@Param("userId") Long userId, @Param("retentionCount") int retentionCount);
}
