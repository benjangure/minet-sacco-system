package com.minet.sacco.repository;

import com.minet.sacco.entity.BatchDeletionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchDeletionAuditRepository extends JpaRepository<BatchDeletionAudit, Long> {
}
