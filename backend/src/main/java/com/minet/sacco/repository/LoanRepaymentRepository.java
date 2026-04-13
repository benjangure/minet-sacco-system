package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);

    @Query("SELECT COALESCE(SUM(lr.amount), 0) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    BigDecimal getTotalRepaidAmount(@Param("loanId") Long loanId);

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.id = :loanId AND lr.paymentDate >= :startDate AND lr.paymentDate <= :endDate ORDER BY lr.paymentDate DESC")
    List<LoanRepayment> findByLoanIdAndDateRange(@Param("loanId") Long loanId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    Long countRepaymentsByLoanId(@Param("loanId") Long loanId);
}
