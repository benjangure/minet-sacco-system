package com.minet.sacco.repository;

import com.minet.sacco.entity.Guarantor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {

    // Optimized with JOIN FETCH to prevent N+1 queries
    @Query("SELECT g FROM Guarantor g " +
           "LEFT JOIN FETCH g.member " +
           "LEFT JOIN FETCH g.loan " +
           "WHERE g.loan.id = :loanId")
    List<Guarantor> findByLoanIdWithDetails(@Param("loanId") Long loanId);

    List<Guarantor> findByLoanId(Long loanId);

    // Batch version: get all guarantors for multiple loans in one query
    @Query("SELECT g FROM Guarantor g LEFT JOIN FETCH g.member WHERE g.loan.id IN :loanIds ORDER BY g.loan.id")
    List<Guarantor> findByLoanIdIn(@Param("loanIds") List<Long> loanIds);

    List<Guarantor> findByLoan(com.minet.sacco.entity.Loan loan);

    List<Guarantor> findByMemberId(Long memberId);

    List<Guarantor> findByStatus(Guarantor.Status status);

    List<Guarantor> findByLoanIdAndStatus(Long loanId, Guarantor.Status status);

    List<Guarantor> findByMemberIdAndStatus(Long memberId, Guarantor.Status status);

    long countByMemberIdAndStatus(Long memberId, Guarantor.Status status);

    /**
     * Sum of pledged amounts for a member across all active guarantorships.
     * Only counts ACTIVE status (loan has been DISBURSED).
     * PENDING and ACCEPTED pledges don't freeze savings until the loan is actually disbursed.
     * This ensures guarantors can apply for multiple loans before any are disbursed.
     * CRITICAL: Excludes self-guarantees (self_guarantee = false) to avoid double-counting.
     * Self-guarantees are already counted separately in getTrueSavings().
     */
    @Query(value = "SELECT COALESCE(SUM(g.pledge_amount), 0) FROM guarantors g " +
           "JOIN loans l ON g.loan_id = l.id " +
           "WHERE g.member_id = :memberId " +
           "AND g.self_guarantee = false " +
           "AND g.status = 'ACTIVE' " +
           "AND l.status NOT IN ('REPAID', 'REJECTED', 'DEFAULTED')", nativeQuery = true)
    BigDecimal sumActivePledgesByMemberId(@Param("memberId") Long memberId);

    /**
     * Same as above but excluding a specific loan (used when re-validating an existing application).
     * CRITICAL: Excludes self-guarantees (self_guarantee = false) to avoid double-counting.
     */
    @Query(value = "SELECT COALESCE(SUM(g.pledge_amount), 0) FROM guarantors g " +
           "JOIN loans l ON g.loan_id = l.id " +
           "WHERE g.member_id = :memberId " +
           "AND g.loan_id <> :excludeLoanId " +
           "AND g.self_guarantee = false " +
           "AND g.status = 'ACTIVE' " +
           "AND l.status NOT IN ('REPAID', 'REJECTED', 'DEFAULTED')", nativeQuery = true)
    BigDecimal sumActivePledgesByMemberIdExcludingLoan(@Param("memberId") Long memberId,
                                                       @Param("excludeLoanId") Long excludeLoanId);

    // Report-specific queries
    List<Guarantor> findByMemberIdAndSelfGuaranteeIsTrueAndStatus(Long memberId, Guarantor.Status status);

    List<Guarantor> findByMemberIdAndSelfGuaranteeIsFalseAndStatus(Long memberId, Guarantor.Status status);

    List<Guarantor> findByMemberIdAndSelfGuaranteeIsFalse(Long memberId);

    List<Guarantor> findByMemberIdAndSelfGuaranteeIsFalseAndStatusNotIn(Long memberId, List<Guarantor.Status> statuses);

    /**
     * PERFORMANCE OPTIMIZATION: Fetch all guarantors for a member's loans in a single query
     * instead of N+1 queries (one per loan).
     * This is used by EligibilityCalculationService to avoid the N+1 problem.
     */
    @Query("SELECT g FROM Guarantor g WHERE g.loan.member.id = :memberId " +
           "AND g.selfGuarantee = true AND g.loan.status = 'DISBURSED'")
    List<Guarantor> findAllSelfGuaranteesByMemberId(@Param("memberId") Long memberId);

    /**
     * Delete all guarantors for a specific loan.
     * Used during batch rollback to remove all guarantees for a loan being deleted.
     */
    void deleteByLoanId(Long loanId);

    // ============================================================
    // NEXT OF KIN GUARANTOR QUERIES
    // ============================================================

    /**
     * Find all active guarantees where member is PRIMARY guarantor (not NOK)
     */
    @Query("SELECT g FROM Guarantor g " +
           "LEFT JOIN FETCH g.loan l " +
           "LEFT JOIN FETCH l.member " +
           "LEFT JOIN FETCH g.nextOfKinGuarantor nok " +
           "LEFT JOIN FETCH nok.member " +
           "WHERE g.member.id = :memberId " +
           "AND g.isNextOfKin = false " +
           "AND g.status IN ('ACTIVE', 'ACCEPTED', 'PENDING') " +
           "AND l.status NOT IN ('REPAID', 'REJECTED', 'DEFAULTED')")
    List<Guarantor> findActiveGuaranteesByMemberId(@Param("memberId") Long memberId);

    /**
     * Find guarantor by ID with NOK details loaded
     */
    @Query("SELECT g FROM Guarantor g " +
           "LEFT JOIN FETCH g.nextOfKinGuarantor nok " +
           "LEFT JOIN FETCH nok.member " +
           "WHERE g.id = :guarantorId")
    Guarantor findByIdWithNok(@Param("guarantorId") Long guarantorId);

    /**
     * PERFORMANCE OPTIMIZATION for Over-Committed Guarantor Report
     * Fetch all active guarantors with loans in a single query using JOIN FETCH
     */
    @Query("SELECT g FROM Guarantor g " +
           "LEFT JOIN FETCH g.member m " +
           "LEFT JOIN FETCH g.loan l " +
           "LEFT JOIN FETCH l.member borrower " +
           "WHERE g.selfGuarantee = false " +
           "AND g.status = 'ACTIVE' " +
           "ORDER BY m.id")
    List<Guarantor> findAllActiveGuarantorsWithDetails();
}
