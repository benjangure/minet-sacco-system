package com.minet.sacco.repository;

import com.minet.sacco.entity.BulkDisbursementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkDisbursementItemRepository extends JpaRepository<BulkDisbursementItem, Long> {
    List<BulkDisbursementItem> findByBatch_Id(Long batchId);
    List<BulkDisbursementItem> findByStatus(String status);
}
