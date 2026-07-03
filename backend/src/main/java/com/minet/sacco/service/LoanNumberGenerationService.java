package com.minet.sacco.service;

import com.minet.sacco.entity.Loan;
import com.minet.sacco.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanNumberGenerationService {

    @Autowired
    private LoanRepository loanRepository;

    /**
     * Generate a unique loan number based on max suffix scanning.
     * Format: LN-YYYY-NNNNN (e.g., LN-2026-00001)
     * 
     * Scans all loans with matching prefix, finds the maximum numeric suffix,
     * and returns the next sequential number.
     * This is based on what loan numbers actually exist, not on disbursement status.
     * 
     * @param loan the loan to generate number for
     * @return unique loan number
     */
    public String generateLoanNumber(Loan loan) {
        int year = LocalDateTime.now().getYear();
        return generateLoanNumberForYear(year);
    }

    /**
     * Generate a unique loan number for a specific year using max suffix scanning.
     * Queries all loans, filters where loanNumber starts with "LN-YYYY-",
     * parses the numeric suffix from each, takes the max, and returns prefix + next number.
     * 
     * @param year the year for the loan number
     * @return unique loan number for that year
     */
    public String generateLoanNumberForYear(int year) {
        String prefix = "LN-" + year + "-";
        
        // Query all loans and filter by prefix
        List<Loan> yearLoans = loanRepository.findAll().stream()
            .filter(l -> l.getLoanNumber() != null && l.getLoanNumber().startsWith(prefix))
            .toList();
        
        // Parse numeric suffix and find maximum
        int maxSeq = yearLoans.stream()
            .map(l -> {
                try {
                    return Integer.parseInt(l.getLoanNumber().split("-")[2]);
                } catch (Exception e) {
                    return 0;
                }
            })
            .max(Integer::compare)
            .orElse(0);
        
        return prefix + String.format("%05d", maxSeq + 1);
    }
}
