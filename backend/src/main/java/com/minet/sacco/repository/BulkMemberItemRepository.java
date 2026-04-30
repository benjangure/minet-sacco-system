package com.minet.sacco.repository;

import com.minet.sacco.entity.BulkMemberItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BulkMemberItemRepository extends JpaRepository<BulkMemberItem, Long> {
    List<BulkMemberItem> findByBatch_Id(Long batchId);
}
