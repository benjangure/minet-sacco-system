package com.minet.sacco.service;

import com.minet.sacco.dto.DepositRequest;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BulkProcessingService {

    @Autowired
    private BulkBatchRepository bulkBatchRepository;

    @Autowired
    private BulkTransactionItemRepository bulkTransactionItemRepository;

    @Autowired
    private BulkMemberItemRepository bulkMemberItemRepository;

    @Autowired
    private BulkLoanItemRepository bulkLoanItemRepository;

    @Autowired
    private BulkDisbursementItemRepository bulkDisbursementItemRepository;

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private BulkValidationService bulkValidationService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public BulkBatch parseAndValidate(MultipartFile file, String batchType, User uploader) throws IOException {
        validateFile(file);
        String batchNumber = generateBatchNumber(batchType);

        BulkBatch batch = new BulkBatch();
        batch.setBatchNumber(batchNumber);
        batch.setBatchType(batchType);
        batch.setFileName(file.getOriginalFilename());
        batch.setUploadedBy(uploader);

        switch (batchType) {
            case "MONTHLY_CONTRIBUTIONS":
                return parseMonthlyContributions(file, batch);
            case "MEMBER_REGISTRATION":
                return parseMemberRegistration(file, batch);
            case "LOAN_APPLICATIONS":
                return parseLoanApplications(file, batch);
            case "LOAN_DISBURSEMENTS":
                return parseLoanDisbursements(file, batch);
            default:
                throw new RuntimeException("Unknown batch type: " + batchType);
        }
    }

    @Transactional
    private BulkBatch parseMonthlyContributions(MultipartFile file, BulkBatch batch) throws IOException {
        List<BulkTransactionItem> items = excelParserService.parseMonthlyContributions(file);
        List<String> allErrors = bulkValidationService.validateBatch(items);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            BulkTransactionItem item = items.get(i);
            item.setRowNumber(i + 2);
            totalAmount = totalAmount.add(item.getSavingsAmount() != null ? item.getSavingsAmount() : BigDecimal.ZERO)
                                   .add(item.getSharesAmount() != null ? item.getSharesAmount() : BigDecimal.ZERO)
                                   .add(item.getLoanRepaymentAmount() != null ? item.getLoanRepaymentAmount() : BigDecimal.ZERO)
                                   .add(item.getBenevolentFundAmount() != null ? item.getBenevolentFundAmount() : BigDecimal.ZERO)
                                   .add(item.getDevelopmentFundAmount() != null ? item.getDevelopmentFundAmount() : BigDecimal.ZERO)
                                   .add(item.getSchoolFeesAmount() != null ? item.getSchoolFeesAmount() : BigDecimal.ZERO)
                                   .add(item.getHolidayFundAmount() != null ? item.getHolidayFundAmount() : BigDecimal.ZERO)
                                   .add(item.getEmergencyFundAmount() != null ? item.getEmergencyFundAmount() : BigDecimal.ZERO);
        }

        batch.setTotalRecords(items.size());
        batch.setTotalAmount(totalAmount);
        batch.setStatus("COMPLETED");
        batch = bulkBatchRepository.save(batch);

        for (BulkTransactionItem item : items) {
            item.setBatch(batch);
            bulkTransactionItemRepository.save(item);
        }

        // Auto-process the batch immediately
        processMonthlyContributions(batch);

        auditService.logAction(batch.getUploadedBy(), "BULK_UPLOAD", "BulkBatch", batch.getId(),
            "Uploaded bulk batch: " + batch.getBatchNumber() + " with " + items.size() + " records", null, null);

        return batch;
    }

    @Transactional
    private BulkBatch parseMemberRegistration(MultipartFile file, BulkBatch batch) throws IOException {
        List<BulkMemberItem> items = excelParserService.parseMemberRegistration(file);
        List<String> allErrors = bulkValidationService.validateMemberBatch(items);

        // Fail fast if there are validation errors — prevents silent transaction rollback
        if (!allErrors.isEmpty()) {
            throw new RuntimeException(String.join("; ", allErrors));
        }
        
        batch.setTotalRecords(items.size());
        batch.setTotalAmount(BigDecimal.ZERO);
        
        // For member registration, auto-approve and process immediately (no approval needed)
        batch.setStatus("APPROVED");
        batch.setApprovedBy(batch.getUploadedBy());
        batch.setApprovedAt(LocalDateTime.now());
        batch = bulkBatchRepository.save(batch);

        for (int i = 0; i < items.size(); i++) {
            BulkMemberItem item = items.get(i);
            item.setRowNumber(i + 2);
            item.setBatch(batch);
            bulkMemberItemRepository.save(item);
        }

        auditService.logAction(batch.getUploadedBy(), "BULK_UPLOAD", "BulkBatch", batch.getId(),
            "Uploaded member registration batch: " + batch.getBatchNumber() + " with " + items.size() + " records", null, null);

        // Auto-process member registration immediately
        processApprovedBatch(batch);

        return batch;
    }

    @Transactional
    private BulkBatch parseLoanApplications(MultipartFile file, BulkBatch batch) throws IOException {
        List<BulkLoanItem> items = excelParserService.parseLoanApplications(file);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (BulkLoanItem item : items) {
            if (item.getAmount() != null) {
                totalAmount = totalAmount.add(item.getAmount());
            }
            
            // Calculate loan repayment details
            if (item.getAmount() != null && item.getTermMonths() != null) {
                // Get the loan product to get interest rate
                LoanProduct product = loanProductRepository.findByName(item.getLoanProductName()).orElse(null);
                if (product != null) {
                    BigDecimal principal = item.getAmount();
                    BigDecimal annualRate = product.getInterestRate();
                    Integer termMonths = item.getTermMonths();
                    
                    // Simple interest calculation: Interest = Principal * Rate * Time
                    BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                    BigDecimal timeInYears = BigDecimal.valueOf(termMonths).divide(BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP);
                    BigDecimal totalInterest = principal.multiply(rate).multiply(timeInYears).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal totalRepayable = principal.add(totalInterest);
                    BigDecimal monthlyRepayment = totalRepayable.divide(BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);
                    
                    item.setTotalInterest(totalInterest);
                    item.setTotalRepayable(totalRepayable);
                    item.setMonthlyRepayment(monthlyRepayment);
                }
            }
        }

        batch.setTotalRecords(items.size());
        batch.setTotalAmount(totalAmount);
        batch.setStatus("PENDING");
        batch = bulkBatchRepository.save(batch);

        for (BulkLoanItem item : items) {
            item.setBatch(batch);
            bulkLoanItemRepository.save(item);
        }

        auditService.logAction(batch.getUploadedBy(), "BULK_UPLOAD", "BulkBatch", batch.getId(),
            "Uploaded loan applications batch: " + batch.getBatchNumber() + " with " + items.size() + " records", null, null);

        return batch;
    }

    @Transactional
    private BulkBatch parseLoanDisbursements(MultipartFile file, BulkBatch batch) throws IOException {
        List<BulkDisbursementItem> items = excelParserService.parseLoanDisbursements(file);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (BulkDisbursementItem item : items) {
            if (item.getDisbursementAmount() != null) {
                totalAmount = totalAmount.add(item.getDisbursementAmount());
            }
        }

        batch.setTotalRecords(items.size());
        batch.setTotalAmount(totalAmount);
        batch.setStatus("PENDING");
        batch = bulkBatchRepository.save(batch);

        for (BulkDisbursementItem item : items) {
            item.setBatch(batch);
            bulkDisbursementItemRepository.save(item);
        }

        auditService.logAction(batch.getUploadedBy(), "BULK_UPLOAD", "BulkBatch", batch.getId(),
            "Uploaded loan disbursements batch: " + batch.getBatchNumber() + " with " + items.size() + " records", null, null);

        return batch;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String filename = file.getOriginalFilename();
        
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls") && !filename.endsWith(".csv"))) {
            throw new RuntimeException("Only Excel (.xlsx, .xls) and CSV files are allowed");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size must not exceed 5MB");
        }
    }

    private String generateBatchNumber(String batchType) {
        String prefix = "BATCH-" + batchType.substring(0, 3).toUpperCase() + "-";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + timestamp + "-" + randomPart;
    }

    private String generateMemberNumber() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "M-" + year + "-";
        
        // Find all members created in current year
        List<Member> yearMembers = memberRepository.findAll().stream()
            .filter(m -> m.getMemberNumber() != null && m.getMemberNumber().startsWith(prefix))
            .toList();
        
        // Get the highest sequence number
        int maxSequence = yearMembers.stream()
            .map(m -> {
                String[] parts = m.getMemberNumber().split("-");
                try {
                    return Integer.parseInt(parts[2]);
                } catch (Exception e) {
                    return 0;
                }
            })
            .max(Integer::compare)
            .orElse(0);
        
        // Generate next member number
        int nextSequence = maxSequence + 1;
        return prefix + String.format("%03d", nextSequence);
    }

    public BulkBatch getBatchById(Long id) {
        return bulkBatchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Batch not found"));
    }

    public List<BulkBatch> getAllBatches() {
        return bulkBatchRepository.findAll();
    }

    public List<BulkBatch> getBatchesByStatus(String status) {
        return bulkBatchRepository.findByStatus(status);
    }

    public List<BulkTransactionItem> getBatchItems(Long batchId) {
        return bulkTransactionItemRepository.findByBatchId(batchId);
    }

    public List<BulkMemberItem> getBatchMemberItems(Long batchId) {
        return bulkMemberItemRepository.findByBatchId(batchId);
    }

    public List<BulkLoanItem> getBatchLoanItems(Long batchId) {
        return bulkLoanItemRepository.findByBatchId(batchId);
    }

    public List<BulkDisbursementItem> getBatchDisbursementItems(Long batchId) {
        return bulkDisbursementItemRepository.findByBatchId(batchId);
    }

    /**
     * Get all approved loan items ready for disbursement
     */
    public List<BulkLoanItem> getApprovedLoanItems() {
        return bulkLoanItemRepository.findByStatus("APPROVED");
    }

    /**
     * Get approved loan items for a specific batch
     */
    public List<BulkLoanItem> getApprovedLoanItemsByBatch(Long batchId) {
        List<BulkLoanItem> items = bulkLoanItemRepository.findByBatchId(batchId);
        return items.stream()
            .filter(item -> "APPROVED".equals(item.getStatus()))
            .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public BulkBatch approveBatch(Long batchId, User approver) {
        BulkBatch batch = getBatchById(batchId);
        
        // Validation
        if (!batch.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING batches can be approved");
        }
        
        if (batch.getUploadedBy().getId().equals(approver.getId())) {
            throw new RuntimeException("Cannot approve your own batch (Maker-Checker rule)");
        }

        // Update batch
        batch.setStatus("APPROVED");
        batch.setApprovedBy(approver);
        batch.setApprovedAt(LocalDateTime.now());
        batch = bulkBatchRepository.save(batch);

        // Audit log
        auditService.logAction(approver, "BULK_APPROVE", "BulkBatch", batch.getId(),
            "Approved bulk batch: " + batch.getBatchNumber(), null, null);

        // Process asynchronously
        processApprovedBatch(batch);

        return batch;
    }

    @Async
    @Transactional
    public void processApprovedBatch(BulkBatch batch) {
        batch.setStatus("PROCESSING");
        batch.setProcessedAt(LocalDateTime.now());
        bulkBatchRepository.save(batch);

        switch (batch.getBatchType()) {
            case "MONTHLY_CONTRIBUTIONS":
                processMonthlyContributions(batch);
                break;
            case "MEMBER_REGISTRATION":
                processMemberRegistration(batch);
                break;
            case "LOAN_APPLICATIONS":
                processLoanApplications(batch);
                break;
            case "LOAN_DISBURSEMENTS":
                processLoanDisbursements(batch);
                break;
        }
    }

    @Transactional
    private void processMonthlyContributions(BulkBatch batch) {
        List<BulkTransactionItem> items = bulkTransactionItemRepository.findByBatchId(batch.getId());
        int successCount = 0;
        int failedCount = 0;

        for (BulkTransactionItem item : items) {
            try {
                processTransactionItem(item);
                item.setStatus("SUCCESS");
                item.setProcessedAt(LocalDateTime.now());
                successCount++;
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMessage(e.getMessage());
                item.setProcessedAt(LocalDateTime.now());
                failedCount++;
            }
            bulkTransactionItemRepository.save(item);
        }

        batch.setSuccessfulRecords(successCount);
        batch.setFailedRecords(failedCount);
        batch.setStatus("COMPLETED");
        bulkBatchRepository.save(batch);
    }

    @Transactional
    private void processMemberRegistration(BulkBatch batch) {
        List<BulkMemberItem> items = bulkMemberItemRepository.findByBatchId(batch.getId());
        int successCount = 0;
        int failedCount = 0;

        for (BulkMemberItem item : items) {
            try {
                processMemberItem(item);
                item.setStatus("SUCCESS");
                item.setProcessedAt(LocalDateTime.now());
                successCount++;
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMessage(e.getMessage());
                item.setProcessedAt(LocalDateTime.now());
                failedCount++;
            }
            bulkMemberItemRepository.save(item);
        }

        batch.setSuccessfulRecords(successCount);
        batch.setFailedRecords(failedCount);
        batch.setStatus("COMPLETED");
        bulkBatchRepository.save(batch);
    }

    @Transactional
    private void processLoanApplications(BulkBatch batch) {
        List<BulkLoanItem> items = bulkLoanItemRepository.findByBatchId(batch.getId());
        int successCount = 0;
        int failedCount = 0;

        for (BulkLoanItem item : items) {
            try {
                processLoanItem(item);
                item.setStatus("SUCCESS");
                item.setProcessedAt(LocalDateTime.now());
                successCount++;
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMessage(e.getMessage());
                item.setProcessedAt(LocalDateTime.now());
                failedCount++;
            }
            bulkLoanItemRepository.save(item);
        }

        batch.setSuccessfulRecords(successCount);
        batch.setFailedRecords(failedCount);
        batch.setStatus("COMPLETED");
        bulkBatchRepository.save(batch);
    }

    @Transactional
    private void processTransactionItem(BulkTransactionItem item) {
        Member member = memberRepository.findByMemberNumber(item.getMemberNumber())
            .orElseThrow(() -> new RuntimeException("Member not found: " + item.getMemberNumber()));

        User systemUser = item.getBatch().getApprovedBy();

        if (item.getSavingsAmount() != null && item.getSavingsAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest();
            depositRequest.setMemberId(member.getId());
            depositRequest.setAmount(item.getSavingsAmount());
            depositRequest.setAccountType("SAVINGS");
            depositRequest.setDescription("Bulk contribution - " + item.getBatch().getBatchNumber());
            accountService.deposit(depositRequest, systemUser);
        }

        if (item.getSharesAmount() != null && item.getSharesAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest();
            depositRequest.setMemberId(member.getId());
            depositRequest.setAmount(item.getSharesAmount());
            depositRequest.setAccountType("SHARES");
            depositRequest.setDescription("Bulk contribution - " + item.getBatch().getBatchNumber());
            accountService.deposit(depositRequest, systemUser);
        }

        if (item.getBenevolentFundAmount() != null && item.getBenevolentFundAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest();
            depositRequest.setMemberId(member.getId());
            depositRequest.setAmount(item.getBenevolentFundAmount());
            depositRequest.setAccountType("BENEVOLENT_FUND");
            depositRequest.setDescription("Bulk contribution - " + item.getBatch().getBatchNumber());
            accountService.deposit(depositRequest, systemUser);
        }

        if (item.getDevelopmentFundAmount() != null && item.getDevelopmentFundAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest();
            depositRequest.setMemberId(member.getId());
            depositRequest.setAmount(item.getDevelopmentFundAmount());
            depositRequest.setAccountType("DEVELOPMENT_FUND");
            depositRequest.setDescription("Bulk contribution - " + item.getBatch().getBatchNumber());
            accountService.deposit(depositRequest, systemUser);
        }

        if (item.getSchoolFeesAmount() != null && item.getSchoolFeesAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest();
            depositRequest.setMemberId(member.getId());
            depositRequest.setAmount(item.getSchoolFeesAmount());
            depositRequest.setAccountType("SCHOOL_FEES");
            depositRequest.setDescription("Bulk contribution - " + item.getBatch().getBatchNumber());
            accountService.deposit(depositRequest, systemUser);
        }

        if (item.getHolidayFundAmount() != null && item.getHolidayFundAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest();
            depositRequest.setMemberId(member.getId());
            depositRequest.setAmount(item.getHolidayFundAmount());
            depositRequest.setAccountType("HOLIDAY_FUND");
            depositRequest.setDescription("Bulk contribution - " + item.getBatch().getBatchNumber());
            accountService.deposit(depositRequest, systemUser);
        }

        if (item.getEmergencyFundAmount() != null && item.getEmergencyFundAmount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositRequest = new DepositRequest();
            depositRequest.setMemberId(member.getId());
            depositRequest.setAmount(item.getEmergencyFundAmount());
            depositRequest.setAccountType("EMERGENCY_FUND");
            depositRequest.setDescription("Bulk contribution - " + item.getBatch().getBatchNumber());
            accountService.deposit(depositRequest, systemUser);
        }

        if (item.getLoanRepaymentAmount() != null && item.getLoanRepaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (item.getLoanNumber() == null || item.getLoanNumber().isEmpty()) {
                throw new RuntimeException("Loan number required for repayment");
            }
            
            Loan loan = loanRepository.findByLoanNumber(item.getLoanNumber())
                .orElseThrow(() -> new RuntimeException("Loan not found: " + item.getLoanNumber()));
            
            LoanRepayment repayment = new LoanRepayment();
            repayment.setLoan(loan);
            repayment.setAmount(item.getLoanRepaymentAmount());
            repayment.setRepaymentDate(LocalDateTime.now());
            repayment.setCreatedBy(systemUser);
            loanRepaymentRepository.save(repayment);
            
            loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(item.getLoanRepaymentAmount()));
            if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                loan.setStatus(Loan.Status.REPAID);
            }
            
            loanRepository.save(loan);
        }
    }

    @Transactional
    private void processMemberItem(BulkMemberItem item) {
        // Check if member already exists
        if (memberRepository.findByNationalId(item.getNationalId()).isPresent()) {
            throw new RuntimeException("Member with national ID already exists: " + item.getNationalId());
        }

        Member member = new Member();
        member.setFirstName(item.getFirstName());
        member.setLastName(item.getLastName());
        member.setEmail(item.getEmail());
        member.setPhone(item.getPhone());
        member.setNationalId(item.getNationalId());
        member.setDateOfBirth(item.getDateOfBirth());
        member.setDepartment(item.getDepartment());
        member.setEmployeeId(item.getEmployeeId()); // Store employee ID properly
        member.setEmploymentStatus("PERMANENT"); // Default employment status
        member.setEmployer(item.getEmployer());
        member.setBankName(item.getBank());
        member.setBankAccountNumber(item.getBankAccount());
        member.setNextOfKinName(item.getNextOfKin());
        member.setNextOfKinPhone(item.getNokPhone());
        // Use employeeId as memberNumber (the identifier)
        String identifier = item.getEmployeeId() != null && !item.getEmployeeId().isBlank()
            ? item.getEmployeeId()
            : generateMemberNumber();
        member.setMemberNumber(identifier);
        // Members are ACTIVE immediately upon bulk upload (digitalization of existing members)
        member.setStatus(Member.Status.ACTIVE);
        member.setApprovedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());

        member = memberRepository.save(member);
        
        // Create default accounts (Savings and Shares) automatically
        createDefaultAccounts(member);

        // Create mobile app login credentials: username = memberNumber, default password = nationalId
        createMemberLoginCredentials(member);
        
        item.setMember(member);
    }

    private void createMemberLoginCredentials(Member member) {
        String username = member.getMemberNumber();
        if (username == null || userRepository.existsByUsername(username)) {
            return; // Skip if no identifier or account already exists
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(member.getEmail() != null && !member.getEmail().isBlank()
            ? member.getEmail()
            : username + "@minet.sacco");
        user.setPassword(passwordEncoder.encode(member.getNationalId()));
        user.setRole(User.Role.MEMBER);
        user.setMemberId(member.getId());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void createDefaultAccounts(Member member) {
        // Create Savings Account
        Account savingsAccount = new Account();
        savingsAccount.setMember(member);
        savingsAccount.setAccountType(Account.AccountType.SAVINGS);
        savingsAccount.setBalance(BigDecimal.ZERO);
        savingsAccount.setCreatedAt(LocalDateTime.now());
        accountRepository.save(savingsAccount);

        // Create Shares Account
        Account sharesAccount = new Account();
        sharesAccount.setMember(member);
        sharesAccount.setAccountType(Account.AccountType.SHARES);
        sharesAccount.setBalance(BigDecimal.ZERO);
        sharesAccount.setCreatedAt(LocalDateTime.now());
        accountRepository.save(sharesAccount);
    }

    @Transactional
    private void processLoanItem(BulkLoanItem item) {
        Member member = memberRepository.findByMemberNumber(item.getMemberNumber())
            .orElseThrow(() -> new RuntimeException("Member not found: " + item.getMemberNumber()));

        LoanProduct loanProduct = loanProductRepository.findByName(item.getLoanProductName())
            .orElseThrow(() -> new RuntimeException("Loan product not found: " + item.getLoanProductName()));

        if (item.getAmount().compareTo(loanProduct.getMinAmount()) < 0) {
            throw new RuntimeException("Loan amount below minimum: " + loanProduct.getMinAmount());
        }
        if (item.getAmount().compareTo(loanProduct.getMaxAmount()) > 0) {
            throw new RuntimeException("Loan amount exceeds maximum: " + loanProduct.getMaxAmount());
        }

        BigDecimal principal = item.getAmount();
        BigDecimal annualRate = loanProduct.getInterestRate();
        Integer termMonths = item.getTermMonths() != null ? item.getTermMonths() : (loanProduct.getMinTermMonths() != null ? loanProduct.getMinTermMonths() : 12);
        
        BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal timeInYears = BigDecimal.valueOf(termMonths).divide(BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal totalInterest = principal.multiply(rate).multiply(timeInYears).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalRepayable = principal.add(totalInterest);
        BigDecimal monthlyRepayment = totalRepayable.divide(BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);

        // Create Loan entity at processing time with PENDING status
        Loan loan = new Loan();
        loan.setMember(member);
        loan.setLoanProduct(loanProduct);
        loan.setAmount(principal);
        loan.setInterestRate(annualRate);
        loan.setTermMonths(termMonths);
        loan.setMonthlyRepayment(monthlyRepayment);
        loan.setTotalInterest(totalInterest);
        loan.setTotalRepayable(totalRepayable);
        loan.setOutstandingBalance(totalRepayable);
        loan.setPurpose(item.getPurpose());
        loan.setStatus(Loan.Status.PENDING);
        loan.setApplicationDate(LocalDateTime.now());
        loan.setCreatedBy(item.getBatch().getUploadedBy());
        loan.setLoanNumber(null); // Will be assigned on disbursement

        loan = loanRepository.save(loan);
        
        // Store loan reference in BulkLoanItem
        item.setLoan(loan);

        // Store calculations in BulkLoanItem for display
        item.setTotalInterest(totalInterest);
        item.setTotalRepayable(totalRepayable);
        item.setMonthlyRepayment(monthlyRepayment);

        // Add guarantors with PENDING status
        if (item.getGuarantor1() != null && !item.getGuarantor1().isEmpty()) {
            Member guarantor1 = memberRepository.findByMemberNumber(item.getGuarantor1())
                .orElseThrow(() -> new RuntimeException("Guarantor not found: " + item.getGuarantor1()));
            
            Guarantor g1 = new Guarantor();
            g1.setLoan(loan);
            g1.setMember(guarantor1);
            g1.setStatus(Guarantor.Status.PENDING);
            guarantorRepository.save(g1);
        }

        if (item.getGuarantor2() != null && !item.getGuarantor2().isEmpty()) {
            Member guarantor2 = memberRepository.findByMemberNumber(item.getGuarantor2())
                .orElseThrow(() -> new RuntimeException("Guarantor not found: " + item.getGuarantor2()));
            
            Guarantor g2 = new Guarantor();
            g2.setLoan(loan);
            g2.setMember(guarantor2);
            g2.setStatus(Guarantor.Status.PENDING);
            guarantorRepository.save(g2);
        }

        if (item.getGuarantor3() != null && !item.getGuarantor3().isEmpty()) {
            Member guarantor3 = memberRepository.findByMemberNumber(item.getGuarantor3())
                .orElseThrow(() -> new RuntimeException("Guarantor not found: " + item.getGuarantor3()));
            
            Guarantor g3 = new Guarantor();
            g3.setLoan(loan);
            g3.setMember(guarantor3);
            g3.setStatus(Guarantor.Status.PENDING);
            guarantorRepository.save(g3);
        }
    }

    @Transactional
    public BulkBatch rejectBatch(Long batchId, User rejector, String reason) {
        BulkBatch batch = getBatchById(batchId);
        
        if (!batch.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING batches can be rejected");
        }

        batch.setStatus("REJECTED");
        batch.setApprovedBy(rejector);
        batch.setApprovedAt(LocalDateTime.now());
        batch = bulkBatchRepository.save(batch);

        // Audit log
        auditService.logAction(rejector, "BULK_REJECT", "BulkBatch", batch.getId(),
            "Rejected bulk batch: " + batch.getBatchNumber() + " - Reason: " + reason, null, null);

        return batch;
    }

    @Transactional
    private void processLoanDisbursements(BulkBatch batch) {
        List<BulkDisbursementItem> items = bulkDisbursementItemRepository.findByBatchId(batch.getId());
        int successCount = 0;
        int failedCount = 0;

        for (BulkDisbursementItem item : items) {
            try {
                processDisbursementItem(item);
                item.setStatus("SUCCESS");
                item.setProcessedAt(LocalDateTime.now());
                successCount++;
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMessage(e.getMessage());
                item.setProcessedAt(LocalDateTime.now());
                failedCount++;
            }
            bulkDisbursementItemRepository.save(item);
        }

        batch.setSuccessfulRecords(successCount);
        batch.setFailedRecords(failedCount);
        batch.setStatus(failedCount == 0 ? "COMPLETED" : "PARTIALLY_COMPLETED");
        bulkBatchRepository.save(batch);
    }

    @Transactional
    private void processDisbursementItem(BulkDisbursementItem item) {
        Loan loan = loanRepository.findByLoanNumber(item.getLoanNumber())
            .orElseThrow(() -> new RuntimeException("Loan not found: " + item.getLoanNumber()));

        if (loan.getStatus() != Loan.Status.APPROVED) {
            throw new RuntimeException("Loan must be APPROVED before disbursement");
        }

        // Update loan status to DISBURSED
        loan.setStatus(Loan.Status.DISBURSED);
        loan.setDisbursementDate(LocalDateTime.now());
        Loan updatedLoan = loanRepository.save(loan);

        // Credit member's account
        Account account = accountRepository.findByMemberIdAndAccountType(
                updatedLoan.getMember().getId(), 
                Account.AccountType.valueOf(item.getDisbursementAccount()))
            .orElseGet(() -> {
                Account newAccount = new Account();
                newAccount.setMember(updatedLoan.getMember());
                newAccount.setAccountType(Account.AccountType.valueOf(item.getDisbursementAccount()));
                newAccount.setBalance(BigDecimal.ZERO);
                newAccount.setCreatedAt(LocalDateTime.now());
                return accountRepository.save(newAccount);
            });

        account.setBalance(account.getBalance().add(item.getDisbursementAmount()));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(Transaction.TransactionType.LOAN_DISBURSEMENT);
        transaction.setAmount(item.getDisbursementAmount());
        transaction.setDescription("Loan disbursement - Loan: " + item.getLoanNumber());
        transaction.setCreatedBy(item.getBatch().getApprovedBy());
        transactionRepository.save(transaction);
    }

    @Autowired
    private GuarantorValidationService guarantorValidationService;

    @Autowired
    private LoanEligibilityValidator loanEligibilityValidator;

    @Autowired
    private LoanDisbursementService loanDisbursementService;

    @Autowired
    private NotificationService notificationService;

    public Map<String, Object> validateBulkLoanItemGuarantors(Long itemId) {
        BulkLoanItem item = bulkLoanItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Loan item not found"));

        System.out.println("DEBUG: Validating loan item " + itemId + " for member " + item.getMemberNumber());
        System.out.println("DEBUG: Loan amount from item: " + item.getAmount());

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", itemId);
        result.put("memberNumber", item.getMemberNumber());
        result.put("loanAmount", item.getAmount());
        result.put("loanProductName", item.getLoanProductName());
        result.put("purpose", item.getPurpose());

        // Step 1: Validate loan product is enabled
        LoanProduct loanProduct = loanProductRepository.findByName(item.getLoanProductName())
            .orElseThrow(() -> new RuntimeException("Loan product not found: " + item.getLoanProductName()));
        
        if (loanProduct.getIsActive() == null || !loanProduct.getIsActive()) {
            result.put("productEnabled", false);
            result.put("productError", "Loan product '" + item.getLoanProductName() + "' is not enabled by admin");
            return result;
        }
        
        result.put("productEnabled", true);

        // Get member information
        Member member = memberRepository.findByMemberNumber(item.getMemberNumber())
            .orElseThrow(() -> new RuntimeException("Member not found: " + item.getMemberNumber()));

        Map<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("memberId", member.getId());
        memberInfo.put("memberName", member.getFirstName() + " " + member.getLastName());
        memberInfo.put("status", member.getStatus());
        memberInfo.put("email", member.getEmail());
        memberInfo.put("phone", member.getPhone());

        // Get member's accounts
        Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SAVINGS);
        Optional<Account> sharesAccount = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SHARES);

        BigDecimal savingsBalance = savingsAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
        BigDecimal sharesBalance = sharesAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
        // For member eligibility, only SAVINGS count (not shares)
        // Shares are capital contributions and don't count toward loan capacity
        BigDecimal totalBalance = savingsBalance;

        System.out.println("DEBUG: Member " + member.getFirstName() + " - Savings: " + savingsBalance + ", Shares: " + sharesBalance + ", Total: " + totalBalance);

        memberInfo.put("savingsBalance", savingsBalance);
        memberInfo.put("sharesBalance", sharesBalance);
        memberInfo.put("totalBalance", totalBalance);

        // Get member's loan history
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());
        BigDecimal totalOutstanding = memberLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || loan.getStatus() == Loan.Status.APPROVED)
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long defaultedLoans = memberLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DEFAULTED)
                .count();

        long activeLoans = memberLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || loan.getStatus() == Loan.Status.APPROVED)
                .count();

        memberInfo.put("totalOutstandingBalance", totalOutstanding);
        memberInfo.put("defaultedLoans", defaultedLoans);
        memberInfo.put("activeLoans", activeLoans);
        memberInfo.put("totalLoans", memberLoans.size());

        // Check member eligibility using the same method as approveLoanItem
        Map<String, Object> memberEligibilityCheck = validateMemberEligibility(member, item.getAmount());
        boolean memberEligible = (boolean) memberEligibilityCheck.get("isEligible");
        List<String> memberErrors = (List<String>) memberEligibilityCheck.get("errors");
        List<String> memberWarnings = (List<String>) memberEligibilityCheck.get("warnings");

        memberInfo.put("isEligible", memberEligible);
        memberInfo.put("errors", memberErrors);
        memberInfo.put("warnings", memberWarnings);

        result.put("memberInfo", memberInfo);

        // Collect guarantor IDs
        List<Long> guarantorIds = new ArrayList<>();
        List<String> guarantorNames = new ArrayList<>();

        if (item.getGuarantor1() != null && !item.getGuarantor1().isEmpty()) {
            Member g1 = memberRepository.findByMemberNumber(item.getGuarantor1())
                .orElseThrow(() -> new RuntimeException("Guarantor 1 not found: " + item.getGuarantor1()));
            guarantorIds.add(g1.getId());
            guarantorNames.add(g1.getFirstName() + " " + g1.getLastName());
        }

        if (item.getGuarantor2() != null && !item.getGuarantor2().isEmpty()) {
            Member g2 = memberRepository.findByMemberNumber(item.getGuarantor2())
                .orElseThrow(() -> new RuntimeException("Guarantor 2 not found: " + item.getGuarantor2()));
            guarantorIds.add(g2.getId());
            guarantorNames.add(g2.getFirstName() + " " + g2.getLastName());
        }

        result.put("guarantorCount", guarantorIds.size());
        result.put("guarantorNames", guarantorNames);

        // Validate all guarantors
        List<GuarantorValidationService.GuarantorValidationResult> validationResults = 
            guarantorValidationService.validateAllGuarantors(guarantorIds, item.getAmount());

        System.out.println("DEBUG: Guarantor validation results:");
        for (GuarantorValidationService.GuarantorValidationResult vr : validationResults) {
            System.out.println("  - " + vr.getGuarantorName() + ": Eligible=" + vr.isEligible() + ", Total Balance=" + vr.getTotalBalance());
        }

        result.put("validationResults", validationResults);
        // Don't set allGuarantorsEligible here - let frontend calculate from individual results
        result.put("validationSummary", guarantorValidationService.getValidationSummary(validationResults));

        return result;
    }

    @Transactional
    public Map<String, Object> approveLoanItem(Long itemId, User approver) {
        BulkLoanItem item = bulkLoanItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Loan item not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", itemId);

        // Get or create the Loan entity
        Loan loan = item.getLoan();
        if (loan == null) {
            // Create a new Loan entity from the bulk item
            Member member = memberRepository.findByMemberNumber(item.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found: " + item.getMemberNumber()));
            
            LoanProduct product = loanProductRepository.findByName(item.getLoanProductName())
                .orElseThrow(() -> new RuntimeException("Loan product not found: " + item.getLoanProductName()));
            
            loan = new Loan();
            loan.setMember(member);
            loan.setLoanProduct(product);
            loan.setAmount(item.getAmount());
            loan.setInterestRate(product.getInterestRate());
            loan.setTermMonths(item.getTermMonths());
            loan.setPurpose(item.getPurpose());
            loan.setStatus(Loan.Status.PENDING);
            loan.setApplicationDate(LocalDateTime.now());
            loan.setCreatedBy(approver);
            
            // Calculate repayment details
            loan.calculateRepaymentDetails();
            
            loan = loanRepository.save(loan);
            item.setLoan(loan);
            bulkLoanItemRepository.save(item);
        }

        // Step 1: Validate member eligibility
        Member member = memberRepository.findByMemberNumber(item.getMemberNumber())
            .orElseThrow(() -> new RuntimeException("Member not found: " + item.getMemberNumber()));

        Map<String, Object> memberEligibilityCheck = validateMemberEligibility(member, item.getAmount());
        boolean memberEligible = (boolean) memberEligibilityCheck.get("isEligible");
        List<String> memberErrors = (List<String>) memberEligibilityCheck.get("errors");

        // Store member eligibility in Loan entity
        loan.setMemberEligibilityStatus(memberEligible ? "ELIGIBLE" : "INELIGIBLE");
        if (!memberErrors.isEmpty()) {
            loan.setMemberEligibilityErrors(String.join("; ", memberErrors));
        }

        result.put("memberEligibility", memberEligibilityCheck);

        // Step 1.5: Validate loan product is enabled
        LoanProduct loanProduct = loanProductRepository.findByName(item.getLoanProductName())
            .orElseThrow(() -> new RuntimeException("Loan product not found: " + item.getLoanProductName()));
        
        if (loanProduct.getIsActive() == null || !loanProduct.getIsActive()) {
            item.setStatus("REJECTED");
            item.setErrorMessage("Loan product is not enabled: " + item.getLoanProductName());
            bulkLoanItemRepository.save(item);
            
            result.put("status", "REJECTED");
            result.put("reason", "Loan product not enabled");
            result.put("details", java.util.Arrays.asList("The loan product '" + item.getLoanProductName() + "' is not currently enabled by the admin"));
            result.put("rejectedBy", approver.getUsername());
            result.put("rejectedAt", LocalDateTime.now());
            
            return result;
        }

        // If member is not eligible, reject the loan
        if (!memberEligible) {
            item.setStatus("REJECTED");
            item.setErrorMessage("Member not eligible: " + String.join("; ", memberErrors));
            bulkLoanItemRepository.save(item);

            result.put("status", "REJECTED");
            result.put("reason", "Member eligibility check failed");
            result.put("details", memberErrors);
            result.put("rejectedBy", approver.getUsername());
            result.put("rejectedAt", LocalDateTime.now());

            return result;
        }

        // Step 2: Validate guarantor eligibility
        List<Long> guarantorIds = new ArrayList<>();
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        
        for (Guarantor g : guarantors) {
            guarantorIds.add(g.getMember().getId());
        }

        List<GuarantorValidationService.GuarantorValidationResult> guarantorResults = 
            guarantorValidationService.validateAllGuarantors(guarantorIds, item.getAmount());

        // Store guarantor eligibility in Loan entity
        for (int i = 0; i < guarantorResults.size() && i < 3; i++) {
            GuarantorValidationService.GuarantorValidationResult gResult = guarantorResults.get(i);
            String status = gResult.isEligible() ? "ELIGIBLE" : "INELIGIBLE";
            String errors = gResult.getErrors().isEmpty() ? null : String.join("; ", gResult.getErrors());
            
            if (i == 0) {
                loan.setGuarantor1EligibilityStatus(status);
                loan.setGuarantor1EligibilityErrors(errors);
                if (guarantors.size() > 0) {
                    guarantors.get(0).setStatus(gResult.isEligible() ? Guarantor.Status.ACCEPTED : Guarantor.Status.REJECTED);
                    guarantorRepository.save(guarantors.get(0));
                }
            } else if (i == 1) {
                loan.setGuarantor2EligibilityStatus(status);
                loan.setGuarantor2EligibilityErrors(errors);
                if (guarantors.size() > 1) {
                    guarantors.get(1).setStatus(gResult.isEligible() ? Guarantor.Status.ACCEPTED : Guarantor.Status.REJECTED);
                    guarantorRepository.save(guarantors.get(1));
                }
            } else if (i == 2) {
                loan.setGuarantor3EligibilityStatus(status);
                loan.setGuarantor3EligibilityErrors(errors);
                if (guarantors.size() > 2) {
                    guarantors.get(2).setStatus(gResult.isEligible() ? Guarantor.Status.ACCEPTED : Guarantor.Status.REJECTED);
                    guarantorRepository.save(guarantors.get(2));
                }
            }
        }

        result.put("guarantorValidation", guarantorResults);

        // Check guarantor eligibility
        long eligibleGuarantors = guarantorResults.stream().filter(GuarantorValidationService.GuarantorValidationResult::isEligible).count();
        long totalGuarantors = guarantorResults.size();

        // Step 3: Determine approval decision
        String approvalDecision;
        if (eligibleGuarantors == totalGuarantors) {
            // All guarantors eligible - APPROVE
            approvalDecision = "APPROVED";
            item.setStatus("APPROVED");
            loan.setStatus(Loan.Status.APPROVED);
            loan.setApprovalDate(LocalDateTime.now());
            loan.setApprovedBy(approver);
        } else if (eligibleGuarantors > 0) {
            // Some guarantors eligible - CONDITIONAL
            approvalDecision = "CONDITIONAL_APPROVAL";
            item.setStatus("CONDITIONAL_APPROVAL");
            loan.setStatus(Loan.Status.APPROVED);
            loan.setApprovalDate(LocalDateTime.now());
            loan.setApprovedBy(approver);
        } else {
            // No guarantors eligible - REJECT
            approvalDecision = "REJECTED";
            item.setStatus("REJECTED");
            List<String> guarantorErrors = new ArrayList<>();
            for (GuarantorValidationService.GuarantorValidationResult gr : guarantorResults) {
                if (!gr.isEligible()) {
                    guarantorErrors.addAll(gr.getErrors());
                }
            }
            item.setErrorMessage("All guarantors ineligible: " + String.join("; ", guarantorErrors));
        }

        bulkLoanItemRepository.save(item);
        loanRepository.save(loan);

        result.put("status", approvalDecision);
        result.put("eligibleGuarantors", eligibleGuarantors);
        result.put("totalGuarantors", totalGuarantors);
        result.put("approvedBy", approver.getUsername());
        result.put("approvedAt", LocalDateTime.now());

        // Send notification to Treasurer
        String notificationMessage = "Loan application for member " + member.getMemberNumber() + 
            " (" + member.getFirstName() + " " + member.getLastName() + ") has been " + 
            approvalDecision.toLowerCase() + " by " + approver.getUsername();
        notificationService.notifyUsersByRole("TREASURER", notificationMessage, "LOAN_APPROVAL");

        return result;
    }

    /**
     * Validate member eligibility for loan application
     */
    private Map<String, Object> validateMemberEligibility(Member member, BigDecimal loanAmount) {
        LoanEligibilityValidator.EligibilityResult result = 
                loanEligibilityValidator.validateMemberEligibility(member, loanAmount);
        
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isEligible", result.isEligible());
        resultMap.put("errors", result.getErrors());
        resultMap.put("warnings", result.getWarnings());
        resultMap.put("memberName", member.getFirstName() + " " + member.getLastName());
        resultMap.put("savingsBalance", result.getSavingsBalance());
        resultMap.put("sharesBalance", result.getSharesBalance());
        resultMap.put("totalBalance", result.getTotalBalance());
        resultMap.put("activeLoans", result.getActiveLoans());
        resultMap.put("defaultedLoans", result.getDefaultedLoans());

        return resultMap;
    }

    @Transactional
    public Map<String, Object> rejectLoanItem(Long itemId, User rejector, String reason) {
        BulkLoanItem item = bulkLoanItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Loan item not found"));

        item.setStatus("REJECTED");
        item.setErrorMessage(reason);
        bulkLoanItemRepository.save(item);

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", itemId);
        result.put("status", "REJECTED");
        result.put("reason", reason);
        result.put("rejectedBy", rejector.getUsername());
        result.put("rejectedAt", LocalDateTime.now());

        return result;
    }


    /**
     * Bulk disburse approved loans using existing Loan entities
     */
    @Transactional
    public Map<String, Object> bulkDisburseLoanItems(List<Long> itemIds, User treasurer) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> successfulDisbursements = new ArrayList<>();
        List<Map<String, Object>> failedDisbursements = new ArrayList<>();
        
        for (Long itemId : itemIds) {
            try {
                BulkLoanItem item = bulkLoanItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Loan item not found: " + itemId));
                
                if (!"APPROVED".equals(item.getStatus()) && !"CONDITIONAL_APPROVAL".equals(item.getStatus())) {
                    throw new RuntimeException("Loan item is not APPROVED. Current status: " + item.getStatus());
                }
                
                // Get existing Loan entity
                Loan loan = item.getLoan();
                if (loan == null) {
                    throw new RuntimeException("Loan entity not found for this item");
                }
                
                // Use consolidated disbursement service
                Loan disbursedLoan = loanDisbursementService.disburseLoan(loan, treasurer);
                disbursedLoan.setDisbursedBy(treasurer);
                loanRepository.save(disbursedLoan);
                
                // Update bulk item status
                item.setStatus("DISBURSED");
                item.setProcessedAt(LocalDateTime.now());
                bulkLoanItemRepository.save(item);
                
                Map<String, Object> success = new HashMap<>();
                success.put("itemId", itemId);
                success.put("memberNumber", item.getMemberNumber());
                success.put("loanAmount", item.getAmount());
                success.put("loanId", disbursedLoan.getId());
                success.put("loanNumber", disbursedLoan.getLoanNumber());
                success.put("status", "DISBURSED");
                successfulDisbursements.add(success);
                
            } catch (Exception e) {
                Map<String, Object> failed = new HashMap<>();
                failed.put("itemId", itemId);
                failed.put("status", "FAILED");
                failed.put("error", e.getMessage());
                failedDisbursements.add(failed);
            }
        }
        
        result.put("successfulCount", successfulDisbursements.size());
        result.put("failedCount", failedDisbursements.size());
        result.put("successfulDisbursements", successfulDisbursements);
        result.put("failedDisbursements", failedDisbursements);
        result.put("totalProcessed", itemIds.size());
        
        return result;
    }

    @Transactional
    public Map<String, Object> bulkApproveLoanItems(List<Long> itemIds, User approver) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> approvedItems = new ArrayList<>();
        List<Map<String, Object>> flaggedItems = new ArrayList<>();
        
        for (Long itemId : itemIds) {
            try {
                BulkLoanItem item = bulkLoanItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Loan item not found: " + itemId));
                
                // Step 1: Validate loan product is enabled
                LoanProduct loanProduct = loanProductRepository.findByName(item.getLoanProductName())
                    .orElseThrow(() -> new RuntimeException("Loan product not found: " + item.getLoanProductName()));
                
                if (loanProduct.getIsActive() == null || !loanProduct.getIsActive()) {
                    // Product not enabled - flag this item
                    Map<String, Object> flagged = new HashMap<>();
                    flagged.put("itemId", itemId);
                    flagged.put("memberNumber", item.getMemberNumber());
                    flagged.put("loanAmount", item.getAmount());
                    flagged.put("status", "FLAGGED");
                    flagged.put("reason", "Loan product not enabled");
                    flagged.put("details", java.util.Arrays.asList("The loan product '" + item.getLoanProductName() + "' is not currently enabled by the admin"));
                    item.setStatus("FLAGGED");
                    bulkLoanItemRepository.save(item);
                    flaggedItems.add(flagged);
                    continue;
                }
                
                // Step 2: Validate member eligibility
                Member member = memberRepository.findByMemberNumber(item.getMemberNumber())
                    .orElseThrow(() -> new RuntimeException("Member not found: " + item.getMemberNumber()));
                
                Map<String, Object> memberEligibilityCheck = validateMemberEligibility(member, item.getAmount());
                boolean memberEligible = (boolean) memberEligibilityCheck.get("isEligible");
                
                if (!memberEligible) {
                    // Member not eligible - flag this item
                    Map<String, Object> flagged = new HashMap<>();
                    flagged.put("itemId", itemId);
                    flagged.put("memberNumber", item.getMemberNumber());
                    flagged.put("loanAmount", item.getAmount());
                    flagged.put("status", "FLAGGED");
                    flagged.put("reason", "Member eligibility check failed");
                    flagged.put("details", memberEligibilityCheck.get("errors"));
                    item.setStatus("FLAGGED");
                    bulkLoanItemRepository.save(item);
                    flaggedItems.add(flagged);
                    continue;
                }
                
                // Step 3: Validate guarantor eligibility
                List<Long> guarantorIds = new ArrayList<>();
                if (item.getGuarantor1() != null && !item.getGuarantor1().isEmpty()) {
                    Member g1 = memberRepository.findByMemberNumber(item.getGuarantor1())
                        .orElseThrow(() -> new RuntimeException("Guarantor 1 not found: " + item.getGuarantor1()));
                    guarantorIds.add(g1.getId());
                }
                if (item.getGuarantor2() != null && !item.getGuarantor2().isEmpty()) {
                    Member g2 = memberRepository.findByMemberNumber(item.getGuarantor2())
                        .orElseThrow(() -> new RuntimeException("Guarantor 2 not found: " + item.getGuarantor2()));
                    guarantorIds.add(g2.getId());
                }
                
                List<GuarantorValidationService.GuarantorValidationResult> guarantorResults = 
                    guarantorValidationService.validateAllGuarantors(guarantorIds, item.getAmount());
                
                long eligibleGuarantors = guarantorResults.stream()
                    .filter(GuarantorValidationService.GuarantorValidationResult::isEligible)
                    .count();
                long totalGuarantors = guarantorResults.size();
                
                if (eligibleGuarantors == totalGuarantors) {
                    // All guarantors eligible - APPROVE
                    item.setStatus("APPROVED");
                    bulkLoanItemRepository.save(item);
                    
                    Map<String, Object> approved = new HashMap<>();
                    approved.put("itemId", itemId);
                    approved.put("memberNumber", item.getMemberNumber());
                    approved.put("loanAmount", item.getAmount());
                    approved.put("status", "APPROVED");
                    approved.put("approvedBy", approver.getUsername());
                    approved.put("approvedAt", LocalDateTime.now());
                    approvedItems.add(approved);
                } else if (eligibleGuarantors > 0) {
                    // Some guarantors eligible - FLAG for review
                    item.setStatus("FLAGGED");
                    bulkLoanItemRepository.save(item);
                    
                    Map<String, Object> flagged = new HashMap<>();
                    flagged.put("itemId", itemId);
                    flagged.put("memberNumber", item.getMemberNumber());
                    flagged.put("loanAmount", item.getAmount());
                    flagged.put("status", "FLAGGED");
                    flagged.put("reason", "Partial guarantor eligibility");
                    flagged.put("eligibleGuarantors", eligibleGuarantors);
                    flagged.put("totalGuarantors", totalGuarantors);
                    flaggedItems.add(flagged);
                } else {
                    // No guarantors eligible - FLAG
                    item.setStatus("FLAGGED");
                    bulkLoanItemRepository.save(item);
                    
                    Map<String, Object> flagged = new HashMap<>();
                    flagged.put("itemId", itemId);
                    flagged.put("memberNumber", item.getMemberNumber());
                    flagged.put("loanAmount", item.getAmount());
                    flagged.put("status", "FLAGGED");
                    flagged.put("reason", "No eligible guarantors");
                    flaggedItems.add(flagged);
                }
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("itemId", itemId);
                error.put("status", "ERROR");
                error.put("error", e.getMessage());
                flaggedItems.add(error);
            }
        }
        
        result.put("approvedCount", approvedItems.size());
        result.put("flaggedCount", flaggedItems.size());
        result.put("approvedItems", approvedItems);
        result.put("flaggedItems", flaggedItems);
        result.put("totalProcessed", itemIds.size());
        
        return result;
    }
}


