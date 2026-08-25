package com.minet.sacco.repository;

import com.minet.sacco.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Optimized queries with JOIN FETCH to prevent N+1 queries
    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.member " +
           "LEFT JOIN FETCH l.loanProduct " +
           "WHERE l.member.id = :memberId")
    List<Loan> findByMemberIdWithDetails(@Param("memberId") Long memberId);

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.member " +
           "LEFT JOIN FETCH l.loanProduct " +
           "WHERE l.status = :status")
    List<Loan> findByStatusWithDetails(@Param("status") Loan.Status status);

    @Query("SELECT DISTINCT l FROM Loan l " +
           "LEFT JOIN FETCH l.member " +
           "LEFT JOIN FETCH l.loanProduct " +
           "ORDER BY l.applicationDate DESC")
    List<Loan> findAllWithDetails();

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByStatus(Loan.Status status);

    List<Loan> findByCreatedById(Long createdById);

    List<Loan> findByApprovedById(Long approvedById);

    Optional<Loan> findByLoanNumber(String loanNumber);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('DISBURSED', 'REPAID') " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    List<Loan> findDisbursedOrRepaidLoansInPeriod(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT l FROM Loan l WHERE l.status = 'DEFAULTED' " +
           "AND l.applicationDate >= :startDate AND l.applicationDate <= :endDate")
    List<Loan> findDefaultedLoansInPeriod(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(" +
           "COALESCE(l.interestCollected, 0) + " +
           "COALESCE((SELECT SUM(lr.interestAmount) FROM LoanRepayment lr WHERE lr.loan.id = l.id), 0)), 0) " +
           "FROM Loan l " +
           "WHERE l.status IN ('DISBURSED', 'REPAID') " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    BigDecimal sumInterestIncomeInPeriod(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
           "WHERE l.status = 'DEFAULTED' " +
           "AND l.applicationDate >= :startDate AND l.applicationDate <= :endDate")
    BigDecimal sumLoanLossProvisionsInPeriod(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(" +
           "COALESCE(l.interestCollected, 0) + " +
           "COALESCE((SELECT SUM(lr.interestAmount) FROM LoanRepayment lr WHERE lr.loan.id = l.id), 0)), 0) " +
           "FROM Loan l " +
           "WHERE l.status = 'DISBURSED' " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    BigDecimal sumInterestIncomeFromDisbursedLoans(@Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(" +
           "COALESCE(l.interestCollected, 0) + " +
           "COALESCE((SELECT SUM(lr.interestAmount) FROM LoanRepayment lr WHERE lr.loan.id = l.id), 0)), 0) " +
           "FROM Loan l " +
           "WHERE l.status = 'REPAID' " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    BigDecimal sumInterestIncomeFromRepaidLoans(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(" +
           "COALESCE(l.interestCollected, 0) + " +
           "COALESCE((SELECT SUM(lr.interestAmount) FROM LoanRepayment lr WHERE lr.loan.id = l.id), 0)), 0) " +
           "FROM Loan l " +
           "WHERE l.status IN ('DISBURSED', 'REPAID') " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    BigDecimal sumInterestIncomeFromDisbursedAndRepaidLoans(@Param("startDate") LocalDateTime startDate, 
                                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(l) FROM Loan l " +
           "WHERE l.status = 'DEFAULTED' " +
           "AND l.applicationDate >= :startDate AND l.applicationDate <= :endDate")
    Long countDefaultedLoansInPeriod(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(l) FROM Loan l " +
           "WHERE (l.status = 'DISBURSED' OR l.status = 'REPAID') " +
           "AND YEAR(l.disbursementDate) = :year")
    Long countByYearAndDisbursed(@Param("year") int year);

    boolean existsByLoanNumberAndIdNot(String loanNumber, Long loanId);

    List<Loan> findByMemberIdAndStatus(Long memberId, Loan.Status status);

    List<Loan> findByMemberIdAndStatusIn(Long memberId, java.util.List<Loan.Status> statuses);

    List<Loan> findByMemberIdAndLoanProductId(Long memberId, Long loanProductId);

    /**
     * Sum of outstanding balances for loans disbursed ON OR BEFORE a specific date.
     * Used for date-aware balance sheet asset calculations.
     */
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
           "WHERE l.status IN ('DISBURSED') " +
           "AND l.disbursementDate <= :asOfDate")
    BigDecimal sumOutstandingBalanceAsOf(@Param("asOfDate") LocalDateTime asOfDate);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
           "WHERE l.status = 'DISBURSED' " +
           "AND l.loanProduct.id = :loanProductId " +
           "AND l.disbursementDate <= :asOfDate")
    BigDecimal sumOutstandingBalanceAsOfByProduct(@Param("asOfDate") LocalDateTime asOfDate,
                                                   @Param("loanProductId") Integer loanProductId);
}