package com.minet.sacco.repository;

import com.minet.sacco.entity.LoanTopUpHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanTopUpHistoryRepository extends JpaRepository<LoanTopUpHistory, Long> {
    
    /**
     * Find all top-up history entries for a specific loan
     * Ordered by most recent first
     */
    List<LoanTopUpHistory> findByLoanIdOrderByTopupDateDesc(Long loanId);
    
    /**
     * Count number of top-ups for a loan
     */
    long countByLoanId(Long loanId);
}
