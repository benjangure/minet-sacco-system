package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for importing historical loan data from the old system.
 * Handles DISBURSED, REPAID, and DEFAULTED loans with full guarantor setup.
 * Bypasses the normal approval workflow - loans are imported directly.
 */
@Service
public class LoanMigrationService {

    @Autowired
    private LoanMigrationItemRepository loanMigrationItemRepository;

    @Autowired
    private BulkBatchRepository bulkBatchRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final List<String> VALID_STATUSES = List.of("DISBURSED", "REPAID", "DEFAULTED");
    private static final List<String> VALID_GUARANTORSHIP_TYPES = List.of("NORMAL", "SELF");

    /**
     * Parse, validate, and process a loan migration file.
     * Returns the batch with per-row validation results.
     */
    @Transactional
    public BulkBatch parseMigrateAndProcess(MultipartFile file, User uploader) throws Exception {
        if (file.isEmpty()) throw new RuntimeException("File is empty");
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new RuntimeException("Only Excel (.xlsx, .xls) files are supported for loan migration");
        }

        List<LoanMigrationItem> items = excelParserService.parseLoanMigration(file);
        if (items.isEmpty()) {
            throw new RuntimeException("No data rows found in the file. Ensure the file has a header row and at least one data row.");
        }

        // Create batch
        BulkBatch batch = new BulkBatch();
        batch.setBatchNumber("BATCH-LMG-" + System.currentTimeMillis());
        batch.setBatchType("LOAN_MIGRATION");
        batch.setFileName(filename);
        batch.setTotalRecords(items.size());
        batch.setTotalAmount(BigDecimal.ZERO);
        batch.setStatus("PROCESSING");
        batch.setUploadedBy(uploader);
        batch.setApprovedBy(uploader);
        batch.setApprovedAt(LocalDateTime.now());
        batch = bulkBatchRepository.save(batch);

        // Save items and validate each one
        int successCount = 0;
        int failedCount = 0;
        BigDecimal totalPrincipal = BigDecimal.ZERO;

        for (LoanMigrationItem item : items) {
            item.setBatch(batch);
            item = loanMigrationItemRepository.save(item);

            List<String> errors = validateItem(item);
            if (!errors.isEmpty()) {
                item.setStatus("FAILED");
                item.setErrorMessage(String.join("; ", errors));
                item.setProcessedAt(LocalDateTime.now());
                loanMigrationItemRepository.save(item);
                failedCount++;
                continue;
            }

            // Process the item
            try {
                processItem(item, uploader);
                item.setStatus("SUCCESS");
                item.setProcessedAt(LocalDateTime.now());
                successCount++;
                if (item.getPrincipalAmount() != null) {
                    totalPrincipal = totalPrincipal.add(item.getPrincipalAmount());
                }
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMessage(e.getMessage());
                item.setProcessedAt(LocalDateTime.now());
                failedCount++;
            }
            loanMigrationItemRepository.save(item);
        }

        // Update batch summary
        batch.setSuccessfulRecords(successCount);
        batch.setFailedRecords(failedCount);
        batch.setTotalAmount(totalPrincipal);
        batch.setProcessedAt(LocalDateTime.now());
        if (failedCount == 0) {
            batch.setStatus("COMPLETED");
        } else if (successCount == 0) {
            batch.setStatus("FAILED");
        } else {
            batch.setStatus("PARTIALLY_COMPLETED");
        }
        batch = bulkBatchRepository.save(batch);

        auditService.logAction(uploader, "LOAN_MIGRATION", "BulkBatch", batch.getId(),
            "Loan migration: " + successCount + " imported, " + failedCount + " failed", null, null);

        return batch;
    }

    /**
     * Validate a single loan migration item. Returns list of error messages.
     */
    private List<String> validateItem(LoanMigrationItem item) {
        List<String> errors = new ArrayList<>();
        int row = item.getRowNumber();

        // Employee ID
        if (item.getEmployeeId() == null || item.getEmployeeId().isBlank()) {
            errors.add("Row " + row + ": Employee ID is required");
        } else if (memberRepository.findByMemberNumber(item.getEmployeeId()).isEmpty()) {
            errors.add("Row " + row + ": Member with Employee ID '" + item.getEmployeeId() + "' not found. Register the member first.");
        }

        // Loan number (if provided)
        if (item.getLoanNumber() != null && !item.getLoanNumber().trim().isEmpty()) {
            String loanNumber = item.getLoanNumber().trim();
            if (loanRepository.findByLoanNumber(loanNumber).isPresent()) {
                errors.add("Row " + row + ": Loan number '" + loanNumber + "' already exists in the system");
            }
        }

        // Loan product
        if (item.getLoanProductName() == null || item.getLoanProductName().isBlank()) {
            errors.add("Row " + row + ": Loan product name is required");
        } else {
            try {
                if (loanProductRepository.findByName(item.getLoanProductName()).isEmpty()) {
                    // Build helpful error with available product names
                    List<String> availableNames = loanProductRepository.findAll().stream()
                        .filter(p -> p.getIsActive() != null && p.getIsActive())
                        .map(p -> "'" + p.getName() + "'")
                        .toList();
                    errors.add("Row " + row + ": Loan product '" + item.getLoanProductName() + "' not found. Available products: " + String.join(", ", availableNames));
                }
            } catch (Exception e) {
                errors.add("Row " + row + ": Loan product '" + item.getLoanProductName() + "' is ambiguous - multiple products with this name exist. Please contact the administrator to resolve duplicate product names.");
            }
        }

        // Principal
        if (item.getPrincipalAmount() == null || item.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Row " + row + ": Principal amount must be greater than 0");
        }

        // Term
        if (item.getTermMonths() == null || item.getTermMonths() <= 0) {
            errors.add("Row " + row + ": Term months must be greater than 0");
        }

        // Disbursement date
        if (item.getDisbursementDate() == null) {
            errors.add("Row " + row + ": Disbursement date is required (format: DD/MM/YYYY)");
        } else if (item.getDisbursementDate().isAfter(java.time.LocalDate.now())) {
            errors.add("Row " + row + ": Disbursement date cannot be in the future");
        }

        // Loan status
        if (item.getLoanStatus() == null || item.getLoanStatus().isBlank()) {
            errors.add("Row " + row + ": Loan status is required (DISBURSED, REPAID, or DEFAULTED)");
        } else if (!VALID_STATUSES.contains(item.getLoanStatus())) {
            errors.add("Row " + row + ": Invalid loan status '" + item.getLoanStatus() + "'. Must be DISBURSED, REPAID, or DEFAULTED");
        }

        // Outstanding balance
        if (item.getOutstandingBalance() == null || item.getOutstandingBalance().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Row " + row + ": Outstanding balance must be 0 or greater");
        } else if ("REPAID".equals(item.getLoanStatus()) && item.getOutstandingBalance().compareTo(BigDecimal.ZERO) != 0) {
            errors.add("Row " + row + ": Outstanding balance must be 0 for REPAID loans");
        } else if (item.getPrincipalAmount() != null && item.getOutstandingBalance().compareTo(item.getPrincipalAmount()) > 0) {
            errors.add("Row " + row + ": Outstanding balance (" + item.getOutstandingBalance() + ") cannot exceed principal (" + item.getPrincipalAmount() + ")");
        }

        // Guarantorship type
        if (item.getGuarantorshipType() == null || item.getGuarantorshipType().isBlank()) {
            errors.add("Row " + row + ": Guarantorship type is required (NORMAL or SELF)");
        } else if (!VALID_GUARANTORSHIP_TYPES.contains(item.getGuarantorshipType())) {
            errors.add("Row " + row + ": Invalid guarantorship type '" + item.getGuarantorshipType() + "'. Must be NORMAL or SELF");
        }

        // If errors so far, skip guarantor validation (member/product may not be found)
        if (!errors.isEmpty()) return errors;

        // Guarantor-specific validation
        if ("NORMAL".equals(item.getGuarantorshipType())) {
            errors.addAll(validateNormalGuarantors(item));
        } else if ("SELF".equals(item.getGuarantorshipType())) {
            // Self-guarantee: no external guarantors should be provided
            if (hasAnyGuarantor(item)) {
                errors.add("Row " + row + ": SELF guarantorship should not have external guarantors. Remove guarantor columns or use NORMAL type.");
            }
        }

        return errors;
    }

    private List<String> validateNormalGuarantors(LoanMigrationItem item) {
        List<String> errors = new ArrayList<>();
        int row = item.getRowNumber();
        String borrowerEmployeeId = item.getEmployeeId();

        // Collect all guarantor pairs
        List<String[]> guarantorPairs = getGuarantorPairs(item);

        if (guarantorPairs.isEmpty()) {
            errors.add("Row " + row + ": NORMAL guarantorship requires at least one guarantor");
            return errors;
        }

        BigDecimal totalPledge = BigDecimal.ZERO;
        Set<String> seenGuarantors = new HashSet<>();

        for (String[] pair : guarantorPairs) {
            String gEmpId = pair[0];
            String gPledgeStr = pair[1];
            BigDecimal gPledge;

            try {
                gPledge = new BigDecimal(gPledgeStr);
            } catch (Exception e) {
                errors.add("Row " + row + ": Invalid pledge amount for guarantor '" + gEmpId + "'");
                continue;
            }

            if (gPledge.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Row " + row + ": Pledge amount for guarantor '" + gEmpId + "' must be greater than 0");
                continue;
            }

            if (gEmpId.equalsIgnoreCase(borrowerEmployeeId)) {
                errors.add("Row " + row + ": Guarantor '" + gEmpId + "' cannot be the same as the borrower. Use SELF type instead.");
                continue;
            }

            if (seenGuarantors.contains(gEmpId.toUpperCase())) {
                errors.add("Row " + row + ": Duplicate guarantor '" + gEmpId + "'");
                continue;
            }
            seenGuarantors.add(gEmpId.toUpperCase());

            if (memberRepository.findByMemberNumber(gEmpId).isEmpty()) {
                errors.add("Row " + row + ": Guarantor with Employee ID '" + gEmpId + "' not found in system");
                continue;
            }

            totalPledge = totalPledge.add(gPledge);
        }

        // Validate pledges sum to principal
        if (errors.isEmpty() && item.getPrincipalAmount() != null) {
            if (totalPledge.compareTo(item.getPrincipalAmount()) != 0) {
                errors.add("Row " + row + ": Guarantor pledges total (" + totalPledge + ") must equal principal amount (" + item.getPrincipalAmount() + ")");
            }
        }

        return errors;
    }

    /**
     * Process a validated loan migration item - create the loan and guarantors.
     */
    @Transactional
    private void processItem(LoanMigrationItem item, User processor) {
        Member borrower = memberRepository.findByMemberNumber(item.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Member not found: " + item.getEmployeeId()));

        LoanProduct product = loanProductRepository.findByName(item.getLoanProductName())
            .orElseThrow(() -> new RuntimeException("Loan product not found: " + item.getLoanProductName()));

        // ALWAYS use the product's configured interest rate - this ensures calculations
        // match exactly what is shown on the Loan Products admin page.
        // The interest rate column in the template is informational only and is ignored.
        BigDecimal interestRate = product.getInterestRate();

        // Calculate loan financials using the same formula as the system (simple interest)
        // Interest = Principal × (Rate/100) × (Term/12)
        // This matches Loan.calculateRepaymentDetails()
        BigDecimal principal = item.getPrincipalAmount();
        Integer termMonths = item.getTermMonths();
        BigDecimal rate = interestRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal timeInYears = new BigDecimal(termMonths).divide(new BigDecimal("12"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal totalInterest = principal.multiply(rate).multiply(timeInYears).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalRepayable = principal.add(totalInterest);
        BigDecimal monthlyRepayment = totalRepayable.divide(new BigDecimal(termMonths), 2, java.math.RoundingMode.HALF_UP);

        // Store calculated values on item for reporting
        item.setTotalInterest(totalInterest);
        item.setTotalRepayable(totalRepayable);
        item.setMonthlyRepayment(monthlyRepayment);

        // Create the loan
        Loan loan = new Loan();
        loan.setMember(borrower);
        loan.setLoanProduct(product);
        loan.setAmount(principal);
        loan.setOriginalPrincipal(principal);
        loan.setOriginalAmount(principal);
        loan.setInterestRate(interestRate);
        loan.setTermMonths(termMonths);
        loan.setMonthlyRepayment(monthlyRepayment);
        loan.setTotalInterest(totalInterest);
        loan.setTotalRepayable(totalRepayable);
        loan.setOutstandingBalance(item.getOutstandingBalance());
        loan.setPurpose(item.getPurpose() != null ? item.getPurpose() : "Migrated loan");
        loan.setApplicationDate(item.getDisbursementDate().atStartOfDay());
        loan.setApprovalDate(item.getDisbursementDate().atStartOfDay());
        loan.setDisbursementDate(item.getDisbursementDate().atStartOfDay());
        loan.setApprovedBy(processor);
        loan.setDisbursedBy(processor);
        loan.setCreatedBy(processor);
        loan.setMigrationStatus("MIGRATED");
        loan.setMemberEligibilityStatus("APPROVED");

        // Set status
        Loan.Status loanStatus = Loan.Status.valueOf(item.getLoanStatus());
        loan.setStatus(loanStatus);

        // Use provided loan number if available, otherwise generate one
        if (item.getLoanNumber() != null && !item.getLoanNumber().trim().isEmpty()) {
            loan.setLoanNumber(item.getLoanNumber().trim());
        } else {
            loan.setLoanNumber(generateLoanNumber());
        }

        loan = loanRepository.save(loan);

        // Create LOAN_DISBURSEMENT transaction for audit trail and transaction history
        // This ensures migrated loans appear in member's transaction history
        Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
            borrower.getId(), Account.AccountType.SAVINGS
        );
        if (savingsAccount.isPresent()) {
            Transaction disbursementTransaction = new Transaction();
            disbursementTransaction.setAccount(savingsAccount.get());
            disbursementTransaction.setTransactionType(Transaction.TransactionType.LOAN_DISBURSEMENT);
            disbursementTransaction.setAmount(principal);
            disbursementTransaction.setDescription("Loan disbursement - " + loan.getLoanNumber() +
                (item.getPurpose() != null ? " (" + item.getPurpose() + ")" : "") + " [MIGRATED]");
            disbursementTransaction.setTransactionDate(item.getDisbursementDate().atStartOfDay());
            transactionRepository.save(disbursementTransaction);
        }

        // Create guarantors
        if ("SELF".equals(item.getGuarantorshipType())) {
            // Self-guarantee: borrower guarantees their own loan
            Guarantor selfGuarantor = new Guarantor();
            selfGuarantor.setLoan(loan);
            selfGuarantor.setMember(borrower);
            selfGuarantor.setSelfGuarantee(true);
            selfGuarantor.setGuaranteeAmount(principal);
            selfGuarantor.setPledgeAmount(loanStatus == Loan.Status.DISBURSED ? principal : BigDecimal.ZERO);
            selfGuarantor.setStatus(loanStatus == Loan.Status.DISBURSED ? Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
            selfGuarantor.setApprovedAt(item.getDisbursementDate().atStartOfDay());
            selfGuarantor.setMigrationStatus("MIGRATED");
            guarantorRepository.save(selfGuarantor);

            // Freeze savings for active self-guaranteed loans
            if (loanStatus == Loan.Status.DISBURSED) {
                freezeSavings(borrower, principal);
            }
        } else {
            // Normal guarantorship: create external guarantors
            List<String[]> guarantorPairs = getGuarantorPairs(item);
            for (String[] pair : guarantorPairs) {
                String gEmpId = pair[0];
                BigDecimal gPledge = new BigDecimal(pair[1]);

                Member guarantorMember = memberRepository.findByMemberNumber(gEmpId)
                    .orElseThrow(() -> new RuntimeException("Guarantor not found: " + gEmpId));

                Guarantor guarantor = new Guarantor();
                guarantor.setLoan(loan);
                guarantor.setMember(guarantorMember);
                guarantor.setSelfGuarantee(false);
                guarantor.setGuaranteeAmount(gPledge);
                // Only freeze pledge for active (DISBURSED) loans
                guarantor.setPledgeAmount(loanStatus == Loan.Status.DISBURSED ? gPledge : BigDecimal.ZERO);
                guarantor.setStatus(loanStatus == Loan.Status.DISBURSED ? Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
                guarantor.setApprovedAt(item.getDisbursementDate().atStartOfDay());
                guarantor.setMigrationStatus("MIGRATED");
                guarantorRepository.save(guarantor);
            }
        }

        item.setLoan(loan);
    }

    /**
     * Freeze savings for a self-guaranteed loan.
     * Updates the frozen_savings field on the member's savings account.
     */
    private void freezeSavings(Member member, BigDecimal amount) {
        accountRepository.findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS)
            .ifPresent(account -> {
                BigDecimal currentFrozen = account.getFrozenSavings() != null ? account.getFrozenSavings() : BigDecimal.ZERO;
                account.setFrozenSavings(currentFrozen.add(amount));
                accountRepository.save(account);
            });
    }

    /**
     * Extract non-null guarantor pairs (employeeId, pledgeAmount) from item.
     */
    private List<String[]> getGuarantorPairs(LoanMigrationItem item) {
        List<String[]> pairs = new ArrayList<>();
        addPairIfPresent(pairs, item.getGuarantor1EmployeeId(), item.getGuarantor1PledgeAmount());
        addPairIfPresent(pairs, item.getGuarantor2EmployeeId(), item.getGuarantor2PledgeAmount());
        addPairIfPresent(pairs, item.getGuarantor3EmployeeId(), item.getGuarantor3PledgeAmount());
        addPairIfPresent(pairs, item.getGuarantor4EmployeeId(), item.getGuarantor4PledgeAmount());
        addPairIfPresent(pairs, item.getGuarantor5EmployeeId(), item.getGuarantor5PledgeAmount());
        addPairIfPresent(pairs, item.getGuarantor6EmployeeId(), item.getGuarantor6PledgeAmount());
        return pairs;
    }

    private void addPairIfPresent(List<String[]> pairs, String empId, BigDecimal pledge) {
        if (empId != null && !empId.isBlank() && pledge != null && pledge.compareTo(BigDecimal.ZERO) > 0) {
            pairs.add(new String[]{empId.trim(), pledge.toPlainString()});
        }
    }

    private boolean hasAnyGuarantor(LoanMigrationItem item) {
        return (item.getGuarantor1EmployeeId() != null && !item.getGuarantor1EmployeeId().isBlank())
            || (item.getGuarantor2EmployeeId() != null && !item.getGuarantor2EmployeeId().isBlank())
            || (item.getGuarantor3EmployeeId() != null && !item.getGuarantor3EmployeeId().isBlank())
            || (item.getGuarantor4EmployeeId() != null && !item.getGuarantor4EmployeeId().isBlank())
            || (item.getGuarantor5EmployeeId() != null && !item.getGuarantor5EmployeeId().isBlank())
            || (item.getGuarantor6EmployeeId() != null && !item.getGuarantor6EmployeeId().isBlank());
    }

    private String generateLoanNumber() {
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String prefix = "LN-" + year + "-";
        List<Loan> yearLoans = loanRepository.findAll().stream()
            .filter(l -> l.getLoanNumber() != null && l.getLoanNumber().startsWith(prefix))
            .toList();
        int maxSeq = yearLoans.stream()
            .map(l -> {
                try { return Integer.parseInt(l.getLoanNumber().split("-")[2]); }
                catch (Exception e) { return 0; }
            })
            .max(Integer::compare).orElse(0);
        return prefix + String.format("%05d", maxSeq + 1);
    }

    public List<LoanMigrationItem> getMigrationItems(Long batchId) {
        return loanMigrationItemRepository.findByBatch_Id(batchId);
    }
}
