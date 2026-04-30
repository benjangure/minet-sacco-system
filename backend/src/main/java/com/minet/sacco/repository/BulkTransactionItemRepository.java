package com.minet.sacco.repository;

import com.minet.sacco.entity.BulkTransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkTransactionItemRepository extends JpaRepository<BulkTransactionItem, Long> {
    List<BulkTransactionItem> findByBatch_Id(Long batchId);
    List<BulkTransactionItem> findByBatch_IdAndStatus(Long batchId, String status);
}
