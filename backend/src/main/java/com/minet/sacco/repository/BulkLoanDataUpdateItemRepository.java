package com.minet.sacco.repository;

import com.minet.sacco.entity.BulkBatch;
import com.minet.sacco.entity.BulkLoanDataUpdateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkLoanDataUpdateItemRepository extends JpaRepository<BulkLoanDataUpdateItem, Long> {
    List<BulkLoanDataUpdateItem> findByBatch(BulkBatch batch);
    
    List<BulkLoanDataUpdateItem> findByBatchAndStatus(BulkBatch batch, String status);
}
