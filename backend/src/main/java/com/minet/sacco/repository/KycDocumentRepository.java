package com.minet.sacco.repository;

import com.minet.sacco.entity.KycDocument;
import com.minet.sacco.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    /**
     * Find all KYC documents for a specific member
     */
    List<KycDocument> findByMemberId(Long memberId);

    /**
     * Find a specific document type for a member
     */
    Optional<KycDocument> findByMemberIdAndDocumentType(Long memberId, KycDocument.DocumentType documentType);

    /**
     * Find all documents with a specific verification status
     */
    List<KycDocument> findByVerificationStatus(KycDocument.VerificationStatus status);

    /**
     * Find all documents for a member with a specific verification status
     */
    List<KycDocument> findByMemberIdAndVerificationStatus(Long memberId, KycDocument.VerificationStatus status);

    /**
     * Check if all required KYC documents are uploaded for a member
     */
    @Query("SELECT COUNT(DISTINCT kd.documentType) FROM KycDocument kd WHERE kd.member.id = :memberId")
    long countDistinctDocumentTypesByMemberId(@Param("memberId") Long memberId);

    /**
     * Find all documents uploaded by a specific user
     */
    List<KycDocument> findByUploadedById(Long userId);

    /**
     * Find all documents verified by a specific user
     */
    List<KycDocument> findByVerifiedById(Long userId);

    /**
     * Check if member has all required documents uploaded
     */
    @Query("SELECT COUNT(kd) FROM KycDocument kd WHERE kd.member.id = :memberId AND kd.verificationStatus != 'REJECTED'")
    long countValidDocumentsByMemberId(@Param("memberId") Long memberId);

    /**
     * Find members with incomplete KYC
     */
    @Query("SELECT DISTINCT kd.member FROM KycDocument kd WHERE kd.verificationStatus = 'PENDING' ORDER BY kd.uploadDate DESC")
    List<Member> findMembersWithPendingDocuments();

    /**
     * Find all documents pending verification
     */
    @Query("SELECT kd FROM KycDocument kd WHERE kd.verificationStatus = 'PENDING' ORDER BY kd.uploadDate ASC")
    List<KycDocument> findAllPendingDocuments();
}
