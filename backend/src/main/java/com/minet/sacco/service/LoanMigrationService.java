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

        // PHASE 4: Loan migration imports outstanding_balance as a snapshot only.
        // totalInterest is NOT imported or recalculated. We treat outstandingBalance as the single source of truth.
        // All future repayments on migrated loans follow Phase 2's rules (mandatory principal/interest split).
        // No special-casing or backfill of totalInterest from the old system.
        
        BigDecimal interestRate = product.getInterestRate();
        BigDecimal principal = item.getPrincipalAmount();
        Integer termMonths = item.getTermMonths();
        
        // Calculate monthly repayment for informational purposes only.
        // Outstanding balance may differ from principal, but monthly repayment is consistent with original terms.
        BigDecimal rate = interestRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal timeInYears = new BigDecimal(termMonths).divide(new BigDecimal("12"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal monthlyRepayment = principal.multiply(rate).multiply(timeInYears)
            .divide(new BigDecimal(termMonths), 2, java.math.RoundingMode.HALF_UP);

        // Store values on item for reporting - but totalInterest and totalRepayable are NOT set
        // This ensures the loan migration process respects the historical snapshot
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
        // PHASE 4: Do NOT set totalInterest or totalRepayable
        // Outstanding balance is the only snapshot we import - it represents current state, not calculated values
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

    /**
     * Generate a properly formatted Excel template for loan migration.
     * Columns MUST match the order expected by parseLoanMigration().
     */
    public byte[] generateLoanMigrationTemplate() {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Loan Migration");

            // Create header row with proper column order - MUST match parseLoanMigration() expectations exactly
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Employee ID",                                    // 0: Required
                    "Loan Number",                                    // 1: Optional (can be blank, system generates if empty)
                    "Loan Product Name",                             // 2: Required (e.g. Emergency Loan 1, Normal Loan)
                    "Principal Amount",                              // 3: Required
                    "Term (Months)",                                 // 4: Required
                    "Interest Rate % (optional)",                    // 5: Optional (uses product default if empty)
                    "Disbursement Date (DD/MM/YYYY)",               // 6: REQUIRED - This is CRITICAL!
                    "Loan Status (DISBURSED/REPAID/DEFAULTED)",     // 7: Required
                    "Outstanding Balance",                           // 8: Required
                    "Guarantorship Type (NORMAL/SELF)",             // 9: Required
                    "Guarantor 1 Employee ID",                       // 10-11: Optional
                    "Guarantor 1 Pledge Amount",
                    "Guarantor 2 Employee ID",                       // 12-13: Optional
                    "Guarantor 2 Pledge Amount",
                    "Guarantor 3 Employee ID",                       // 14-15: Optional
                    "Guarantor 3 Pledge Amount",
                    "Guarantor 4 Employee ID",                       // 16-17: Optional
                    "Guarantor 4 Pledge Amount",
                    "Guarantor 5 Employee ID",                       // 18-19: Optional
                    "Guarantor 5 Pledge Amount",
                    "Guarantor 6 Employee ID",                       // 20-21: Optional
                    "Guarantor 6 Pledge Amount",
                    "Purpose (optional)"                             // 22: Optional
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

            // Example row 1: Emergency Loan with guarantors
            org.apache.poi.ss.usermodel.Row exampleRow1 = sheet.createRow(1);
            exampleRow1.createCell(0).setCellValue("EMP041");                    // Employee ID
            exampleRow1.createCell(1).setCellValue("");                          // Loan Number (optional - leave blank to auto-generate)
            exampleRow1.createCell(2).setCellValue("Emergency Loan 1");           // Loan Product Name
            exampleRow1.createCell(3).setCellValue(100000);                      // Principal Amount
            exampleRow1.createCell(4).setCellValue(12);                          // Term (Months)
            exampleRow1.createCell(5).setCellValue("");                          // Interest Rate % (optional)
            exampleRow1.createCell(6).setCellValue("15/01/2024");                // DISBURSEMENT DATE - REQUIRED!
            exampleRow1.createCell(7).setCellValue("DISBURSED");                 // Loan Status
            exampleRow1.createCell(8).setCellValue(75000);                       // Outstanding Balance
            exampleRow1.createCell(9).setCellValue("NORMAL");                    // Guarantorship Type
            exampleRow1.createCell(10).setCellValue("EMP066");                   // Guarantor 1 ID
            exampleRow1.createCell(11).setCellValue(50000);                      // Guarantor 1 Pledge
            exampleRow1.createCell(12).setCellValue("EMP063");                   // Guarantor 2 ID
            exampleRow1.createCell(13).setCellValue(50000);                      // Guarantor 2 Pledge
            // Guarantors 3-6 left blank

            // Example row 2: Normal loan with SELF guarantee (no guarantors)
            org.apache.poi.ss.usermodel.Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("EMP040");                    // Employee ID
            exampleRow2.createCell(1).setCellValue("");                          // Loan Number (optional)
            exampleRow2.createCell(2).setCellValue("Normal Loan");               // Loan Product Name
            exampleRow2.createCell(3).setCellValue(300000);                      // Principal Amount
            exampleRow2.createCell(4).setCellValue(60);                          // Term (Months)
            exampleRow2.createCell(5).setCellValue("");                          // Interest Rate (optional)
            exampleRow2.createCell(6).setCellValue("03/02/2025");                // DISBURSEMENT DATE - REQUIRED!
            exampleRow2.createCell(7).setCellValue("DISBURSED");                 // Loan Status
            exampleRow2.createCell(8).setCellValue(190000);                      // Outstanding Balance
            exampleRow2.createCell(9).setCellValue("SELF");                      // Guarantorship Type (SELF = no guarantors needed)
            // Leave guarantor columns blank

            // Example row 3: Repaid loan
            org.apache.poi.ss.usermodel.Row exampleRow3 = sheet.createRow(3);
            exampleRow3.createCell(0).setCellValue("EMP042");
            exampleRow3.createCell(1).setCellValue("");
            exampleRow3.createCell(2).setCellValue("Emergency Loan 2");
            exampleRow3.createCell(3).setCellValue(50000);
            exampleRow3.createCell(4).setCellValue(6);
            exampleRow3.createCell(5).setCellValue("");
            exampleRow3.createCell(6).setCellValue("01/03/2024");                // DISBURSEMENT DATE - REQUIRED!
            exampleRow3.createCell(7).setCellValue("REPAID");
            exampleRow3.createCell(8).setCellValue(0);
            exampleRow3.createCell(9).setCellValue("NORMAL");
            exampleRow3.createCell(10).setCellValue("EMP054");
            exampleRow3.createCell(11).setCellValue(35000);
            exampleRow3.createCell(12).setCellValue("EMP055");
            exampleRow3.createCell(13).setCellValue(15000);

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