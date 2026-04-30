package com.minet.sacco.repository;

import com.minet.sacco.entity.MigrationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MigrationBatchRepository extends JpaRepository<MigrationBatch, Long> {
    List<MigrationBatch> findAllByOrderByBatchDateDesc();
    Optional<MigrationBatch> findFirstByMigrationExecutedTrueOrderByExecutedAtDesc();
    List<MigrationBatch> findByVerificationStatus(String status);
    List<MigrationBatch> findByApprovalStatusAndVerificationStatus(String approvalStatus, String verificationStatus);
}
