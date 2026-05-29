package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanMigrationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanMigrationItemRepository extends JpaRepository<LoanMigrationItem, Long> {
    List<LoanMigrationItem> findByBatch_Id(Long batchId);
}
