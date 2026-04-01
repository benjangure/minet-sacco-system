package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanEligibilityRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanEligibilityRulesRepository extends JpaRepository<LoanEligibilityRules, Long> {
}
