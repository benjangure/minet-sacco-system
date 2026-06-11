package com.minet.sacco.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.minet.sacco.entity.GLAccountAudit;
import com.minet.sacco.entity.GLAccountAudit.ChangeType;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GLAccountAuditRepository extends JpaRepository<GLAccountAudit, Integer> {
  List<GLAccountAudit> findByGlAccountIdOrderByChangedAtDesc(Integer glAccountId);
  
  List<GLAccountAudit> findByChangedAtBetweenOrderByChangedAtDesc(LocalDateTime start, LocalDateTime end);
  
  List<GLAccountAudit> findByChangeTypeAndChangedAtBetween(ChangeType changeType, LocalDateTime start, LocalDateTime end);
  
  List<GLAccountAudit> findByGlAccountIdAndChangeTypeOrderByChangedAtDesc(Integer glAccountId, ChangeType changeType);
}
