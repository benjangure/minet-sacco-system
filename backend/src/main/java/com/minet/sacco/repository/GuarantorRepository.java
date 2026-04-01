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

    List<Guarantor> findByLoanId(Long loanId);

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
     */
    @Query(value = "SELECT COALESCE(SUM(g.pledge_amount), 0) FROM guarantors g " +
           "JOIN loans l ON g.loan_id = l.id " +
           "WHERE g.member_id = :memberId " +
           "AND g.status = 'ACTIVE' " +
           "AND l.status NOT IN ('REPAID', 'REJECTED', 'DEFAULTED')", nativeQuery = true)
    BigDecimal sumActivePledgesByMemberId(@Param("memberId") Long memberId);

    /**
     * Same as above but excluding a specific loan (used when re-validating an existing application).
     */
    @Query(value = "SELECT COALESCE(SUM(g.pledge_amount), 0) FROM guarantors g " +
           "JOIN loans l ON g.loan_id = l.id " +
           "WHERE g.member_id = :memberId " +
           "AND g.loan_id <> :excludeLoanId " +
           "AND g.status = 'ACTIVE' " +
           "AND l.status NOT IN ('REPAID', 'REJECTED', 'DEFAULTED')", nativeQuery = true)
    BigDecimal sumActivePledgesByMemberIdExcludingLoan(@Param("memberId") Long memberId,
                                                       @Param("excludeLoanId") Long excludeLoanId);
}
