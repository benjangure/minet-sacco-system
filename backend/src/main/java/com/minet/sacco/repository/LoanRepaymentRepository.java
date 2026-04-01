package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoanId(Long loanId);

    @Query("SELECT SUM(lr.amount) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    BigDecimal getTotalRepaidAmount(Long loanId);

    List<LoanRepayment> findByCreatedById(Long createdById);
}
