package com.minet.sacco.repository;

import com.minet.sacco.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Optimized queries with JOIN FETCH
    @Query("SELECT a FROM Account a " +
           "LEFT JOIN FETCH a.member " +
           "WHERE a.member.id = :memberId")
    List<Account> findByMemberIdWithDetails(@Param("memberId") Long memberId);

    @Query("SELECT a FROM Account a " +
           "LEFT JOIN FETCH a.member")
    List<Account> findAllWithDetails();

    List<Account> findByMemberId(Long memberId);

    Optional<Account> findByMemberIdAndAccountType(Long memberId, Account.AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.member.id = :memberId AND a.accountType = 'SAVINGS'")
    List<Account> findSavingsAccountsByMemberId(@Param("memberId") Long memberId);

    /**
     * Sum of account balances as of a specific date, computed from transaction history.
     * This gives a point-in-time balance by summing all deposits and subtracting withdrawals
     * up to the asOfDate. Used for date-aware balance sheet liability calculations.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType IN ('DEPOSIT','LOAN_DISBURSEMENT') THEN t.amount " +
           "WHEN t.transactionType IN ('WITHDRAWAL','LOAN_REPAYMENT') THEN -t.amount ELSE 0 END), 0) " +
           "FROM Transaction t JOIN t.account a " +
           "WHERE a.accountType = :accountType AND t.transactionDate <= :asOfDate")
    BigDecimal sumAccountBalanceAsOf(@Param("accountType") Account.AccountType accountType,
                                     @Param("asOfDate") java.time.LocalDateTime asOfDate);
}