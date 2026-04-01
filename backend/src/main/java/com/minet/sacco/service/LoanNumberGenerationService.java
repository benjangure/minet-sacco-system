package com.minet.sacco.service;

import com.minet.sacco.entity.Loan;
import com.minet.sacco.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class LoanNumberGenerationService {

    @Autowired
    private LoanRepository loanRepository;

    /**
     * Generate a unique loan number with year-specific counter
     * Format: LN-YYYY-NNNNN (e.g., LN-2026-00001)
     * 
     * @param loan the loan to generate number for
     * @return unique loan number
     */
    public String generateLoanNumber(Loan loan) {
        int year = LocalDateTime.now().getYear();
        
        // Count loans disbursed in current year
        long yearCount = loanRepository.countByYearAndDisbursed(year);
        
        // Generate number with year-specific counter
        return String.format("LN-%d-%05d", year, yearCount + 1);
    }

    /**
     * Generate a unique loan number for a specific year
     * Useful for testing or manual generation
     * 
     * @param year the year for the loan number
     * @return unique loan number for that year
     */
    public String generateLoanNumberForYear(int year) {
        long yearCount = loanRepository.countByYearAndDisbursed(year);
        return String.format("LN-%d-%05d", year, yearCount + 1);
    }
}
