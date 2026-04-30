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

    @Query("SELECT COALESCE(SUM(l.totalInterest), 0) FROM Loan l " +
           "WHERE l.status IN ('DISBURSED', 'REPAID') " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    BigDecimal sumInterestIncomeInPeriod(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
           "WHERE l.status = 'DEFAULTED' " +
           "AND l.applicationDate >= :startDate AND l.applicationDate <= :endDate")
    BigDecimal sumLoanLossProvisionsInPeriod(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(l.totalInterest), 0) FROM Loan l " +
           "WHERE l.status = 'DISBURSED' " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    BigDecimal sumInterestIncomeFromDisbursedLoans(@Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(l.totalInterest), 0) FROM Loan l " +
           "WHERE l.status = 'REPAID' " +
           "AND l.disbursementDate >= :startDate AND l.disbursementDate <= :endDate")
    BigDecimal sumInterestIncomeFromRepaidLoans(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(l.totalInterest), 0) FROM Loan l " +
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
           "WHERE l.status = 'DISBURSED' " +
           "AND YEAR(l.disbursementDate) = :year")
    Long countByYearAndDisbursed(@Param("year") int year);

    List<Loan> findByMemberIdAndStatus(Long memberId, Loan.Status status);

    List<Loan> findByMemberIdAndStatusIn(Long memberId, java.util.List<Loan.Status> statuses);
}