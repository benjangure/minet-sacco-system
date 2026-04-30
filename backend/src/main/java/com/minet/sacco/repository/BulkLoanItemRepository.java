package com.minet.sacco.repository;

import com.minet.sacco.entity.BulkLoanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BulkLoanItemRepository extends JpaRepository<BulkLoanItem, Long> {
    List<BulkLoanItem> findByBatch_Id(Long batchId);
    List<BulkLoanItem> findByStatus(String status);
}
