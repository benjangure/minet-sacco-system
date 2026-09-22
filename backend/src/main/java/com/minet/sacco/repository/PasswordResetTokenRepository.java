package com.minet.sacco.repository;

import com.minet.sacco.entity.PasswordResetToken;
import com.minet.sacco.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a password reset token by its hash (tokens are stored as hash in DB)
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Find all active (unused and not expired) tokens for a user
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId AND t.isUsed = false AND t.expiresAt > :now AND t.resetType = :resetType")
    List<PasswordResetToken> findActiveTokensByUserId(@Param("userId") Long userId, 
                                                      @Param("resetType") PasswordResetToken.ResetType resetType,
                                                      @Param("now") LocalDateTime now);

    /**
     * Find the most recent active token for a user
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId AND t.isUsed = false AND t.expiresAt > :now AND t.resetType = :resetType ORDER BY t.createdAt DESC LIMIT 1")
    Optional<PasswordResetToken> findMostRecentActiveToken(@Param("userId") Long userId,
                                                           @Param("resetType") PasswordResetToken.ResetType resetType,
                                                           @Param("now") LocalDateTime now);

    /**
     * Find all tokens by user
     */
    List<PasswordResetToken> findByUserId(Long userId);

    /**
     * Find tokens that are expired
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.expiresAt < :now")
    List<PasswordResetToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Find active tokens by email address
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.emailAddress = :email AND t.isUsed = false AND t.expiresAt > :now")
    List<PasswordResetToken> findActiveTokensByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    /**
     * Delete expired tokens
     */
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Check if a token exists and is valid for a user
     */
    @Query("SELECT COUNT(t) > 0 FROM PasswordResetToken t WHERE t.user.id = :userId AND t.isUsed = false AND t.expiresAt > :now")
    boolean hasActiveToken(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
