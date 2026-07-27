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

    @Autowired
    private LoanGuarantorUpdateService loanGuarantorUpdateService;

    @Autowired
    private LoanNumberGenerationService loanNumberGenerationService;

    @Autowired
    private LoanMigrationSnapshotRepository loanMigrationSnapshotRepository;

    private static final List<String> VALID_STATUSES = List.of("DISBURSED", "REPAID", "DEFAULTED");
    private static final List<String> VALID_GUARANTORSHIP_TYPES = List.of("NORMAL", "SELF", "PARTIAL");

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

        // First, validate ALL items without saving them
        Map<LoanMigrationItem, List<String>> validationResults = new HashMap<>();
        for (LoanMigrationItem item : items) {
            List<String> errors = validateItem(item);
            validationResults.put(item, errors);
        }

        // Now process validated items
        int successCount = 0;
        int failedCount = 0;
        BigDecimal totalPrincipal = BigDecimal.ZERO;

        for (LoanMigrationItem item : items) {
            item.setBatch(batch);
            
            List<String> errors = validationResults.get(item);
            if (errors != null && !errors.isEmpty()) {
                // For failed items, set error fields and save them so the treasurer
                // can see exactly which rows failed and why.
                item.setStatus("FAILED");
                item.setErrorMessage(String.join("; ", errors));
                item.setProcessedAt(LocalDateTime.now());
                // Always save failed items now that disbursement_date is nullable (V131) —
                // every validation failure must be visible to the treasurer with its
                // error message, regardless of which field caused the failure.
                loanMigrationItemRepository.save(item);
                failedCount++;
                continue;
            }
            
            // Save validated item
            item = loanMigrationItemRepository.save(item);

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
     * Supports two modes:
     * - CREATE mode: Loan Number is blank/null → creates new loan
     * - UPDATE mode: Loan Number is provided → updates existing loan
     */
    private List<String> validateItem(LoanMigrationItem item) {
        List<String> errors = new ArrayList<>();
        int row = item.getRowNumber();

        // Detect mode based on Loan Number presence
        boolean isUpdateMode = item.getLoanNumber() != null && !item.getLoanNumber().trim().isEmpty();

        if (isUpdateMode) {
            // ============ UPDATE MODE VALIDATION ============
            validateUpdateMode(item, errors);
        } else {
            // ============ CREATE MODE VALIDATION ============
            validateCreateMode(item, errors);
        }

        return errors;
    }

    /**
     * Validate CREATE mode: Loan Number is blank, creating a new loan
     */
    private void validateCreateMode(LoanMigrationItem item, List<String> errors) {
        int row = item.getRowNumber();

        // Employee ID - REQUIRED
        if (item.getEmployeeId() == null || item.getEmployeeId().isBlank()) {
            errors.add("Row " + row + ": Employee ID is required");
        } else if (memberRepository.findByMemberNumber(item.getEmployeeId()).isEmpty()) {
            errors.add("Row " + row + ": Member with Employee ID '" + item.getEmployeeId() + "' not found. Register the member first.");
        }

        // Loan product - REQUIRED
        if (item.getLoanProductName() == null || item.getLoanProductName().isBlank()) {
            errors.add("Row " + row + ": Loan product name is required");
        } else {
            try {
                if (loanProductRepository.findByName(item.getLoanProductName()).isEmpty()) {
                    List<String> availableNames = loanProductRepository.findAll().stream()
                        .filter(p -> p.getIsActive() != null && p.getIsActive())
                        .map(p -> "'" + p.getName() + "'")
                        .toList();
                    errors.add("Row " + row + ": Loan product '" + item.getLoanProductName() + "' not found. Available products: " + String.join(", ", availableNames));
                }
            } catch (Exception e) {
                errors.add("Row " + row + ": Loan product '" + item.getLoanProductName() + "' is ambiguous - multiple products with this name exist.");
            }
        }

        // Principal - REQUIRED
        if (item.getPrincipalAmount() == null || item.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Row " + row + ": Principal amount must be greater than 0");
        }

        // Term - OPTIONAL (can be filled in later during update)
        if (item.getTermMonths() != null && item.getTermMonths() <= 0) {
            errors.add("Row " + row + ": Term months must be greater than 0 (or leave blank)");
        }

        // Disbursement date - OPTIONAL (can be filled in later during update)
        if (item.getDisbursementDate() != null && item.getDisbursementDate().isAfter(java.time.LocalDate.now())) {
            errors.add("Row " + row + ": Disbursement date cannot be in the future");
        }

        // Loan status - REQUIRED
        if (item.getLoanStatus() == null || item.getLoanStatus().isBlank()) {
            errors.add("Row " + row + ": Loan status is required (DISBURSED, REPAID, or DEFAULTED)");
        } else if (!VALID_STATUSES.contains(item.getLoanStatus())) {
            errors.add("Row " + row + ": Invalid loan status '" + item.getLoanStatus() + "'. Must be DISBURSED, REPAID, or DEFAULTED");
        }

        // Outstanding balance - OPTIONAL (remains null for new loans, can be set in UPDATE later)
        if (item.getOutstandingBalance() != null) {
            if (item.getOutstandingBalance().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Row " + row + ": Outstanding balance must be 0 or greater (or leave blank)");
            }
            if ("REPAID".equals(item.getLoanStatus()) && item.getOutstandingBalance().compareTo(BigDecimal.ZERO) != 0) {
                errors.add("Row " + row + ": Outstanding balance must be 0 for REPAID loans");
            }
            if (item.getPrincipalAmount() != null && item.getOutstandingBalance().compareTo(item.getPrincipalAmount()) > 0) {
                errors.add("Row " + row + ": Outstanding balance cannot exceed principal");
            }
        }

        // Interest collected - OPTIONAL (only for historical/migrated loans)
        if (item.getInterestCollected() != null) {
            if (item.getInterestCollected().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Row " + row + ": Interest collected must be 0 or greater (or leave blank)");
            }
            // Interest can be collected for any loan status (DISBURSED, REPAID, or DEFAULTED)
            // DISBURSED loans can have interest collected during their active repayment period
            // REPAID loans can have interest that was collected during repayment
            // DEFAULTED loans can have interest that accrued before default
        }

        // Guarantorship type - OPTIONAL in CREATE (can be set later via UPDATE)
        if (item.getGuarantorshipType() != null && !item.getGuarantorshipType().isBlank()) {
            if (!VALID_GUARANTORSHIP_TYPES.contains(item.getGuarantorshipType())) {
                errors.add("Row " + row + ": Invalid guarantorship type '" + item.getGuarantorshipType() + "'. Must be NORMAL, SELF, or PARTIAL");
                return; // Can't validate guarantors if type is invalid
            }

            // If errors so far, skip guarantor validation
            if (!errors.isEmpty()) return;

            // Guarantor-specific validation only if type provided
            if ("NORMAL".equals(item.getGuarantorshipType())) {
                errors.addAll(validateNormalGuarantors(item));
            } else if ("SELF".equals(item.getGuarantorshipType())) {
                // Self-guarantee: no external guarantors should be provided
                if (hasAnyGuarantor(item)) {
                    errors.add("Row " + row + ": SELF guarantorship should not have external guarantors. Remove guarantor columns or use NORMAL type.");
                }
            } else if ("PARTIAL".equals(item.getGuarantorshipType())) {
                errors.addAll(validatePartialGuarantors(item));
            }
        } else {
            // Guarantorship type not provided - validate that no guarantors are provided either
            if (hasAnyGuarantor(item)) {
                errors.add("Row " + row + ": Guarantors provided but Guarantorship Type is blank. Specify NORMAL, SELF, or PARTIAL");
            }
        }
    }

    /**
     * Validate UPDATE mode: Loan Number is provided, updating existing loan
     */
    private void validateUpdateMode(LoanMigrationItem item, List<String> errors) {
        int row = item.getRowNumber();
        String loanNumber = item.getLoanNumber().trim();

        // Loan Number - REQUIRED and must exist
        Optional<Loan> existingLoan = loanRepository.findByLoanNumber(loanNumber);
        if (existingLoan.isEmpty()) {
            errors.add("Row " + row + ": Loan '" + loanNumber + "' not found in system");
            return; // Can't validate further without the loan
        }

        Loan loan = existingLoan.get();

        // Loan should be DISBURSED for normal updates (warn if REPAID/DEFAULTED)
        if (loan.getStatus() != Loan.Status.DISBURSED && 
            loan.getStatus() != Loan.Status.REPAID && 
            loan.getStatus() != Loan.Status.DEFAULTED) {
            errors.add("Row " + row + ": Loan '" + loanNumber + "' has status " + loan.getStatus() + 
                ". Can only update DISBURSED, REPAID, or DEFAULTED loans.");
            return;
        }

        // Disbursement date - OPTIONAL in UPDATE (if provided, validate)
        if (item.getDisbursementDate() != null && item.getDisbursementDate().isAfter(java.time.LocalDate.now())) {
            errors.add("Row " + row + ": Disbursement date cannot be in the future");
        }

        // Outstanding balance - OPTIONAL in UPDATE (if provided, validate)
        if (item.getOutstandingBalance() != null) {
            if (item.getOutstandingBalance().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Row " + row + ": Outstanding balance must be 0 or greater");
            }
            if (loan.getAmount() != null && item.getOutstandingBalance().compareTo(loan.getAmount()) > 0) {
                errors.add("Row " + row + ": Outstanding balance cannot exceed principal (" + loan.getAmount() + ")");
            }
            if ("REPAID".equals(loan.getStatus()) && item.getOutstandingBalance().compareTo(BigDecimal.ZERO) != 0) {
                errors.add("Row " + row + ": Outstanding balance must be 0 for REPAID loans");
            }
        }

        // Term - OPTIONAL in UPDATE (if provided, validate)
        if (item.getTermMonths() != null && item.getTermMonths() <= 0) {
            errors.add("Row " + row + ": Term months must be greater than 0 (or leave blank)");
        }

        // Guarantors - OPTIONAL in UPDATE
        if (hasAnyGuarantor(item)) {
            // If any guarantor provided, validate them
            errors.addAll(validateUpdateGuarantors(item, loan));
        }
    }

    /**
     * Validate guarantors for UPDATE mode (more lenient than CREATE mode)
     */
    private List<String> validateUpdateGuarantors(LoanMigrationItem item, Loan loan) {
        List<String> errors = new ArrayList<>();
        int row = item.getRowNumber();

        // Collect all guarantor pairs
        List<String[]> guarantorPairs = getGuarantorPairs(item);

        if (guarantorPairs.isEmpty()) {
            return errors; // No guarantors provided is fine
        }

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

            if (seenGuarantors.contains(gEmpId.toUpperCase())) {
                errors.add("Row " + row + ": Duplicate guarantor '" + gEmpId + "'");
                continue;
            }
            seenGuarantors.add(gEmpId.toUpperCase());

            if (memberRepository.findByMemberNumber(gEmpId).isEmpty()) {
                errors.add("Row " + row + ": Guarantor with Employee ID '" + gEmpId + "' not found");
                continue;
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
     * Validate PARTIAL guarantors: borrower self-guarantees part of loan (column 1),
     * and other members cover the rest (columns 2–6).
     * Column 1 MUST be the borrower's own employee ID.
     * Columns 2–6 are external guarantors, validated like NORMAL.
     * At least one external guarantor (columns 2–6) is required.
     * All pledges must sum exactly to principal.
     */
    private List<String> validatePartialGuarantors(LoanMigrationItem item) {
        List<String> errors = new ArrayList<>();
        int row = item.getRowNumber();
        String borrowerEmployeeId = item.getEmployeeId();

        // Extract column 1 (self-guarantee) and columns 2-6 (external guarantors)
        String col1EmpId = item.getGuarantor1EmployeeId();
        BigDecimal col1Pledge = item.getGuarantor1PledgeAmount();

        // Column 1 validation: must be borrower's own ID
        if (col1EmpId == null || col1EmpId.isBlank()) {
            errors.add("Row " + row + ": PARTIAL guarantorship requires column 1 (self-guarantee) to contain borrower's Employee ID and pledge amount");
            return errors;
        }

        if (!col1EmpId.equalsIgnoreCase(borrowerEmployeeId)) {
            errors.add("Row " + row + ": PARTIAL guarantorship column 1 must be the borrower's Employee ID ('" + borrowerEmployeeId + "'), got '" + col1EmpId + "'");
            return errors;
        }

        if (col1Pledge == null || col1Pledge.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Row " + row + ": PARTIAL guarantorship column 1 pledge amount must be greater than 0");
            return errors;
        }

        // Extract external guarantors (columns 2-6) and validate them like NORMAL
        List<String[]> externalGuarantorPairs = new ArrayList<>();
        addPairIfPresent(externalGuarantorPairs, item.getGuarantor2EmployeeId(), item.getGuarantor2PledgeAmount());
        addPairIfPresent(externalGuarantorPairs, item.getGuarantor3EmployeeId(), item.getGuarantor3PledgeAmount());
        addPairIfPresent(externalGuarantorPairs, item.getGuarantor4EmployeeId(), item.getGuarantor4PledgeAmount());
        addPairIfPresent(externalGuarantorPairs, item.getGuarantor5EmployeeId(), item.getGuarantor5PledgeAmount());
        addPairIfPresent(externalGuarantorPairs, item.getGuarantor6EmployeeId(), item.getGuarantor6PledgeAmount());

        // At least one external guarantor is required
        if (externalGuarantorPairs.isEmpty()) {
            errors.add("Row " + row + ": PARTIAL guarantorship requires at least one external guarantor (columns 2–6). Use SELF type if only borrower guarantees.");
            return errors;
        }

        // Validate external guarantors like NORMAL (no duplicates, valid members, etc.)
        BigDecimal totalExternalPledge = BigDecimal.ZERO;
        Set<String> seenGuarantors = new HashSet<>();
        seenGuarantors.add(borrowerEmployeeId.toUpperCase()); // Borrower already in column 1

        for (String[] pair : externalGuarantorPairs) {
            String gEmpId = pair[0];
            String gPledgeStr = pair[1];
            BigDecimal gPledge;

            try {
                gPledge = new BigDecimal(gPledgeStr);
            } catch (Exception e) {
                errors.add("Row " + row + ": Invalid pledge amount for external guarantor '" + gEmpId + "'");
                continue;
            }

            if (gEmpId.equalsIgnoreCase(borrowerEmployeeId)) {
                errors.add("Row " + row + ": External guarantor '" + gEmpId + "' cannot be the same as the borrower (already in column 1)");
                continue;
            }

            if (seenGuarantors.contains(gEmpId.toUpperCase())) {
                errors.add("Row " + row + ": Duplicate guarantor '" + gEmpId + "'");
                continue;
            }
            seenGuarantors.add(gEmpId.toUpperCase());

            if (memberRepository.findByMemberNumber(gEmpId).isEmpty()) {
                errors.add("Row " + row + ": External guarantor with Employee ID '" + gEmpId + "' not found in system");
                continue;
            }

            totalExternalPledge = totalExternalPledge.add(gPledge);
        }

        // Validate that all pledges (column 1 + columns 2-6) sum exactly to principal
        if (errors.isEmpty()) {
            BigDecimal totalPledge = col1Pledge.add(totalExternalPledge);
            if (totalPledge.compareTo(item.getPrincipalAmount()) != 0) {
                errors.add("Row " + row + ": All pledges (self-guarantee " + col1Pledge + " + external guarantors " + totalExternalPledge + " = " + totalPledge + ") must sum exactly to principal (" + item.getPrincipalAmount() + ")");
            }
        }

        return errors;
    }

    /**
     * Process a validated loan migration item - create new loan or update existing loan.
     * Mode is auto-detected: Loan Number blank = CREATE, Loan Number present = UPDATE
     * For UPDATE mode: creates a snapshot BEFORE modifications so rollback can restore.
     */
    @Transactional
    private void processItem(LoanMigrationItem item, User processor) {
        boolean isUpdateMode = item.getLoanNumber() != null && !item.getLoanNumber().trim().isEmpty();

        if (isUpdateMode) {
            item.setMigrationMode("UPDATE");
            Loan existingLoan = loanRepository.findByLoanNumber(item.getLoanNumber().trim())
                .orElseThrow(() -> new RuntimeException("Loan not found: " + item.getLoanNumber()));

            // Only create a snapshot if one does not already exist for this loan.
            // The snapshot must capture the loan's TRUE original state -- if we
            // overwrote it on every UPDATE-mode migration, a rollback would restore
            // to an already-modified state instead of the real original.
            LoanMigrationSnapshot snapshot = loanMigrationSnapshotRepository
                .findByLoanId(existingLoan.getId())
                .orElseGet(() -> {
                    LoanMigrationSnapshot newSnapshot = new LoanMigrationSnapshot(
                        existingLoan,
                        "Snapshot before UPDATE mode migration item #" + item.getRowNumber()
                    );
                    return loanMigrationSnapshotRepository.save(newSnapshot);
                });
            item.setSnapshot(snapshot);

            processUpdateItem(item, processor);
        } else {
            item.setMigrationMode("CREATE");
            processCreateItem(item, processor);
        }
    }

    /**
     * CREATE mode: Create a brand new loan with minimal or complete data
     * All validation should have already happened in validateItem() and validateCreateMode()
     */
    @Transactional
    private void processCreateItem(LoanMigrationItem item, User processor) {
        // Get required fields (already validated)
        Member borrower = memberRepository.findByMemberNumber(item.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Member not found: " + item.getEmployeeId()));

        LoanProduct product = loanProductRepository.findByName(item.getLoanProductName())
            .orElseThrow(() -> new RuntimeException("Loan product not found: " + item.getLoanProductName()));

        BigDecimal interestRate = product.getInterestRate();
        BigDecimal principal = item.getPrincipalAmount();
        
        // Term: use provided value, or null if not provided (can be filled in via UPDATE later)
        Integer termMonths = item.getTermMonths();

        // Create the loan
        Loan loan = new Loan();
        loan.setMember(borrower);
        loan.setLoanProduct(product);
        loan.setAmount(principal);
        loan.setOriginalPrincipal(principal);
        loan.setOriginalAmount(principal);
        loan.setInterestRate(interestRate);
        loan.setTermMonths(termMonths); // Can be null
        loan.setOutstandingBalance(item.getOutstandingBalance());
        loan.setPurpose(item.getPurpose() != null ? item.getPurpose() : "Migrated loan");
        
        // interestCollected is a historical fact for migrated loans -- the total
        // interest actually paid before migration. It is NOT derived from any
        // formula and does NOT feed into totalInterest/interestRemaining, which
        // have no meaning in this reducing-balance system (interest is only ever
        // determined per-repayment by the treasurer, never precalculated).
        // Do NOT call calculateRepaymentDetails() here -- see LoanService.java
        // line ~278 for the same reasoning already applied to live loans.
        loan.setInterestCollected(
            item.getInterestCollected() != null ? item.getInterestCollected() : BigDecimal.ZERO
        );
        
        // Disbursement date: use provided or null
        if (item.getDisbursementDate() != null) {
            loan.setApplicationDate(item.getDisbursementDate().atStartOfDay());
            loan.setApprovalDate(item.getDisbursementDate().atStartOfDay());
            loan.setDisbursementDate(item.getDisbursementDate().atStartOfDay());
        }
        
        loan.setApprovedBy(processor);
        loan.setDisbursedBy(processor);
        loan.setCreatedBy(processor);
        loan.setMigrationStatus("MIGRATED");
        
        // Set status - determine based on loan status
        Loan.Status loanStatus = Loan.Status.valueOf(item.getLoanStatus());
        
        // Only set eligibility status if loan is in approval stages
        // For disbursed, repaid, or defaulted loans, don't show eligibility status
        if (loanStatus != Loan.Status.DISBURSED && 
            loanStatus != Loan.Status.REPAID && 
            loanStatus != Loan.Status.DEFAULTED) {
            loan.setMemberEligibilityStatus("APPROVED");
        }

        loan.setStatus(loanStatus);

        // Auto-generate loan number using centralized service
        loan.setLoanNumber(loanNumberGenerationService.generateLoanNumberForYear(java.time.LocalDate.now().getYear()));

        loan = loanRepository.save(loan);

        // Create LOAN_DISBURSEMENT transaction for audit trail
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
            if (item.getDisbursementDate() != null) {
                disbursementTransaction.setTransactionDate(item.getDisbursementDate().atStartOfDay());
            } else {
                disbursementTransaction.setTransactionDate(LocalDateTime.now());
            }
            transactionRepository.save(disbursementTransaction);
        }

        // Create guarantors (only if guarantorship type is provided)
        if (item.getGuarantorshipType() != null && !item.getGuarantorshipType().isBlank()) {
            // Calculate reduction ratio for partially-repaid loans
            // For loans already partially repaid at migration, pledges must be scaled by:
            // reduction_ratio = outstanding_balance / original_principal
            // This ensures frozen pledge amounts reflect the actual remaining exposure
            BigDecimal reductionRatio = BigDecimal.ONE;
            if (principal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal outstanding = item.getOutstandingBalance() != null ? item.getOutstandingBalance() : BigDecimal.ZERO;
                if (outstanding.compareTo(BigDecimal.ZERO) < 0) outstanding = BigDecimal.ZERO;
                reductionRatio = outstanding.divide(principal, 10, java.math.RoundingMode.HALF_UP);
            }
            
            if ("SELF".equals(item.getGuarantorshipType())) {
                Guarantor selfGuarantor = new Guarantor();
                selfGuarantor.setLoan(loan);
                selfGuarantor.setMember(borrower);
                selfGuarantor.setSelfGuarantee(true);
                selfGuarantor.setGuaranteeAmount(principal);
                // Apply reduction ratio to pledge amount for partially-repaid loans
                BigDecimal adjustedSelfPledge = principal.multiply(reductionRatio);
                selfGuarantor.setPledgeAmount(loanStatus == Loan.Status.DISBURSED ? adjustedSelfPledge : BigDecimal.ZERO);
                selfGuarantor.setStatus(loanStatus == Loan.Status.DISBURSED ? Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
                if (item.getDisbursementDate() != null) {
                    selfGuarantor.setApprovedAt(item.getDisbursementDate().atStartOfDay());
                } else {
                    selfGuarantor.setApprovedAt(LocalDateTime.now());
                }
                selfGuarantor.setMigrationStatus("MIGRATED");
                guarantorRepository.save(selfGuarantor);

                // Freeze savings for active self-guaranteed loans (using adjusted pledge)
                if (loanStatus == Loan.Status.DISBURSED) {
                    freezeSavings(borrower, adjustedSelfPledge);
                }
            } else if ("NORMAL".equals(item.getGuarantorshipType())) {
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
                    // Apply reduction ratio to pledge amount for partially-repaid loans
                    BigDecimal adjustedPledge = gPledge.multiply(reductionRatio);
                    guarantor.setPledgeAmount(loanStatus == Loan.Status.DISBURSED ? adjustedPledge : BigDecimal.ZERO);
                    guarantor.setStatus(loanStatus == Loan.Status.DISBURSED ? Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
                    if (item.getDisbursementDate() != null) {
                        guarantor.setApprovedAt(item.getDisbursementDate().atStartOfDay());
                    } else {
                        guarantor.setApprovedAt(LocalDateTime.now());
                    }
                    guarantor.setMigrationStatus("MIGRATED");
                    guarantorRepository.save(guarantor);
                }
            } else if ("PARTIAL".equals(item.getGuarantorshipType())) {
                // Partial guarantorship: borrower self-guarantees part, external guarantors cover rest
                
                // Create self-guarantee for borrower (column 1 pledge)
                BigDecimal selfPledgeAmount = item.getGuarantor1PledgeAmount();
                Guarantor selfGuarantor = new Guarantor();
                selfGuarantor.setLoan(loan);
                selfGuarantor.setMember(borrower);
                selfGuarantor.setSelfGuarantee(true);
                selfGuarantor.setGuaranteeAmount(selfPledgeAmount);
                // Apply reduction ratio to pledge amount for partially-repaid loans
                BigDecimal adjustedSelfPledge = selfPledgeAmount.multiply(reductionRatio);
                selfGuarantor.setPledgeAmount(loanStatus == Loan.Status.DISBURSED ? adjustedSelfPledge : BigDecimal.ZERO);
                selfGuarantor.setStatus(loanStatus == Loan.Status.DISBURSED ? Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
                if (item.getDisbursementDate() != null) {
                    selfGuarantor.setApprovedAt(item.getDisbursementDate().atStartOfDay());
                } else {
                    selfGuarantor.setApprovedAt(LocalDateTime.now());
                }
                selfGuarantor.setMigrationStatus("MIGRATED");
                guarantorRepository.save(selfGuarantor);

                // Freeze savings for active partial-guaranteed loans (using adjusted self pledge)
                if (loanStatus == Loan.Status.DISBURSED) {
                    freezeSavings(borrower, adjustedSelfPledge);
                }

                // Create external guarantors (columns 2-6)
                List<String[]> externalGuarantorPairs = new ArrayList<>();
                addPairIfPresent(externalGuarantorPairs, item.getGuarantor2EmployeeId(), item.getGuarantor2PledgeAmount());
                addPairIfPresent(externalGuarantorPairs, item.getGuarantor3EmployeeId(), item.getGuarantor3PledgeAmount());
                addPairIfPresent(externalGuarantorPairs, item.getGuarantor4EmployeeId(), item.getGuarantor4PledgeAmount());
                addPairIfPresent(externalGuarantorPairs, item.getGuarantor5EmployeeId(), item.getGuarantor5PledgeAmount());
                addPairIfPresent(externalGuarantorPairs, item.getGuarantor6EmployeeId(), item.getGuarantor6PledgeAmount());

                for (String[] pair : externalGuarantorPairs) {
                    String gEmpId = pair[0];
                    BigDecimal gPledge = new BigDecimal(pair[1]);

                    Member guarantorMember = memberRepository.findByMemberNumber(gEmpId)
                        .orElseThrow(() -> new RuntimeException("Guarantor not found: " + gEmpId));

                    Guarantor guarantor = new Guarantor();
                    guarantor.setLoan(loan);
                    guarantor.setMember(guarantorMember);
                    guarantor.setSelfGuarantee(false);
                    guarantor.setGuaranteeAmount(gPledge);
                    // Apply reduction ratio to pledge amount for partially-repaid loans
                    BigDecimal adjustedPledge = gPledge.multiply(reductionRatio);
                    guarantor.setPledgeAmount(loanStatus == Loan.Status.DISBURSED ? adjustedPledge : BigDecimal.ZERO);
                    guarantor.setStatus(loanStatus == Loan.Status.DISBURSED ? Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
                    if (item.getDisbursementDate() != null) {
                        guarantor.setApprovedAt(item.getDisbursementDate().atStartOfDay());
                    } else {
                        guarantor.setApprovedAt(LocalDateTime.now());
                    }
                    guarantor.setMigrationStatus("MIGRATED");
                    guarantorRepository.save(guarantor);
                }
            }
        }
        // If guarantorship type is blank, no guarantors created (can be added later via UPDATE)

        item.setLoan(loan);
    }

    /**
     * UPDATE mode: Update an existing loan with provided fields
     * Blank fields are skipped (not updated)
     */
    @Transactional
    private void processUpdateItem(LoanMigrationItem item, User processor) {
        String loanNumber = item.getLoanNumber().trim();
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanNumber));

        // Build audit trail of changes
        StringBuilder changeLog = new StringBuilder();

        // Only update fields if they are provided (non-null/non-blank)

        // Term (if provided)
        if (item.getTermMonths() != null && item.getTermMonths() > 0) {
            if ((loan.getTermMonths() == null && item.getTermMonths() != null) ||
                (loan.getTermMonths() != null && !loan.getTermMonths().equals(item.getTermMonths()))) {
                changeLog.append("Term: ").append(loan.getTermMonths()).append(" → ").append(item.getTermMonths()).append("; ");
                loan.setTermMonths(item.getTermMonths());
            }
        }

        // Disbursement Date (if provided)
        if (item.getDisbursementDate() != null) {
            LocalDateTime newDisbursementDateTime = item.getDisbursementDate().atStartOfDay();
            if (!newDisbursementDateTime.equals(loan.getDisbursementDate())) {
                changeLog.append("Disbursement Date: ").append(loan.getDisbursementDate()).append(" → ").append(newDisbursementDateTime).append("; ");
                loan.setDisbursementDate(newDisbursementDateTime);
            }
        }

        // Outstanding Balance (if provided)
        if (item.getOutstandingBalance() != null) {
            if ((loan.getOutstandingBalance() == null && item.getOutstandingBalance() != null) ||
                (loan.getOutstandingBalance() != null && loan.getOutstandingBalance().compareTo(item.getOutstandingBalance()) != 0)) {
                changeLog.append("Outstanding Balance: ").append(loan.getOutstandingBalance()).append(" → ").append(item.getOutstandingBalance()).append("; ");
                loan.setOutstandingBalance(item.getOutstandingBalance());
            }
        }

        // Interest Collected (if provided) -- historical fact for migrated loans,
        // set directly with no derived calculation (see processCreateItem for the
        // same reasoning: totalInterest/interestRemaining have no meaning in this
        // reducing-balance system).
        if (item.getInterestCollected() != null) {
            if ((loan.getInterestCollected() == null && item.getInterestCollected() != null) ||
                (loan.getInterestCollected() != null && loan.getInterestCollected().compareTo(item.getInterestCollected()) != 0)) {
                changeLog.append("Interest Collected: ").append(loan.getInterestCollected()).append(" → ").append(item.getInterestCollected()).append("; ");
                loan.setInterestCollected(item.getInterestCollected());
            }
        }

        // Guarantors (if any provided - all-or-nothing replacement)
        if (hasAnyGuarantor(item)) {
            List<String[]> guarantorPairs = getGuarantorPairs(item);
            List<LoanGuarantorUpdateService.GuarantorPair> newGuarantors = new ArrayList<>();
            for (String[] pair : guarantorPairs) {
                newGuarantors.add(new LoanGuarantorUpdateService.GuarantorPair(pair[0], new BigDecimal(pair[1])));
            }
            
            try {
                String guarantorChangeLog = loanGuarantorUpdateService.updateGuarantors(loan, newGuarantors, processor);
                changeLog.append(guarantorChangeLog).append("; ");
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to update guarantors: " + e.getMessage());
            }
        }

        // Save the updated loan
        loan = loanRepository.save(loan);

        // Log to audit trail
        if (changeLog.length() > 0) {
            auditService.logAction(processor, "LOAN_UPDATE_MIGRATION", "Loan", loan.getId(),
                "Loan " + loanNumber + " updated: " + changeLog.toString(), null, null);
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

    public List<LoanMigrationItem> getMigrationItems(Long batchId) {
        return loanMigrationItemRepository.findByBatch_Id(batchId);
    }

    /**
     * Generate a properly formatted Excel template for loan migration.
     * Supports DUAL MODE: CREATE (blank Loan #) and UPDATE (populated Loan #)
     * Supports three guarantorship types:
     * - NORMAL: External guarantors only (column 1+ as external guarantors)
     * - SELF: Borrower self-guarantees 100% of principal (no external guarantors)
     * - PARTIAL: Borrower self-guarantees part (column 1), external guarantors cover rest (columns 2+)
     * Columns MUST match the order expected by parseLoanMigration().
     */
    public byte[] generateLoanMigrationTemplate() {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Loan Migration");

            // Create header row with proper column order
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Loan Number (blank=CREATE, populate=UPDATE)",    // 0
                    "Employee ID (optional - required for CREATE, can omit for UPDATE)",              // 1
                    "Loan Product Name (optional - required for CREATE, can omit for UPDATE)",        // 2
                    "Principal Amount (optional - required for CREATE, can omit for UPDATE)",         // 3
                    "Term Months (optional - can set via UPDATE)",  // 4
                    "Interest Rate % (optional, uses product default)", // 5
                    "Disbursement Date DD/MM/YYYY (optional)",        // 6
                    "Loan Status (optional - can set via UPDATE)",              // 7
                    "Outstanding Balance (optional, set via UPDATE)",  // 8
                    "Interest Collected KES (optional - historical interest from migrated loans)",  // 9
                    "Guarantorship Type (optional - can be set via UPDATE later)",       // 10
                    "Guarantor 1 Employee ID",                        // 11-12
                    "Guarantor 1 Pledge Amount",
                    "Guarantor 2 Employee ID",                        // 13-14
                    "Guarantor 2 Pledge Amount",
                    "Guarantor 3 Employee ID",                        // 15-16
                    "Guarantor 3 Pledge Amount",
                    "Guarantor 4 Employee ID",                        // 17-18
                    "Guarantor 4 Pledge Amount",
                    "Guarantor 5 Employee ID",                        // 19-20
                    "Guarantor 5 Pledge Amount",
                    "Guarantor 6 Employee ID",                        // 21-22
                    "Guarantor 6 Pledge Amount",
                    "Purpose (optional)"                              // 23
            };

            // Create header cells with formatting
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example row 1: CREATE mode - Emergency Loan with guarantors
            org.apache.poi.ss.usermodel.Row exampleRow1 = sheet.createRow(1);
            exampleRow1.createCell(0).setCellValue("");                          // Loan Number (BLANK = CREATE)
            exampleRow1.createCell(1).setCellValue("EMP041");                    // Employee ID
            exampleRow1.createCell(2).setCellValue("Emergency Loan 1");          // Loan Product Name
            exampleRow1.createCell(3).setCellValue(100000);                      // Principal Amount
            exampleRow1.createCell(4).setCellValue(12);                          // Term (Months)
            exampleRow1.createCell(5).setCellValue("");                          // Interest Rate (optional)
            exampleRow1.createCell(6).setCellValue("15/01/2024");                // Disbursement Date
            exampleRow1.createCell(7).setCellValue("DISBURSED");                 // Loan Status
            exampleRow1.createCell(8).setCellValue("");                          // Outstanding Balance (optional for CREATE)
            exampleRow1.createCell(9).setCellValue(15000);                       // Interest Collected (optional - historical interest)
            exampleRow1.createCell(10).setCellValue("NORMAL");                   // Guarantorship Type
            exampleRow1.createCell(11).setCellValue("EMP066");                   // Guarantor 1 ID
            exampleRow1.createCell(12).setCellValue(50000);                      // Guarantor 1 Pledge
            exampleRow1.createCell(13).setCellValue("EMP063");                   // Guarantor 2 ID
            exampleRow1.createCell(14).setCellValue(50000);                      // Guarantor 2 Pledge
            // Guarantors 3-6 left blank

            // Example row 2: CREATE mode - Self-guaranteed loan with no interest collected yet
            org.apache.poi.ss.usermodel.Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("");                          // Loan Number (BLANK = CREATE)
            exampleRow2.createCell(1).setCellValue("EMP040");                    // Employee ID
            exampleRow2.createCell(2).setCellValue("Normal Loan");               // Loan Product Name
            exampleRow2.createCell(3).setCellValue(300000);                      // Principal Amount
            exampleRow2.createCell(4).setCellValue(60);                          // Term (Months)
            exampleRow2.createCell(5).setCellValue("");                          // Interest Rate (optional)
            exampleRow2.createCell(6).setCellValue("03/02/2025");                // Disbursement Date
            exampleRow2.createCell(7).setCellValue("DISBURSED");                 // Loan Status
            exampleRow2.createCell(8).setCellValue("");                          // Outstanding Balance (optional)
            exampleRow2.createCell(9).setCellValue(0);                           // Interest Collected (0 for new loans)
            exampleRow2.createCell(10).setCellValue("SELF");                     // Guarantorship Type (SELF)

            // Example row 3: UPDATE mode - Update guarantors only
            org.apache.poi.ss.usermodel.Row exampleRow3 = sheet.createRow(3);
            exampleRow3.createCell(0).setCellValue("L001");                      // Loan Number (POPULATED = UPDATE)
            // Remaining fields blank or with updates only
            exampleRow3.createCell(11).setCellValue("EMP010");                   // Guarantor 1 ID (update)
            exampleRow3.createCell(12).setCellValue(100000);                     // Guarantor 1 Pledge (update)

            // Example row 4: UPDATE mode - Update multiple fields including interest collected
            org.apache.poi.ss.usermodel.Row exampleRow4 = sheet.createRow(4);
            exampleRow4.createCell(0).setCellValue("L002");                      // Loan Number (POPULATED = UPDATE)
            exampleRow4.createCell(4).setCellValue(24);                          // Term (update from 12 to 24)
            exampleRow4.createCell(6).setCellValue("15/03/2025");                // Disbursement Date (update)
            exampleRow4.createCell(8).setCellValue(80000);                       // Outstanding Balance (update)
            exampleRow4.createCell(9).setCellValue(25000);                       // Interest Collected (update - additional interest paid)

            // Example row 5: CREATE mode - PARTIAL guarantorship (borrower self-guarantees part, others cover rest)
            org.apache.poi.ss.usermodel.Row exampleRow5 = sheet.createRow(5);
            exampleRow5.createCell(0).setCellValue("");                          // Loan Number (BLANK = CREATE)
            exampleRow5.createCell(1).setCellValue("EMP050");                    // Employee ID
            exampleRow5.createCell(2).setCellValue("Normal Loan");               // Loan Product Name
            exampleRow5.createCell(3).setCellValue(500000);                      // Principal Amount
            exampleRow5.createCell(4).setCellValue(36);                          // Term (Months)
            exampleRow5.createCell(5).setCellValue("");                          // Interest Rate (optional)
            exampleRow5.createCell(6).setCellValue("01/06/2024");                // Disbursement Date
            exampleRow5.createCell(7).setCellValue("DISBURSED");                 // Loan Status
            exampleRow5.createCell(8).setCellValue("");                          // Outstanding Balance (optional for CREATE)
            exampleRow5.createCell(9).setCellValue(0);                           // Interest Collected
            exampleRow5.createCell(10).setCellValue("PARTIAL");                  // Guarantorship Type (PARTIAL)
            exampleRow5.createCell(11).setCellValue("EMP050");                   // Guarantor 1 (borrower self-guarantee) - MUST be Employee ID
            exampleRow5.createCell(12).setCellValue(200000);                     // Guarantor 1 Pledge (self-guarantee amount)
            exampleRow5.createCell(13).setCellValue("EMP045");                   // Guarantor 2 (external) ID
            exampleRow5.createCell(14).setCellValue(200000);                     // Guarantor 2 Pledge
            exampleRow5.createCell(15).setCellValue("EMP042");                   // Guarantor 3 (external) ID
            exampleRow5.createCell(16).setCellValue(100000);                     // Guarantor 3 Pledge

            // Auto-size columns for readability
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            return baos.toByteArray();

        } catch (java.io.IOException e) {
            throw new RuntimeException("Error generating loan migration template: " + e.getMessage());
        }
    }
}