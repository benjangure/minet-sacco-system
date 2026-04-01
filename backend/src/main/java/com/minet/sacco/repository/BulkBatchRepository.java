package com.minet.sacco.repository;

import com.minet.sacco.entity.BulkBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkBatchRepository extends JpaRepository<BulkBatch, Long> {
    List<BulkBatch> findByStatus(String status);
    List<BulkBatch> findByUploadedById(Long uploadedById);
    Optional<BulkBatch> findByBatchNumber(String batchNumber);
    List<BulkBatch> findByBatchType(String batchType);
    List<BulkBatch> findByBatchNumberStartingWith(String prefix);
}
