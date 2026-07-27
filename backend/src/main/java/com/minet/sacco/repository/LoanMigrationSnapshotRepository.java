package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanMigrationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanMigrationSnapshotRepository extends JpaRepository<LoanMigrationSnapshot, Long> {
    Optional<LoanMigrationSnapshot> findByLoanId(Long loanId);
}
