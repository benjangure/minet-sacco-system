package com.minet.sacco.repository;

import com.minet.sacco.entity.KycDocumentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KycDocumentAuditRepository extends JpaRepository<KycDocumentAudit, Long> {

    /**
     * Find all audit logs for a specific KYC document
     */
    List<KycDocumentAudit> findByKycDocumentIdOrderByTimestampDesc(Long kycDocumentId);

    /**
     * Find all audit logs for a specific member
     */
    List<KycDocumentAudit> findByMemberIdOrderByTimestampDesc(Long memberId);

    /**
     * Find all audit logs for a specific action
     */
    List<KycDocumentAudit> findByActionOrderByTimestampDesc(String action);

    /**
     * Find all audit logs performed by a specific user
     */
    List<KycDocumentAudit> findByPerformedByIdOrderByTimestampDesc(Long userId);

    /**
     * Find audit logs within a date range
     */
    List<KycDocumentAudit> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate);
}
