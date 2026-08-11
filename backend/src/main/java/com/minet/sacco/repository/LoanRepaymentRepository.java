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

    @Query("SELECT COALESCE(SUM(lr.principalAmount), 0) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    BigDecimal getTotalPrincipalRepaid(@Param("loanId") Long loanId);

    @Query("SELECT COALESCE(SUM(lr.interestAmount), 0) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    BigDecimal getTotalInterestCollected(@Param("loanId") Long loanId);

    // Batch version: get interest collected for ALL loans in one query
    @Query("SELECT lr.loan.id, COALESCE(SUM(lr.interestAmount), 0) FROM LoanRepayment lr GROUP BY lr.loan.id")
    List<Object[]> getTotalInterestCollectedAllLoans();

    // Batch version: get interest collected for a specific set of loan IDs
    @Query("SELECT lr.loan.id, COALESCE(SUM(lr.interestAmount), 0) FROM LoanRepayment lr WHERE lr.loan.id IN :loanIds GROUP BY lr.loan.id")
    List<Object[]> getTotalInterestCollectedForLoans(@Param("loanIds") List<Long> loanIds);

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.id = :loanId AND lr.paymentDate >= :startDate AND lr.paymentDate <= :endDate ORDER BY lr.paymentDate DESC")
    List<LoanRepayment> findByLoanIdAndDateRange(@Param("loanId") Long loanId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.loan.id = :loanId")
    Long countRepaymentsByLoanId(@Param("loanId") Long loanId);

    @Query("SELECT lr FROM LoanRepayment lr WHERE lr.loan.member.id = :memberId ORDER BY lr.paymentDate DESC")
    List<LoanRepayment> findByMemberIdOrderByPaymentDateDesc(@Param("memberId") Long memberId);
}
