package com.minet.sacco.repository;

import com.minet.sacco.entity.GLManualEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GLManualEntryRepository extends JpaRepository<GLManualEntry, Integer> {
  List<GLManualEntry> findByApprovalStatusOrderByCreatedAtDesc(GLManualEntry.ApprovalStatus status);
  List<GLManualEntry> findByPeriodStatusOrderByCreatedAtDesc(GLManualEntry.PeriodStatus status);
  List<GLManualEntry> findByEntryDateBetweenOrderByEntryDateDesc(LocalDate fromDate, LocalDate toDate);
  List<GLManualEntry> findByGlAccountIdOrderByCreatedAtDesc(Integer glAccountId);
}
