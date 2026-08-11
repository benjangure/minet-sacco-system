package com.minet.sacco.repository;

import com.minet.sacco.entity.PushSubscription;
import com.minet.sacco.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PushSubscription entity
 * Handles database operations for push notification subscriptions
 */
@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    /**
     * Find all active subscriptions for a specific user
     */
    List<PushSubscription> findByUserAndIsActiveTrue(User user);

    /**
     * Find all active subscriptions for a user by user ID
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.user.id = :userId AND ps.isActive = true")
    List<PushSubscription> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Find a subscription by user and endpoint
     */
    Optional<PushSubscription> findByUserAndEndpoint(User user, String endpoint);

    /**
     * Find a subscription by user ID and endpoint
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.user.id = :userId AND ps.endpoint = :endpoint")
    Optional<PushSubscription> findByUserIdAndEndpoint(@Param("userId") Long userId, @Param("endpoint") String endpoint);

    /**
     * Check if a user has any active subscriptions
     */
    boolean existsByUserAndIsActiveTrue(User user);

    /**
     * Check if a specific subscription exists
     */
    boolean existsByUserAndEndpoint(User user, String endpoint);

    /**
     * Delete all subscriptions for a user
     */
    void deleteByUser(User user);

    /**
     * Delete a specific subscription by user and endpoint
     */
    void deleteByUserAndEndpoint(User user, String endpoint);

    /**
     * Deactivate old subscriptions that haven't been used
     */
    @Modifying
    @Query("UPDATE PushSubscription ps SET ps.isActive = false WHERE ps.lastUsedAt < :threshold")
    int deactivateOldSubscriptions(@Param("threshold") LocalDateTime threshold);

    /**
     * Delete inactive subscriptions older than specified date
     */
    @Modifying
    @Query("DELETE FROM PushSubscription ps WHERE ps.isActive = false AND ps.updatedAt < :threshold")
    int deleteInactiveSubscriptions(@Param("threshold") LocalDateTime threshold);

    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("UPDATE PushSubscription ps SET ps.lastUsedAt = :timestamp WHERE ps.id = :id")
    int updateLastUsed(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Count active subscriptions for a user
     */
    @Query("SELECT COUNT(ps) FROM PushSubscription ps WHERE ps.user.id = :userId AND ps.isActive = true")
    long countActiveByUserId(@Param("userId") Long userId);

    /**
     * Find all active subscriptions (for bulk notifications)
     */
    List<PushSubscription> findByIsActiveTrue();

    /**
     * Find active subscriptions for multiple users
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.user.id IN :userIds AND ps.isActive = true")
    List<PushSubscription> findActiveByUserIds(@Param("userIds") List<Long> userIds);
}
