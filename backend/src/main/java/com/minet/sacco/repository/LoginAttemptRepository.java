package com.minet.sacco.repository;

import com.minet.sacco.entity.LoginAttempt;
import com.minet.sacco.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Find all login attempts for a user
     */
    List<LoginAttempt> findByUserId(Long userId);

    /**
     * Find failed login attempts for a user within a time window
     */
    @Query("SELECT l FROM LoginAttempt l WHERE l.user.id = :userId AND l.success = false AND l.attemptTime >= :startTime")
    List<LoginAttempt> findFailedAttemptsSince(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * Find failed login attempts by username within a time window
     */
    @Query("SELECT l FROM LoginAttempt l WHERE l.username = :username AND l.success = false AND l.attemptTime >= :startTime")
    List<LoginAttempt> findFailedAttemptsByUsernameSince(@Param("username") String username, @Param("startTime") LocalDateTime startTime);

    /**
     * Count failed attempts for a user in the last specified minutes
     */
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.user.id = :userId AND l.success = false AND l.attemptTime >= :startTime")
    long countFailedAttemptsSince(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * Count failed attempts by username in the last specified minutes
     */
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.username = :username AND l.success = false AND l.attemptTime >= :startTime")
    long countFailedAttemptsByUsernameSince(@Param("username") String username, @Param("startTime") LocalDateTime startTime);

    /**
     * Get most recent login attempt for a user
     */
    @Query("SELECT l FROM LoginAttempt l WHERE l.user.id = :userId ORDER BY l.attemptTime DESC LIMIT 1")
    LoginAttempt findMostRecentAttempt(@Param("userId") Long userId);

    /**
     * Delete old login attempts (older than specified date)
     */
    @Query("DELETE FROM LoginAttempt l WHERE l.attemptTime < :cutoffDate")
    void deleteOldAttempts(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find all attempts for a user with pagination
     */
    @Query("SELECT l FROM LoginAttempt l WHERE l.user.id = :userId ORDER BY l.attemptTime DESC")
    List<LoginAttempt> findRecentAttempts(@Param("userId") Long userId);
}
