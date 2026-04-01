package com.minet.sacco.service;

import com.minet.sacco.entity.BulkTransactionItem;
import com.minet.sacco.entity.BulkMemberItem;
import com.minet.sacco.entity.BulkLoanItem;
import com.minet.sacco.entity.BulkDisbursementItem;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.Member;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BulkValidationService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepository loanRepository;

    public List<String> validateBatch(List<BulkTransactionItem> items) {
        List<String> errors = new ArrayList<>();
        Set<String> memberNumbers = new HashSet<>();
        
        for (BulkTransactionItem item : items) {
            // Check for duplicate member numbers in batch
            if (memberNumbers.contains(item.getMemberNumber())) {
                errors.add("Row " + item.getRowNumber() + ": Duplicate member number in batch");
            }
            memberNumbers.add(item.getMemberNumber());
            
            // Validate individual item
            errors.addAll(validateItem(item));
        }
        
        return errors;
    }
    
    public List<String> validateItem(BulkTransactionItem item) {
        List<String> errors = new ArrayList<>();
        int rowNumber = item.getRowNumber();
        
        // Validate member number is provided
        if (item.getMemberNumber() == null || item.getMemberNumber().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Member number is required");
            return errors;
        }
        
        // Validate member exists
        Optional<Member> memberOpt = memberRepository.findByMemberNumber(item.getMemberNumber());
        if (memberOpt.isEmpty()) {
            errors.add("Row " + rowNumber + ": Member '" + item.getMemberNumber() + "' not found");
            return errors;
        }
        
        Member member = memberOpt.get();
        item.setMember(member);
        
        // Validate member is active
        if (member.getStatus() != Member.Status.ACTIVE) {
            errors.add("Row " + rowNumber + ": Member is not active (Status: " + member.getStatus() + ")");
        }
        
        // Validate member has not exited
        if (member.getStatus() == Member.Status.EXITED) {
            errors.add("Row " + rowNumber + ": Member has exited the SACCO and cannot perform transactions");
        }
        
        // Validate amounts are non-negative
        if (item.getSavingsAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Savings amount cannot be negative");
        }
        if (item.getSharesAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Shares amount cannot be negative");
        }
        if (item.getLoanRepaymentAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Loan repayment amount cannot be negative");
        }
        if (item.getBenevolentFundAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Benevolent fund amount cannot be negative");
        }
        if (item.getDevelopmentFundAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Development fund amount cannot be negative");
        }
        if (item.getSchoolFeesAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": School fees amount cannot be negative");
        }
        if (item.getHolidayFundAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Holiday fund amount cannot be negative");
        }
        if (item.getEmergencyFundAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Emergency fund amount cannot be negative");
        }
        
        // Validate at least one amount is provided
        if (item.getSavingsAmount().compareTo(BigDecimal.ZERO) == 0 &&
            item.getSharesAmount().compareTo(BigDecimal.ZERO) == 0 &&
            item.getLoanRepaymentAmount().compareTo(BigDecimal.ZERO) == 0 &&
            item.getBenevolentFundAmount().compareTo(BigDecimal.ZERO) == 0 &&
            item.getDevelopmentFundAmount().compareTo(BigDecimal.ZERO) == 0 &&
            item.getSchoolFeesAmount().compareTo(BigDecimal.ZERO) == 0 &&
            item.getHolidayFundAmount().compareTo(BigDecimal.ZERO) == 0 &&
            item.getEmergencyFundAmount().compareTo(BigDecimal.ZERO) == 0) {
            errors.add("Row " + rowNumber + ": At least one amount must be greater than zero");
        }
        
        // Validate loan if repayment specified
        if (item.getLoanRepaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (item.getLoanNumber() == null || item.getLoanNumber().trim().isEmpty()) {
                errors.add("Row " + rowNumber + ": Loan number is required when loan repayment amount is specified");
            } else {
                Optional<Loan> loanOpt = loanRepository.findByLoanNumber(item.getLoanNumber());
                if (loanOpt.isEmpty()) {
                    errors.add("Row " + rowNumber + ": Loan '" + item.getLoanNumber() + "' not found");
                } else {
                    Loan loan = loanOpt.get();
                    
                    // Validate loan belongs to member
                    if (!loan.getMember().getId().equals(member.getId())) {
                        errors.add("Row " + rowNumber + ": Loan '" + item.getLoanNumber() + "' does not belong to member '" + item.getMemberNumber() + "'");
                    }
                    
                    // Validate loan is active
                    if (loan.getStatus() != Loan.Status.APPROVED && loan.getStatus() != Loan.Status.DISBURSED) {
                        errors.add("Row " + rowNumber + ": Loan is not active (Status: " + loan.getStatus() + ")");
                    }
                    
                    // Validate repayment amount doesn't exceed outstanding balance
                    if (item.getLoanRepaymentAmount().compareTo(loan.getOutstandingBalance()) > 0) {
                        errors.add("Row " + rowNumber + ": Repayment amount (" + item.getLoanRepaymentAmount() + 
                                 ") exceeds outstanding balance (" + loan.getOutstandingBalance() + ")");
                    }
                    
                    item.setLoan(loan);
                }
            }
        }
        
        // Validate reasonable amounts (max 1M per transaction)
        BigDecimal maxAmount = new BigDecimal("1000000");
        if (item.getSavingsAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": Savings amount exceeds maximum allowed (KES 1,000,000)");
        }
        if (item.getSharesAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": Shares amount exceeds maximum allowed (KES 1,000,000)");
        }
        if (item.getLoanRepaymentAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": Loan repayment amount exceeds maximum allowed (KES 1,000,000)");
        }
        if (item.getBenevolentFundAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": Benevolent fund amount exceeds maximum allowed (KES 1,000,000)");
        }
        if (item.getDevelopmentFundAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": Development fund amount exceeds maximum allowed (KES 1,000,000)");
        }
        if (item.getSchoolFeesAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": School fees amount exceeds maximum allowed (KES 1,000,000)");
        }
        if (item.getHolidayFundAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": Holiday fund amount exceeds maximum allowed (KES 1,000,000)");
        }
        if (item.getEmergencyFundAmount().compareTo(maxAmount) > 0) {
            errors.add("Row " + rowNumber + ": Emergency fund amount exceeds maximum allowed (KES 1,000,000)");
        }
        
        return errors;
    }

    public List<String> validateMemberBatch(List<BulkMemberItem> items) {
        List<String> errors = new ArrayList<>();
        Set<String> emails = new HashSet<>();
        Set<String> nationalIds = new HashSet<>();
        Set<String> employeeIds = new HashSet<>();
        
        for (BulkMemberItem item : items) {
            // Check for duplicate emails in batch
            if (item.getEmail() != null && !item.getEmail().trim().isEmpty()) {
                if (emails.contains(item.getEmail())) {
                    errors.add("Row " + item.getRowNumber() + ": Email '" + item.getEmail() + "' appears multiple times in this batch");
                }
                emails.add(item.getEmail());
            }
            
            // Check for duplicate national IDs in batch
            if (item.getNationalId() != null && !item.getNationalId().trim().isEmpty()) {
                if (nationalIds.contains(item.getNationalId())) {
                    errors.add("Row " + item.getRowNumber() + ": National ID '" + item.getNationalId() + "' appears multiple times in this batch");
                }
                nationalIds.add(item.getNationalId());
            }

            // Check for duplicate employee IDs in batch
            if (item.getEmployeeId() != null && !item.getEmployeeId().trim().isEmpty()) {
                if (employeeIds.contains(item.getEmployeeId())) {
                    errors.add("Row " + item.getRowNumber() + ": Employee ID '" + item.getEmployeeId() + "' appears multiple times in this batch");
                }
                employeeIds.add(item.getEmployeeId());
            }
            
            // Validate individual item
            errors.addAll(validateMemberItem(item));
        }
        
        return errors;
    }
    
    public List<String> validateMemberItem(BulkMemberItem item) {
        List<String> errors = new ArrayList<>();
        int rowNumber = item.getRowNumber();
        
        // Validate first name
        if (item.getFirstName() == null || item.getFirstName().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": First name is required");
        } else if (item.getFirstName().length() > 50) {
            errors.add("Row " + rowNumber + ": First name must be max 50 characters (current: " + item.getFirstName().length() + ")");
        }
        
        // Validate last name
        if (item.getLastName() == null || item.getLastName().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Last name is required");
        } else if (item.getLastName().length() > 50) {
            errors.add("Row " + rowNumber + ": Last name must be max 50 characters (current: " + item.getLastName().length() + ")");
        }
        
        // Validate email
        if (item.getEmail() == null || item.getEmail().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Email is required");
        } else {
            String email = item.getEmail().trim();
            if (!isValidEmail(email)) {
                errors.add("Row " + rowNumber + ": Invalid email format '" + email + "' - must contain @ symbol and domain (e.g., john.doe@email.com)");
            } else if (memberRepository.findByEmail(email).isPresent()) {
                errors.add("Row " + rowNumber + ": Email '" + email + "' is already registered in the system");
            }
        }
        
        // Validate phone
        if (item.getPhone() == null || item.getPhone().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Phone is required");
        } else {
            String phone = item.getPhone().trim();
            if (phone.length() < 9 || phone.length() > 15) {
                errors.add("Row " + rowNumber + ": Phone '" + phone + "' must be 9-15 characters (e.g., 0712345678 or +254712345678)");
            }
        }
        
        // Validate national ID
        if (item.getNationalId() == null || item.getNationalId().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": National ID is required");
        } else {
            String nationalId = item.getNationalId().trim();
            if (memberRepository.findByNationalId(nationalId).isPresent()) {
                errors.add("Row " + rowNumber + ": National ID '" + nationalId + "' is already registered in the system");
            }
        }
        
        // Validate date of birth
        if (item.getDateOfBirth() == null) {
            errors.add("Row " + rowNumber + ": Date of birth is required. Accepted formats: YYYY-MM-DD (e.g., 1990-01-15), DD/MM/YYYY (e.g., 15/01/1990), or MM/DD/YYYY (e.g., 01/15/1990). Make sure the cell is not empty and contains a valid date.");
        } else {
            LocalDate today = LocalDate.now();
            LocalDate minDate = today.minusYears(18);
            if (item.getDateOfBirth().isAfter(minDate)) {
                errors.add("Row " + rowNumber + ": Member must be at least 18 years old (Date of birth: " + item.getDateOfBirth() + ")");
            }
            // Validate reasonable date (not in future, not too old)
            if (item.getDateOfBirth().isAfter(today)) {
                errors.add("Row " + rowNumber + ": Date of birth cannot be in the future (Date: " + item.getDateOfBirth() + ")");
            }
            if (item.getDateOfBirth().isBefore(today.minusYears(120))) {
                errors.add("Row " + rowNumber + ": Date of birth seems invalid - member would be over 120 years old (Date: " + item.getDateOfBirth() + ")");
            }
        }
        
        // Validate department
        if (item.getDepartment() == null || item.getDepartment().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Department is required");
        } else if (item.getDepartment().length() > 50) {
            errors.add("Row " + rowNumber + ": Department must be max 50 characters (current: " + item.getDepartment().length() + ")");
        }
        
        // Validate employee ID
        if (item.getEmployeeId() == null || item.getEmployeeId().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Employee ID is required");
        } else {
            String employeeId = item.getEmployeeId().trim();
            if (employeeId.length() > 50) {
                errors.add("Row " + rowNumber + ": Employee ID must be max 50 characters (current: " + employeeId.length() + ")");
            } else if (memberRepository.existsByEmployeeId(employeeId)) {
                errors.add("Row " + rowNumber + ": Employee ID '" + employeeId + "' is already registered in the system");
            }
        }
        
        // Validate employer
        if (item.getEmployer() == null || item.getEmployer().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Employer is required");
        } else if (item.getEmployer().length() > 100) {
            errors.add("Row " + rowNumber + ": Employer must be max 100 characters (current: " + item.getEmployer().length() + ")");
        }
        
        // Validate bank
        if (item.getBank() == null || item.getBank().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Bank is required");
        } else if (item.getBank().length() > 50) {
            errors.add("Row " + rowNumber + ": Bank must be max 50 characters (current: " + item.getBank().length() + ")");
        }
        
        // Validate bank account
        if (item.getBankAccount() == null || item.getBankAccount().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Bank account is required");
        } else if (item.getBankAccount().length() > 50) {
            errors.add("Row " + rowNumber + ": Bank account must be max 50 characters (current: " + item.getBankAccount().length() + ")");
        }
        
        // Validate next of kin
        if (item.getNextOfKin() == null || item.getNextOfKin().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Next of kin is required");
        } else if (item.getNextOfKin().length() > 100) {
            errors.add("Row " + rowNumber + ": Next of kin must be max 100 characters (current: " + item.getNextOfKin().length() + ")");
        }
        
        // Validate NOK phone
        if (item.getNokPhone() == null || item.getNokPhone().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": NOK phone is required");
        } else {
            String nokPhone = item.getNokPhone().trim();
            if (nokPhone.length() < 9 || nokPhone.length() > 15) {
                errors.add("Row " + rowNumber + ": NOK phone '" + nokPhone + "' must be 9-15 characters");
            }
        }
        
        return errors;
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        email = email.trim();
        // Basic email validation: must have @ and at least one dot after @
        if (!email.contains("@")) {
            return false;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false; // Multiple @ symbols
        }
        String localPart = parts[0];
        String domainPart = parts[1];
        
        // Local part must not be empty
        if (localPart.isEmpty()) {
            return false;
        }
        
        // Domain must contain at least one dot and have valid structure
        if (!domainPart.contains(".") || domainPart.startsWith(".") || domainPart.endsWith(".")) {
            return false;
        }
        
        // Domain must have at least 2 characters after the last dot (e.g., .com, .co.ke)
        String[] domainParts = domainPart.split("\\.");
        if (domainParts.length < 2 || domainParts[domainParts.length - 1].length() < 2) {
            return false;
        }
        
        return true;
    }

    public List<String> validateLoanBatch(List<BulkLoanItem> items) {
        List<String> errors = new ArrayList<>();
        Set<String> memberNumbers = new HashSet<>();
        
        for (BulkLoanItem item : items) {
            // Check for duplicate member numbers in batch
            if (memberNumbers.contains(item.getMemberNumber())) {
                errors.add("Row " + item.getRowNumber() + ": Duplicate member number in batch");
            }
            memberNumbers.add(item.getMemberNumber());
            
            // Validate individual item
            errors.addAll(validateLoanItem(item));
        }
        
        return errors;
    }
    
    public List<String> validateLoanItem(BulkLoanItem item) {
        List<String> errors = new ArrayList<>();
        int rowNumber = item.getRowNumber();
        
        // Validate member number
        if (item.getMemberNumber() == null || item.getMemberNumber().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Member number is required");
            return errors;
        }
        
        // Validate member exists
        Optional<Member> memberOpt = memberRepository.findByMemberNumber(item.getMemberNumber());
        if (memberOpt.isEmpty()) {
            errors.add("Row " + rowNumber + ": Member '" + item.getMemberNumber() + "' not found");
            return errors;
        }
        
        Member member = memberOpt.get();
        item.setMember(member);
        
        // Validate member is active
        if (member.getStatus() != Member.Status.ACTIVE) {
            errors.add("Row " + rowNumber + ": Member is not active (Status: " + member.getStatus() + ")");
        }
        
        // Validate member has not exited
        if (member.getStatus() == Member.Status.EXITED) {
            errors.add("Row " + rowNumber + ": Member has exited the SACCO and cannot apply for loans");
        }
        
        // Validate loan product name
        if (item.getLoanProductName() == null || item.getLoanProductName().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Loan product name is required");
        }
        
        // Validate amount
        if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Row " + rowNumber + ": Loan amount must be greater than zero");
        } else if (item.getAmount().compareTo(new BigDecimal("10000000")) > 0) {
            errors.add("Row " + rowNumber + ": Loan amount exceeds maximum allowed (KES 10,000,000)");
        }
        
        // Validate purpose
        if (item.getPurpose() == null || item.getPurpose().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Loan purpose is required");
        }
        
        // Validate guarantors
        List<String> guarantorNumbers = new ArrayList<>();
        if (item.getGuarantor1() != null && !item.getGuarantor1().trim().isEmpty()) {
            guarantorNumbers.add(item.getGuarantor1());
        }
        if (item.getGuarantor2() != null && !item.getGuarantor2().trim().isEmpty()) {
            guarantorNumbers.add(item.getGuarantor2());
        }
        
        if (guarantorNumbers.isEmpty()) {
            errors.add("Row " + rowNumber + ": At least one guarantor is required");
        } else {
            for (String guarantorNumber : guarantorNumbers) {
                Optional<Member> guarantorOpt = memberRepository.findByMemberNumber(guarantorNumber);
                if (guarantorOpt.isEmpty()) {
                    errors.add("Row " + rowNumber + ": Guarantor '" + guarantorNumber + "' not found");
                } else {
                    Member guarantor = guarantorOpt.get();
                    if (guarantor.getStatus() != Member.Status.ACTIVE) {
                        errors.add("Row " + rowNumber + ": Guarantor '" + guarantorNumber + "' is not active");
                    }
                    if (guarantor.getStatus() == Member.Status.EXITED) {
                        errors.add("Row " + rowNumber + ": Guarantor '" + guarantorNumber + "' has exited");
                    }
                    if (guarantor.getId().equals(member.getId())) {
                        errors.add("Row " + rowNumber + ": Member cannot be their own guarantor");
                    }
                }
            }
        }
        
        return errors;
    }

    public List<String> validateDisbursementBatch(List<BulkDisbursementItem> items) {
        List<String> errors = new ArrayList<>();
        Set<String> loanNumbers = new HashSet<>();
        
        for (BulkDisbursementItem item : items) {
            // Check for duplicate loan numbers in batch
            if (loanNumbers.contains(item.getLoanNumber())) {
                errors.add("Row " + item.getRowNumber() + ": Duplicate loan number in batch");
            }
            loanNumbers.add(item.getLoanNumber());
            
            // Validate individual item
            errors.addAll(validateDisbursementItem(item));
        }
        
        return errors;
    }
    
    public List<String> validateDisbursementItem(BulkDisbursementItem item) {
        List<String> errors = new ArrayList<>();
        int rowNumber = item.getRowNumber();
        
        // Validate loan number
        if (item.getLoanNumber() == null || item.getLoanNumber().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Loan number is required");
            return errors;
        }
        
        // Validate loan exists
        Optional<Loan> loanOpt = loanRepository.findByLoanNumber(item.getLoanNumber());
        if (loanOpt.isEmpty()) {
            errors.add("Row " + rowNumber + ": Loan '" + item.getLoanNumber() + "' not found");
            return errors;
        }
        
        Loan loan = loanOpt.get();
        item.setLoan(loan);
        
        // Validate loan is approved
        if (loan.getStatus() != Loan.Status.APPROVED) {
            errors.add("Row " + rowNumber + ": Loan must be APPROVED before disbursement (Current status: " + loan.getStatus() + ")");
        }
        
        // Validate disbursement amount
        if (item.getDisbursementAmount() == null || item.getDisbursementAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Row " + rowNumber + ": Disbursement amount must be greater than zero");
        } else if (item.getDisbursementAmount().compareTo(loan.getAmount()) > 0) {
            errors.add("Row " + rowNumber + ": Disbursement amount cannot exceed loan amount (KES " + loan.getAmount() + ")");
        }
        
        // Validate disbursement account
        if (item.getDisbursementAccount() == null || item.getDisbursementAccount().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Disbursement account is required (SAVINGS, SHARES, etc.)");
        }
        
        return errors;
    }
}
