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
        if (item.getLoanRepaymentPrincipalAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Loan repayment principal amount cannot be negative");
        }
        if (item.getLoanRepaymentInterestAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Loan repayment interest amount cannot be negative");
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
        
        if (item.getLoanRepaymentAmount().compareTo(BigDecimal.ZERO) > 0 ||
            item.getLoanRepaymentPrincipalAmount().compareTo(BigDecimal.ZERO) > 0 ||
            item.getLoanRepaymentInterestAmount().compareTo(BigDecimal.ZERO) > 0) {
            
            // PHASE 3: MANDATORY SPLIT VALIDATION
            // If any repayment-related field is filled, principal and interest are REQUIRED
            if (item.getLoanRepaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Loan repayment is provided - principal and interest are MANDATORY
                if (item.getLoanRepaymentPrincipalAmount() == null || 
                    item.getLoanRepaymentPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Loan Repayment Principal Amount is required and must be greater than zero when Loan Repayment is provided");
                    return errors; // Block entire row on first mandatory field failure
                }
                if (item.getLoanRepaymentInterestAmount() == null || 
                    item.getLoanRepaymentInterestAmount().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Loan Repayment Interest Amount is required when Loan Repayment is provided (can be zero, but field must be filled)");
                    return errors; // Block entire row on mandatory field failure
                }
                
                // Validate principal + interest = total (EXACTLY)
                BigDecimal calculatedTotal = item.getLoanRepaymentPrincipalAmount().add(item.getLoanRepaymentInterestAmount());
                if (calculatedTotal.compareTo(item.getLoanRepaymentAmount()) != 0) {
                    errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Principal (" + 
                             item.getLoanRepaymentPrincipalAmount() + ") + Interest (" + 
                             item.getLoanRepaymentInterestAmount() + ") = " + calculatedTotal + 
                             ", but Loan Repayment total is " + item.getLoanRepaymentAmount() + 
                             ". These must match exactly.");
                    return errors; // Block entire row
                }
            }
            
            // Validate loan if any repayment specified
            if (item.getLoanNumber() == null || item.getLoanNumber().trim().isEmpty()) {
                errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Loan number is required when loan repayment amount is specified");
                return errors; // Block entire row
            } else {
                Optional<Loan> loanOpt = loanRepository.findByLoanNumber(item.getLoanNumber());
                if (loanOpt.isEmpty()) {
                    errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Loan '" + item.getLoanNumber() + "' not found");
                    return errors; // Block entire row
                } else {
                    Loan loan = loanOpt.get();
                    
                    // Validate loan belongs to member
                    if (!loan.getMember().getId().equals(member.getId())) {
                        errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Loan '" + item.getLoanNumber() + "' does not belong to member '" + item.getMemberNumber() + "'");
                        return errors; // Block entire row
                    }
                    
                    // Validate loan is active
                    if (loan.getStatus() != Loan.Status.DISBURSED && loan.getStatus() != Loan.Status.REPAID) {
                        errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Loan is not active (Status: " + loan.getStatus() + ")");
                        return errors; // Block entire row
                    }
                    
                    // Validate repayment amount doesn't exceed outstanding balance (if amount is specified)
                    if (item.getLoanRepaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal repaymentRounded = item.getLoanRepaymentAmount().setScale(2, java.math.RoundingMode.HALF_UP);
                        BigDecimal outstandingRounded = loan.getOutstandingBalance().setScale(2, java.math.RoundingMode.HALF_UP);
                        if (repaymentRounded.compareTo(outstandingRounded) > 0) {
                            errors.add("Row " + rowNumber + " (" + item.getMemberNumber() + "): Repayment amount (KES " + repaymentRounded + 
                                     ") exceeds outstanding balance (KES " + outstandingRounded + ")");
                            return errors; // Block entire row
                        }
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
        
        // ========== MANDATORY FIELDS ==========
        
        // Validate full name (REQUIRED)
        if (item.getFullName() == null || item.getFullName().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Full name is required");
        } else if (item.getFullName().length() > 150) {
            errors.add("Row " + rowNumber + ": Full name must be max 150 characters (current: " + item.getFullName().length() + ")");
        }
        // Validate employee ID (REQUIRED)
        if (item.getEmployeeId() == null || item.getEmployeeId().trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Employee ID is required");
        } else {
            String employeeId = item.getEmployeeId().trim();
            if (employeeId.length() > 50) {
                errors.add("Row " + rowNumber + ": Employee ID must be max 50 characters (current: " + employeeId.length() + ")");
            }
            // NOTE: We no longer reject duplicate employee IDs here.
            // The system now supports UPDATING existing members via bulk upload.
            // If employee ID exists, it will be updated; if not, a new member will be created.
        }
        
        // ========== OPTIONAL FIELDS (Can be edited later) ==========
        
        
        // Validate phone (optional)
        if (item.getPhone() != null && !item.getPhone().trim().isEmpty()) {
            String phone = item.getPhone().trim();
            if (phone.length() < 9 || phone.length() > 15) {
                errors.add("Row " + rowNumber + ": Phone '" + phone + "' must be 9-15 characters (e.g., 0712345678 or +254712345678)");
            }
        }
        
        // Validate email (optional but must be valid if provided)
        if (item.getEmail() != null && !item.getEmail().trim().isEmpty()) {
            String email = item.getEmail().trim();
            if (!isValidEmail(email)) {
                errors.add("Row " + rowNumber + ": Invalid email format '" + email + "' - must contain @ symbol and domain (e.g., john.doe@email.com)");
            } else if (memberRepository.findByEmail(email).isPresent()) {
                errors.add("Row " + rowNumber + ": Email '" + email + "' is already registered in the system");
            }
        }
        
        // Validate national ID (optional but must be unique if provided)
        if (item.getNationalId() != null && !item.getNationalId().trim().isEmpty()) {
            String nationalId = item.getNationalId().trim();
            if (memberRepository.findByNationalId(nationalId).isPresent()) {
                errors.add("Row " + rowNumber + ": National ID '" + nationalId + "' is already registered in the system");
            }
        }
        
        // Validate date of birth (optional but validate if provided)
        if (item.getDateOfBirth() != null) {
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
        
        // Validate department (optional)
        if (item.getDepartment() != null && !item.getDepartment().trim().isEmpty()) {
            if (item.getDepartment().length() > 50) {
                errors.add("Row " + rowNumber + ": Department must be max 50 characters (current: " + item.getDepartment().length() + ")");
            }
        }
        
        // Validate employer (optional)
        if (item.getEmployer() != null && !item.getEmployer().trim().isEmpty()) {
            if (item.getEmployer().length() > 100) {
                errors.add("Row " + rowNumber + ": Employer must be max 100 characters (current: " + item.getEmployer().length() + ")");
            }
        }
        
        // Validate bank (optional)
        if (item.getBank() != null && !item.getBank().trim().isEmpty()) {
            if (item.getBank().length() > 50) {
                errors.add("Row " + rowNumber + ": Bank must be max 50 characters (current: " + item.getBank().length() + ")");
            }
        }
        
        // Validate bank account (optional)
        if (item.getBankAccount() != null && !item.getBankAccount().trim().isEmpty()) {
            if (item.getBankAccount().length() > 50) {
                errors.add("Row " + rowNumber + ": Bank account must be max 50 characters (current: " + item.getBankAccount().length() + ")");
            }
        }
        
        // Validate next of kin (optional but must be valid if provided)
        if (item.getNextOfKin() != null && !item.getNextOfKin().trim().isEmpty()) {
            if (item.getNextOfKin().length() > 100) {
                errors.add("Row " + rowNumber + ": Next of kin must be max 100 characters (current: " + item.getNextOfKin().length() + ")");
            }
        }
        
        // Validate NOK phone (optional but must be valid if provided)
        if (item.getNokPhone() != null && !item.getNokPhone().trim().isEmpty()) {
            String nokPhone = item.getNokPhone().trim();
            if (nokPhone.length() < 9 || nokPhone.length() > 15) {
                errors.add("Row " + rowNumber + ": NOK phone '" + nokPhone + "' must be 9-15 characters");
            }
        }

        // Validate opening savings balance (optional, must be >= 0 if provided)
        if (item.getOpeningSavingsBalance() != null && item.getOpeningSavingsBalance().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Opening savings balance cannot be negative");
        }

        // Validate opening shares balance (optional, must be >= 0 if provided)
        if (item.getOpeningSharesBalance() != null && item.getOpeningSharesBalance().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Row " + rowNumber + ": Opening shares balance cannot be negative");
        }

        // Validate date joined (optional, must not be in the future)
        if (item.getDateJoined() != null && item.getDateJoined().isAfter(LocalDate.now())) {
            errors.add("Row " + rowNumber + ": Date joined cannot be in the future (" + item.getDateJoined() + ")");
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
