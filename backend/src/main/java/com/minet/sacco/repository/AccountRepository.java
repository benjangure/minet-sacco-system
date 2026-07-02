package com.minet.sacco.repository;

import com.minet.sacco.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByMemberId(Long memberId);

    Optional<Account> findByMemberIdAndAccountType(Long memberId, Account.AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.member.id = :memberId AND a.accountType = 'SAVINGS'")
    List<Account> findSavingsAccountsByMemberId(@Param("memberId") Long memberId);
}