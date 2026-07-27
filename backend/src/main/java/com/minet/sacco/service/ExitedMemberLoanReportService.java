package com.minet.sacco.service;

import com.minet.sacco.dto.ExitedMemberLoanDTO;
import com.minet.sacco.dto.ExitedMemberLoanDTO.ExitedMemberLoanDetail;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.Member;
import com.minet.sacco.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExitedMemberLoanReportService {

    @Autowired
    private LoanRepository loanRepository;

    /**
     * Generates a report of exited members who still have outstanding disbursed loans.
     * These represent potential unresolved financial obligations from members who have left the SACCO.
     * 
     * @return ExitedMemberLoanDTO containing list of exited members with outstanding loans
     */
    public ExitedMemberLoanDTO generateExitedMembersOutstandingLoansReport() {
        // Fetch all disbursed loans
        List<Loan> disbursedLoans = loanRepository.findByStatus(Loan.Status.DISBURSED);

        List<ExitedMemberLoanDetail> results = new ArrayList<>();

        // Filter for exited members with outstanding loans
        for (Loan loan : disbursedLoans) {
            Member member = loan.getMember();
            
            // Check if member is exited and has outstanding balance
            if (member != null && 
                member.getStatus() == Member.Status.EXITED && 
                loan.getOutstandingBalance() != null && 
                loan.getOutstandingBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {

                // Convert exitDate from LocalDateTime to LocalDate
                LocalDate exitDate = member.getExitDate() != null 
                    ? member.getExitDate().toLocalDate() 
                    : null;

                // Convert disbursementDate from LocalDateTime to LocalDate
                LocalDate disbursementDate = loan.getDisbursementDate() != null 
                    ? loan.getDisbursementDate().toLocalDate() 
                    : null;

                ExitedMemberLoanDetail detail = new ExitedMemberLoanDetail(
                    member.getId(),
                    member.getMemberNumber(),
                    member.getFirstName() + " " + (member.getLastName() != null ? member.getLastName() : ""),
                    exitDate,
                    member.getExitReason(),
                    loan.getId(),
                    loan.getLoanNumber(),
                    loan.getOutstandingBalance(),
                    loan.getOriginalAmount(),
                    disbursementDate
                );

                results.add(detail);
            }
        }

        return new ExitedMemberLoanDTO(results);
    }
}
