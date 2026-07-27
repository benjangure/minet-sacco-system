package com.minet.sacco.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.AccountType;
import java.util.List;
import java.util.Optional;

@Repository
public interface GLAccountRepository extends JpaRepository<GLAccount, Integer> {
  Optional<GLAccount> findByCode(String code);
  
  List<GLAccount> findByAccountTypeOrderByDisplayOrder(AccountType type);
  
  List<GLAccount> findByIsActiveTrueOrderByDisplayOrder();
  
  List<GLAccount> findByAccountTypeAndIsActiveTrue(AccountType type);
  
  List<GLAccount> findByIsActiveTrueAndAccountTypeInOrderByDisplayOrder(List<AccountType> types);
}
