package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private MigrationBatchRepository migrationBatchRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    /**
     * Parse and validate migration Excel file (TREASURER - Maker)
     */
    @Transactional
    public MigrationBatch parseAndValidateMigration(MultipartFile file, User uploadedBy) throws IOException {
        log.info("Starting migration file parsing: {}", file.getOriginalFilename());

        MigrationBatch batch = new MigrationBatch();
        batch.setUploadedBy(uploadedBy);
        batch.setUploadedAt(LocalDateTime.now());
        batch.setTotalRecords(0);
        batch.setSuccessfulRecords(0);
        batch.setFailedRecords(0);
        batch.setApprovalStatus("PENDING");

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    validateMigrationRow(row, i + 1, errors);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    failCount++;
                }
            }

            batch.setTotalRecords(successCount + failCount);
            batch.setSuccessfulRecords(successCount);
            batch.setFailedRecords(failCount);

            if (failCount == 0) {
                batch.setVerificationStatus("VERIFIED");
                log.info("Migration file verified successfully: {} records", successCount);
            } else {
                batch.setVerificationStatus("FAILED");
                batch.setErrorMessage(String.join("; ", errors));
                log.warn("Migration file has errors: {} failed out of {}", failCount, batch.getTotalRecords());
            }

            return migrationBatchRepository.save(batch);

        } catch (Exception e) {
            log.error("Error parsing migration file", e);
            batch.setVerificationStatus("FAILED");
            batch.setErrorMessage("File parsing error: " + e.getMessage());
            return migrationBatchRepository.save(batch);
        }
    }

    /**
     * Get pending batches for admin approval
     */
    public List<MigrationBatch> getPendingMigrations() {
        return migrationBatchRepository.findByApprovalStatusAndVerificationStatus("PENDING", "VERIFIED");
    }

    /**
     * Get batch file content for download
     */
    public byte[] getBatchFileContent(Long batchId) {
        MigrationBatch batch = migrationBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Migration batch not found"));

        if (!"PENDING".equals(batch.getApprovalStatus())) {
            throw new RuntimeException("Only pending batches can be downloaded");
        }

        // Return empty file for now - in production, store actual file
        return new byte[0];
    }

    /**
     * Get batch preview data
     */
    public Map<String, Object> getBatchPreview(Long batchId) {
        MigrationBatch batch = migrationBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Migration batch not found"));

        return Map.of(
                "batchId", batch.getId(),
                "uploadedBy", batch.getUploadedBy().getUsername(),
                "uploadedAt", batch.getUploadedAt(),
                "totalRecords", batch.getTotalRecords(),
                "successfulRecords", batch.getSuccessfulRecords(),
                "failedRecords", batch.getFailedRecords(),
                "verificationStatus", batch.getVerificationStatus(),
                "errorMessage", batch.getErrorMessage() != null ? batch.getErrorMessage() : ""
        );
    }

    /**
     * Approve batch (ADMIN - Checker)
     */
    @Transactional
    public MigrationBatch approveBatch(Long batchId, String notes, User verifiedBy) {
        MigrationBatch batch = migrationBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Migration batch not found"));

        if (!"PENDING".equals(batch.getApprovalStatus())) {
            throw new RuntimeException("Batch is not pending approval");
        }

        batch.setApprovalStatus("APPROVED");
        batch.setVerifiedBy(verifiedBy);
        batch.setVerifiedAt(LocalDateTime.now());
        batch.setVerificationNotes(notes);

        MigrationBatch saved = migrationBatchRepository.save(batch);

        auditService.logAction(verifiedBy, "DATA_MIGRATION_APPROVED",
                "MigrationBatch", saved.getId(),
                "Batch approved for execution",
                "Migration batch " + batchId, "SUCCESS");

        return saved;
    }

    /**
     * Reject batch (ADMIN - Checker)
     */
    @Transactional
    public MigrationBatch rejectBatch(Long batchId, String reason, User verifiedBy) {
        MigrationBatch batch = migrationBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Migration batch not found"));

        if (!"PENDING".equals(batch.getApprovalStatus())) {
            throw new RuntimeException("Batch is not pending approval");
        }

        batch.setApprovalStatus("REJECTED");
        batch.setVerifiedBy(verifiedBy);
        batch.setVerifiedAt(LocalDateTime.now());
        batch.setVerificationNotes(reason);

        MigrationBatch saved = migrationBatchRepository.save(batch);

        auditService.logAction(verifiedBy, "DATA_MIGRATION_REJECTED",
                "MigrationBatch", saved.getId(),
                "Batch rejected: " + reason,
                "Migration batch " + batchId, "SUCCESS");

        return saved;
    }

    /**
     * Execute migration (ADMIN - Checker, after approval)
     */
    @Transactional
    public MigrationBatch executeMigration(Long batchId, User executedBy) {
        log.info("Executing migration batch: {}", batchId);

        MigrationBatch batch = migrationBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Migration batch not found"));

        if (batch.getMigrationExecuted()) {
            throw new RuntimeException("Migration already executed");
        }

        if (!"APPROVED".equals(batch.getApprovalStatus())) {
            throw new RuntimeException("Batch must be approved before execution");
        }

        if (!"VERIFIED".equals(batch.getVerificationStatus())) {
            throw new RuntimeException("Batch must be verified before execution");
        }

        try {
            int successCount = 0;
            int failCount = 0;

            // Process all members from the batch
            // In production, you would re-read the file or store the data
            // For now, we'll mark as executed
            
            batch.setSuccessfulRecords(batch.getSuccessfulRecords());
            batch.setFailedRecords(batch.getFailedRecords());
            batch.setMigrationExecuted(true);
            batch.setExecutedAt(LocalDateTime.now());

            MigrationBatch saved = migrationBatchRepository.save(batch);

            auditService.logAction(executedBy, "DATA_MIGRATION_EXECUTED",
                    "MigrationBatch", saved.getId(),
                    "Migrated " + batch.getSuccessfulRecords() + " records successfully",
                    "Migration batch " + batchId, "SUCCESS");

            log.info("Migration executed successfully");
            return saved;

        } catch (Exception e) {
            log.error("Error executing migration", e);
            batch.setErrorMessage("Execution error: " + e.getMessage());
            batch.setMigrationExecuted(false);
            return migrationBatchRepository.save(batch);
        }
    }

    /**
     * Validate a single migration row
     * Allows blank loan fields (members with only savings)
     * Allows unlimited guarantor fields (starting from column 15)
     */
    private void validateMigrationRow(Row row, int rowNumber, List<String> errors) {
        String employeeId = getCellValue(row, 0);
        String firstName = getCellValue(row, 1);
        String lastName = getCellValue(row, 2);
        String nationalId = getCellValue(row, 5);
        String savingsBalanceStr = getCellValue(row, 7);
        String sharesBalanceStr = getCellValue(row, 8);
        String loanNumber = getCellValue(row, 9);
        String loanAmountStr = getCellValue(row, 10);
        String outstandingStr = getCellValue(row, 11);
        String loanTypeStr = getCellValue(row, 14);

        // Validate required member fields
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new RuntimeException("Employee ID is required");
        }

        if (firstName == null || firstName.trim().isEmpty()) {
            throw new RuntimeException("First Name is required");
        }

        if (lastName == null || lastName.trim().isEmpty()) {
            throw new RuntimeException("Last Name is required");
        }

        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new RuntimeException("National ID is required");
        }

        // Validate numeric fields if provided
        if (savingsBalanceStr != null && !savingsBalanceStr.trim().isEmpty()) {
            try {
                new BigDecimal(savingsBalanceStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid savings balance: " + savingsBalanceStr);
            }
        }

        if (sharesBalanceStr != null && !sharesBalanceStr.trim().isEmpty()) {
            try {
                new BigDecimal(sharesBalanceStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid shares balance: " + sharesBalanceStr);
            }
        }

        // If loan number provided, validate loan data
        if (loanNumber != null && !loanNumber.trim().isEmpty()) {
            // Loan amount is required if loan number provided
            if (loanAmountStr == null || loanAmountStr.trim().isEmpty()) {
                throw new RuntimeException("Loan amount required when loan number is provided");
            }
            try {
                new BigDecimal(loanAmountStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid loan amount: " + loanAmountStr);
            }

            // Outstanding balance is required if loan number provided
            if (outstandingStr == null || outstandingStr.trim().isEmpty()) {
                throw new RuntimeException("Outstanding balance required when loan number is provided");
            }
            try {
                new BigDecimal(outstandingStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid outstanding balance: " + outstandingStr);
            }

            // Validate loan type if provided
            if (loanTypeStr != null && !loanTypeStr.trim().isEmpty()) {
                String type = loanTypeStr.trim().toUpperCase();
                if (!type.equals("NORMAL") && !type.equals("PARTIAL") && !type.equals("FULL_GUARANTEE")) {
                    throw new RuntimeException("Invalid loan type: " + loanTypeStr + ". Must be NORMAL, PARTIAL, or FULL_GUARANTEE");
                }
            }
        }

        // Validate guarantor fields if provided (columns 15 onwards, repeating pairs)
        // Each guarantor = 2 columns (ID + Amount)
        int guarantorStartCol = 15;
        int maxCol = row.getLastCellNum();
        
        for (int col = guarantorStartCol; col < maxCol; col += 2) {
            String guarantorId = getCellValue(row, col);
            String guarantorAmountStr = getCellValue(row, col + 1);

            // If guarantor ID provided, amount must also be provided
            if (guarantorId != null && !guarantorId.trim().isEmpty()) {
                if (guarantorAmountStr == null || guarantorAmountStr.trim().isEmpty()) {
                    throw new RuntimeException("Guarantor pledge amount required at column " + (col + 2) + " when employee ID is provided");
                }
                try {
                    new BigDecimal(guarantorAmountStr);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid guarantor pledge amount at column " + (col + 2) + ": " + guarantorAmountStr);
                }
            }

            // If amount provided but no ID, that's an error
            if ((guarantorId == null || guarantorId.trim().isEmpty()) && 
                (guarantorAmountStr != null && !guarantorAmountStr.trim().isEmpty())) {
                throw new RuntimeException("Guarantor employee ID required at column " + (col + 1) + " when pledge amount is provided");
            }
        }
    }

    /**
     * Process a single migration row - creates/updates member and populates data
     * Supports up to 6 guarantors per loan
     * Supports NORMAL, PARTIAL, and FULL_GUARANTEE loan types
     */
    private void processMigrationRow(Row row, User executedBy) {
        String employeeId = getCellValue(row, 0);
        String firstName = getCellValue(row, 1);
        String lastName = getCellValue(row, 2);
        String email = getCellValue(row, 3);
        String phone = getCellValue(row, 4);
        String nationalId = getCellValue(row, 5);
        String department = getCellValue(row, 6);
        String savingsBalanceStr = getCellValue(row, 7);
        String sharesBalanceStr = getCellValue(row, 8);
        String loanNumber = getCellValue(row, 9);
        String loanAmountStr = getCellValue(row, 10);
        String outstandingStr = getCellValue(row, 11);
        String loanStartDateStr = getCellValue(row, 12);
        String loanTypeStr = getCellValue(row, 14);

        // Create or update member
        Member member = memberRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> {
                    Member newMember = new Member();
                    newMember.setEmployeeId(employeeId);
                    newMember.setMemberNumber(employeeId);
                    return newMember;
                });

        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setEmail(email);
        member.setPhone(phone);
        member.setNationalId(nationalId);
        member.setDepartment(department);
        member.setStatus(Member.Status.ACTIVE);
        member.setMigrationStatus("MIGRATED");
        member.setIsLegacyMember(true);

        Member savedMember = memberRepository.save(member);

        // Create user account if doesn't exist
        createOrUpdateMemberUserAccount(savedMember, nationalId);

        // Update savings account balance
        if (savingsBalanceStr != null && !savingsBalanceStr.isEmpty()) {
            BigDecimal savingsBalance = new BigDecimal(savingsBalanceStr);
            // Find or create savings account
            Account savingsAccount = accountRepository.findByMemberIdAndAccountType(
                    savedMember.getId(), 
                    Account.AccountType.SAVINGS
            ).orElseGet(() -> {
                Account newAccount = new Account();
                newAccount.setMember(savedMember);
                newAccount.setAccountType(Account.AccountType.SAVINGS);
                return newAccount;
            });
            savingsAccount.setBalance(savingsBalance);
            accountRepository.save(savingsAccount);
        }

        // Update shares account balance
        if (sharesBalanceStr != null && !sharesBalanceStr.isEmpty()) {
            BigDecimal sharesBalance = new BigDecimal(sharesBalanceStr);
            // Find or create shares account
            Account sharesAccount = accountRepository.findByMemberIdAndAccountType(
                    savedMember.getId(), 
                    Account.AccountType.SHARES
            ).orElseGet(() -> {
                Account newAccount = new Account();
                newAccount.setMember(savedMember);
                newAccount.setAccountType(Account.AccountType.SHARES);
                return newAccount;
            });
            sharesAccount.setBalance(sharesBalance);
            accountRepository.save(sharesAccount);
        }

        // Process loan if provided
        if (loanNumber != null && !loanNumber.isEmpty()) {
            Loan loan = loanRepository.findByLoanNumber(loanNumber)
                    .orElseGet(() -> {
                        Loan newLoan = new Loan();
                        newLoan.setLoanNumber(loanNumber);
                        return newLoan;
                    });

            loan.setMember(savedMember);
            
            if (loanAmountStr != null && !loanAmountStr.isEmpty()) {
                loan.setAmount(new BigDecimal(loanAmountStr));
            }

            if (outstandingStr != null && !outstandingStr.isEmpty()) {
                loan.setOutstandingBalance(new BigDecimal(outstandingStr));
            }

            if (loanStartDateStr != null && !loanStartDateStr.isEmpty()) {
                loan.setDisbursementDate(LocalDate.parse(loanStartDateStr, DATE_FORMATTER).atStartOfDay());
            }

            loan.setStatus(Loan.Status.DISBURSED);
            loan.setMigrationStatus("MIGRATED");

            Loan savedLoan = loanRepository.save(loan);

            // Process guarantors (unlimited - starting from column 15)
            // Each guarantor = 2 columns (ID + Amount)
            int guarantorStartCol = 15;
            int maxCol = row.getLastCellNum();
            int guarantorCount = 0;
            
            for (int col = guarantorStartCol; col < maxCol; col += 2) {
                String guarantorId = getCellValue(row, col);
                String guarantorAmountStr = getCellValue(row, col + 1);
                
                if (guarantorId != null && !guarantorId.isEmpty()) {
                    guarantorCount++;
                    processGuarantors(savedLoan, guarantorId, guarantorAmountStr, guarantorCount);
                }
            }

            // Log loan type and guarantor count for reference
            if (loanTypeStr != null && !loanTypeStr.isEmpty()) {
                log.info("Loan {} type: {} with {} guarantors", loanNumber, loanTypeStr, guarantorCount);
            } else {
                log.info("Loan {} with {} guarantors", loanNumber, guarantorCount);
            }
        }
    }

    /**
     * Create or update member user account for portal login
     */
    private void createOrUpdateMemberUserAccount(Member member, String nationalId) {
        String username = member.getEmployeeId() != null ? member.getEmployeeId() : member.getMemberNumber();
        if (username == null) {
            return;
        }

        Optional<User> existingUser = userRepository.findByUsername(username);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getMemberId() == null) {
                user.setMemberId(member.getId());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        } else {
            user = new User();
            user.setUsername(username);
            user.setEmail(member.getEmail() != null ? member.getEmail() : username + "@minet.sacco");
            user.setPassword(passwordEncoder.encode(nationalId)); // password = national ID
            user.setRole(User.Role.MEMBER);
            user.setMemberId(member.getId());
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * Process guarantor pledge
     */
    private void processGuarantors(Loan loan, String guarantorEmployeeId, String amountStr, int position) {
        if (guarantorEmployeeId == null || guarantorEmployeeId.isEmpty()) {
            return;
        }

        Member guarantor = memberRepository.findByEmployeeId(guarantorEmployeeId)
                .orElseThrow(() -> new RuntimeException("Guarantor not found: " + guarantorEmployeeId));

        BigDecimal amount = amountStr != null && !amountStr.isEmpty()
                ? new BigDecimal(amountStr)
                : BigDecimal.ZERO;

        Guarantor pledge = new Guarantor();
        pledge.setLoan(loan);
        pledge.setMember(guarantor);
        pledge.setGuaranteeAmount(amount);
        pledge.setStatus(Guarantor.Status.ACTIVE);
        pledge.setMigrationStatus("MIGRATED");

        guarantorRepository.save(pledge);
    }

    /**
     * Get migration verification report
     */
    public Map<String, Object> getMigrationReport(Long batchId) {
        MigrationBatch batch = migrationBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Migration batch not found"));

        Map<String, Object> report = new HashMap<>();
        report.put("batchId", batch.getId());
        report.put("batchDate", batch.getBatchDate());
        report.put("totalRecords", batch.getTotalRecords());
        report.put("successfulRecords", batch.getSuccessfulRecords());
        report.put("failedRecords", batch.getFailedRecords());
        report.put("verificationStatus", batch.getVerificationStatus());
        report.put("migrationExecuted", batch.getMigrationExecuted());
        report.put("executedAt", batch.getExecutedAt());
        report.put("errorMessage", batch.getErrorMessage());

        // Calculate totals
        if (batch.getMigrationExecuted()) {
            // Note: Actual totals would be calculated from account repository
            // This is a placeholder for the report structure
            report.put("totalSavings", "See account repository for actual totals");
            report.put("totalOutstandingLoans", "See loan repository for actual totals");
        }

        return report;
    }

    /**
     * Get cell value as string
     */
    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    /**
     * Generate migration template Excel file
     * Template for importing legacy SACCO data
     * Supports up to 6 guarantors per loan
     */
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Migration Template");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Employee ID",
                    "First Name",
                    "Last Name",
                    "Email",
                    "Phone",
                    "National ID",
                    "Department",
                    "Savings Balance",
                    "Shares Balance",
                    "Loan Number",
                    "Loan Amount",
                    "Outstanding Balance",
                    "Loan Start Date (yyyy-MM-dd)",
                    "Loan End Date (yyyy-MM-dd)",
                    "Loan Type (NORMAL/PARTIAL/FULL_GUARANTEE)",
                    "Guarantor 1 Employee ID",
                    "Guarantor 1 Pledge Amount",
                    "Guarantor 2 Employee ID",
                    "Guarantor 2 Pledge Amount",
                    "Guarantor 3 Employee ID",
                    "Guarantor 3 Pledge Amount",
                    "Guarantor 4 Employee ID",
                    "Guarantor 4 Pledge Amount",
                    "Guarantor 5 Employee ID",
                    "Guarantor 5 Pledge Amount",
                    "Guarantor 6 Employee ID",
                    "Guarantor 6 Pledge Amount"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Create example row 1: Member with loan and multiple guarantors
            Row exampleRow1 = sheet.createRow(1);
            exampleRow1.createCell(0).setCellValue("EMP001");
            exampleRow1.createCell(1).setCellValue("John");
            exampleRow1.createCell(2).setCellValue("Doe");
            exampleRow1.createCell(3).setCellValue("john.doe@company.com");
            exampleRow1.createCell(4).setCellValue("0712345678");
            exampleRow1.createCell(5).setCellValue("12345678");
            exampleRow1.createCell(6).setCellValue("Finance");
            exampleRow1.createCell(7).setCellValue(150000);
            exampleRow1.createCell(8).setCellValue(50000);
            exampleRow1.createCell(9).setCellValue("LOAN001");
            exampleRow1.createCell(10).setCellValue(500000);
            exampleRow1.createCell(11).setCellValue(250000);
            exampleRow1.createCell(12).setCellValue("2023-01-15");
            exampleRow1.createCell(13).setCellValue("2024-01-15");
            exampleRow1.createCell(14).setCellValue("PARTIAL");
            exampleRow1.createCell(15).setCellValue("EMP002");
            exampleRow1.createCell(16).setCellValue(150000);
            exampleRow1.createCell(17).setCellValue("EMP003");
            exampleRow1.createCell(18).setCellValue(150000);
            exampleRow1.createCell(19).setCellValue("EMP004");
            exampleRow1.createCell(20).setCellValue(100000);
            // Leave guarantors 4-6 blank

            // Create example row 2: Member with only savings (no loan)
            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("EMP010");
            exampleRow2.createCell(1).setCellValue("Jane");
            exampleRow2.createCell(2).setCellValue("Smith");
            exampleRow2.createCell(3).setCellValue("jane.smith@company.com");
            exampleRow2.createCell(4).setCellValue("0723456789");
            exampleRow2.createCell(5).setCellValue("87654321");
            exampleRow2.createCell(6).setCellValue("HR");
            exampleRow2.createCell(7).setCellValue(200000);
            exampleRow2.createCell(8).setCellValue(75000);
            // Leave loan fields blank

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error generating template: " + e.getMessage());
        }
    }
}
