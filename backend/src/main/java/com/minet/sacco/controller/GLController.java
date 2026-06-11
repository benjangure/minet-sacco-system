package com.minet.sacco.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.minet.sacco.dto.*;
import com.minet.sacco.service.GLCalculationService;
import com.minet.sacco.service.BalanceSheetService;
import com.minet.sacco.service.IncomeStatementService;
import com.minet.sacco.service.ReportExportService;
import com.minet.sacco.service.GLManualEntryService;
import com.minet.sacco.service.UserService;
import com.minet.sacco.service.AuditService;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.GLAccountRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

@RestController
@RequestMapping("/api/gl")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GLController {
  private static final Logger logger = LoggerFactory.getLogger(GLController.class);
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private BalanceSheetService balanceSheetService;
  
  @Autowired
  private IncomeStatementService incomeStatementService;
  
  @Autowired
  private ReportExportService reportExportService;
  
  @Autowired
  private GLManualEntryService glManualEntryService;
  
  @Autowired
  private UserService userService;
  
  @Autowired
  private AuditService auditService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  /**
   * Get all active GL accounts
   */
  @GetMapping("/accounts")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<List<GLAccount>>> getAllAccounts() {
    List<GLAccount> accounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
    logger.info("Retrieved " + accounts.size() + " active GL accounts");
    return ResponseEntity.ok(ApiResponse.success("GL Accounts retrieved", accounts));
  }
  
  /**
   * Get GL account by code
   */
  @GetMapping("/accounts/{code}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<GLAccount>> getAccount(@PathVariable String code) {
    GLAccount account = glAccountRepository.findByCode(code)
      .orElseThrow(() -> new RuntimeException("GL Account not found: " + code));
    return ResponseEntity.ok(ApiResponse.success("GL Account retrieved", account));
  }
  
  /**
   * Create new GL account (Admin only)
   */
  @PostMapping("/accounts")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLAccount>> createAccount(@RequestBody GLAccount account) {
    // Validate unique code
    if (glAccountRepository.findByCode(account.getCode()).isPresent()) {
      throw new RuntimeException("GL Account code already exists: " + account.getCode());
    }
    
    GLAccount saved = glAccountRepository.save(account);
    logger.info("GL Account created: " + saved.getCode());
    return ResponseEntity.ok(ApiResponse.success("GL Account created", saved));
  }
  
  /**
   * Update GL account
   */
  @PutMapping("/accounts/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLAccount>> updateAccount(
    @PathVariable Integer id,
    @RequestBody GLAccount updates
  ) {
    GLAccount account = glAccountRepository.findById(id)
      .orElseThrow(() -> new RuntimeException("GL Account not found"));
    
    account.setName(updates.getName());
    account.setCalculationConfig(updates.getCalculationConfig());
    account.setDisplayOrder(updates.getDisplayOrder());
    account.setBalanceCalculationType(updates.getBalanceCalculationType());
    
    GLAccount saved = glAccountRepository.save(account);
    logger.info("GL Account updated: " + saved.getCode());
    return ResponseEntity.ok(ApiResponse.success("GL Account updated", saved));
  }
  
  /**
   * Generate Trial Balance Report - grouped by section
   */
  @GetMapping("/trial-balance")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<ApiResponse<GLTrialBalanceReportDTO>> generateTrialBalance(
    @RequestParam(required = false) LocalDate asOfDate,
    @RequestParam(required = false) Integer periodMonth,
    @RequestParam(required = false) Integer periodYear
  ) {
    try {
      if (asOfDate == null) {
        asOfDate = LocalDate.now();
      }
      
      // Default to current period if not specified
      if (periodMonth == null) {
        periodMonth = asOfDate.getMonthValue();
      }
      if (periodYear == null) {
        periodYear = asOfDate.getYear();
      }
      
      // Get trial balance using GL calculation service with period filtering
      GLTrialBalanceReportDTO report = buildGroupedTrialBalance(
        asOfDate, periodMonth, periodYear);
      
      logger.info("Trial Balance generated as of " + asOfDate + 
                  " for period " + periodMonth + "/" + periodYear + 
                  ". Sections: " + report.getSections().size());
      
      return ResponseEntity.ok(ApiResponse.success("Trial Balance generated", report));
    } catch (Exception e) {
      logger.error("Error generating trial balance", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to generate trial balance: " + e.getMessage()));
    }
  }
  
  /**
   * Get balance for a single GL account as of date
   */
  @GetMapping("/accounts/{id}/balance")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<BalanceDTO>> getAccountBalance(
    @PathVariable Integer id,
    @RequestParam(required = false) LocalDate asOfDate
  ) {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    
    BigDecimal balance = glCalculationService.calculateGLAccountBalance(id, asOfDate);
    return ResponseEntity.ok(ApiResponse.success(
      "Balance calculated",
      new BalanceDTO(balance, asOfDate)
    ));
  }
  
  /**
   * Generate Balance Sheet Report
   */
  @GetMapping("/balance-sheet")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<ApiResponse<BalanceSheetDTO>> generateBalanceSheet(
    @RequestParam(required = false) LocalDate asOfDate,
    @RequestParam(required = false) Integer periodMonth,
    @RequestParam(required = false) Integer periodYear
  ) {
    try {
      if (asOfDate == null) {
        asOfDate = LocalDate.now();
      }
      
      // Default to current period if not specified
      if (periodMonth == null) {
        periodMonth = asOfDate.getMonthValue();
      }
      if (periodYear == null) {
        periodYear = asOfDate.getYear();
      }
      
      BalanceSheetDTO report = balanceSheetService.generateBalanceSheet(asOfDate, periodMonth, periodYear);
      logger.info("Balance Sheet generated as of " + asOfDate + 
                  " for period " + periodMonth + "/" + periodYear + 
                  ". Balanced: " + report.getIsBalanced() + 
                  ", Assets: " + report.getTotalAssets() + 
                  ", Liabilities: " + report.getTotalLiabilities() + 
                  ", Equity: " + report.getTotalEquity());
      
      return ResponseEntity.ok(ApiResponse.success("Balance Sheet generated", report));
    } catch (Exception e) {
      logger.error("Error generating balance sheet", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to generate balance sheet: " + e.getMessage()));
    }
  }
  
  /**
   * Generate Income Statement Report
   */
  @GetMapping("/income-statement")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<ApiResponse<IncomeStatementDTO>> generateIncomeStatement(
    @RequestParam(required = false) LocalDate fromDate,
    @RequestParam(required = false) LocalDate toDate,
    @RequestParam(required = false) Integer periodMonth,
    @RequestParam(required = false) Integer periodYear
  ) {
    try {
      if (toDate == null) {
        toDate = LocalDate.now();
      }
      
      // Default to current period if not specified
      if (periodMonth == null) {
        periodMonth = toDate.getMonthValue();
      }
      if (periodYear == null) {
        periodYear = toDate.getYear();
      }
      
      IncomeStatementDTO report = incomeStatementService.generateIncomeStatement(fromDate, toDate, periodMonth, periodYear);
      logger.info("Income Statement generated from " + report.getFromDate() + " to " + report.getToDate() + 
                  " for period " + periodMonth + "/" + periodYear +
                  ". NetIncome: " + report.getNetIncome());
      
      return ResponseEntity.ok(ApiResponse.success("Income Statement generated", report));
    } catch (Exception e) {
      logger.error("Error generating income statement", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to generate income statement: " + e.getMessage()));
    }
  }

  // ===== TRIAL BALANCE EXPORT ENDPOINTS =====
  @GetMapping("/trial-balance/export/excel")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<byte[]> exportTrialBalanceExcel(
    @RequestParam(required = false) LocalDate asOfDate
  ) throws Exception {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    TrialBalanceDTO report = glCalculationService.generateTrialBalance(asOfDate);
    byte[] excelFile = reportExportService.exportTrialBalanceToExcel(report);
    
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trial_balance_" + LocalDate.now() + ".xlsx")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(excelFile);
  }

  @GetMapping("/trial-balance/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<byte[]> exportTrialBalancePdf(
    @RequestParam(required = false) LocalDate asOfDate
  ) throws Exception {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    TrialBalanceDTO report = glCalculationService.generateTrialBalance(asOfDate);
    byte[] pdfFile = reportExportService.exportTrialBalanceToPdf(report);
    
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trial_balance_" + LocalDate.now() + ".pdf")
      .contentType(MediaType.APPLICATION_PDF)
      .body(pdfFile);
  }

  // ===== BALANCE SHEET EXPORT ENDPOINTS =====
  @GetMapping("/balance-sheet/export/excel")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<byte[]> exportBalanceSheetExcel(
    @RequestParam(required = false) LocalDate asOfDate
  ) throws Exception {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    BalanceSheetDTO report = balanceSheetService.generateBalanceSheet(asOfDate);
    byte[] excelFile = reportExportService.exportBalanceSheetToExcel(report);
    
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_sheet_" + LocalDate.now() + ".xlsx")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(excelFile);
  }

  @GetMapping("/balance-sheet/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<byte[]> exportBalanceSheetPdf(
    @RequestParam(required = false) LocalDate asOfDate
  ) throws Exception {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    BalanceSheetDTO report = balanceSheetService.generateBalanceSheet(asOfDate);
    byte[] pdfFile = reportExportService.exportBalanceSheetToPdf(report);
    
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_sheet_" + LocalDate.now() + ".pdf")
      .contentType(MediaType.APPLICATION_PDF)
      .body(pdfFile);
  }

  // ===== INCOME STATEMENT EXPORT ENDPOINTS =====
  @GetMapping("/income-statement/export/excel")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<byte[]> exportIncomeStatementExcel(
    @RequestParam(required = false) LocalDate fromDate,
    @RequestParam(required = false) LocalDate toDate
  ) throws Exception {
    if (toDate == null) {
      toDate = LocalDate.now();
    }
    IncomeStatementDTO report = incomeStatementService.generateIncomeStatement(fromDate, toDate);
    byte[] excelFile = reportExportService.exportIncomeStatementToExcel(report);
    
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=income_statement_" + LocalDate.now() + ".xlsx")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(excelFile);
  }

  @GetMapping("/income-statement/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<byte[]> exportIncomeStatementPdf(
    @RequestParam(required = false) LocalDate fromDate,
    @RequestParam(required = false) LocalDate toDate
  ) throws Exception {
    if (toDate == null) {
      toDate = LocalDate.now();
    }
    IncomeStatementDTO report = incomeStatementService.generateIncomeStatement(fromDate, toDate);
    byte[] pdfFile = reportExportService.exportIncomeStatementToPdf(report);
    
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=income_statement_" + LocalDate.now() + ".pdf")
      .contentType(MediaType.APPLICATION_PDF)
      .body(pdfFile);
  }

  // ===== MANUAL ENTRY ENDPOINTS (for GL adjustments, accruals, etc.) =====

  /**
   * Create a new GL manual entry (PENDING status)
   */
  @PostMapping("/manual-entries")
  @PreAuthorize("hasRole('TREASURER')")
  public ResponseEntity<ApiResponse<GLManualEntryDTO>> createManualEntry(
    @RequestBody GLManualEntryRequest request,
    Authentication authentication
  ) {
    String username = authentication.getName();
    Integer userId = userService.getUserIdByUsername(username);
    if (userId == null) {
      throw new RuntimeException("User not found: " + username);
    }
    
    try {
      GLManualEntryDTO entry = glManualEntryService.createManualEntry(request, userId);
      logger.info("Manual GL entry created by " + username + ": " + entry.getGlAccountCode());
      
      // Capture in audit trail
      Optional<User> userOptional = userService.getUserById(userId.longValue());
      if (userOptional.isPresent()) {
        User user = userOptional.get();
        auditService.logAction(
          user,
          "GL_ENTRY_CREATED",
          "GLManualEntry",
          Long.valueOf(entry.getId()),
          "Account: " + entry.getGlAccountCode() + ", Amount: " + entry.getAmount() + ", Type: " + (entry.getIsDebit() ? "Debit" : "Credit"),
          "Created GL Manual Entry - Reason: " + entry.getEntryReason(),
          "SUCCESS"
        );
      }
      
      return ResponseEntity.ok(ApiResponse.success("Manual entry created and pending approval", entry));
    } catch (Exception e) {
      logger.error("Error creating GL entry: " + e.getMessage(), e);
      
      // Capture failure in audit trail
      try {
        Optional<User> userOptional = userService.getUserById(userId.longValue());
        if (userOptional.isPresent()) {
          User user = userOptional.get();
          auditService.logActionWithError(
            user,
            "GL_ENTRY_CREATED",
            "GLManualEntry",
            null,
            null,
            "Failed to create GL Manual Entry",
            e.getMessage()
          );
        }
      } catch (Exception auditEx) {
        logger.error("Failed to log audit for GL entry creation: " + auditEx.getMessage());
      }
      
      throw e;
    }
  }

  /**
   * Get all pending manual entries (for admin approval)
   */
  @GetMapping("/manual-entries/pending")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<List<GLManualEntryDTO>>> getPendingEntries() {
    List<GLManualEntryDTO> entries = glManualEntryService.getPendingEntries();
    logger.info("Retrieved " + entries.size() + " pending manual GL entries");
    
    return ResponseEntity.ok(ApiResponse.success("Pending entries retrieved", entries));
  }

  /**
   * Get all manual entries
   */
  @GetMapping("/manual-entries")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<ApiResponse<List<GLManualEntryDTO>>> getAllEntries() {
    List<GLManualEntryDTO> entries = glManualEntryService.getAllEntries();
    logger.info("Retrieved " + entries.size() + " manual GL entries");
    
    return ResponseEntity.ok(ApiResponse.success("Manual entries retrieved", entries));
  }

  /**
   * Get manual entries by GL account
   */
  @GetMapping("/manual-entries/account/{accountId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<List<GLManualEntryDTO>>> getEntriesByAccount(
    @PathVariable Integer accountId
  ) {
    List<GLManualEntryDTO> entries = glManualEntryService.getEntriesByAccount(accountId);
    logger.info("Retrieved " + entries.size() + " manual entries for account " + accountId);
    
    return ResponseEntity.ok(ApiResponse.success("Entries retrieved", entries));
  }

  /**
   * Approve a pending manual entry (Admin only)
   */
  @PutMapping("/manual-entries/{entryId}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLManualEntryDTO>> approveEntry(
    @PathVariable Integer entryId,
    Authentication authentication
  ) {
    String username = authentication.getName();
    Integer userId = userService.getUserIdByUsername(username);
    if (userId == null) {
      throw new RuntimeException("User not found: " + username);
    }
    
    try {
      GLManualEntryDTO entry = glManualEntryService.approveEntry(entryId, userId);
      logger.info("Manual GL entry approved by " + username + ": " + entry.getGlAccountCode());
      
      // Capture in audit trail
      Optional<User> userOptional = userService.getUserById(userId.longValue());
      if (userOptional.isPresent()) {
        User user = userOptional.get();
        auditService.logAction(
          user,
          "GL_ENTRY_APPROVED",
          "GLManualEntry",
          Long.valueOf(entryId),
          "Account: " + entry.getGlAccountCode() + ", Amount: " + entry.getAmount(),
          "Approved GL Manual Entry - Reason: " + entry.getEntryReason(),
          "SUCCESS"
        );
      }
      
      return ResponseEntity.ok(ApiResponse.success("Entry approved and will be included in GL calculations", entry));
    } catch (Exception e) {
      logger.error("Error approving GL entry: " + e.getMessage(), e);
      
      // Capture failure in audit trail
      try {
        Optional<User> userOptional = userService.getUserById(userId.longValue());
        if (userOptional.isPresent()) {
          User user = userOptional.get();
          auditService.logActionWithError(
            user,
            "GL_ENTRY_APPROVED",
            "GLManualEntry",
            Long.valueOf(entryId),
            null,
            "Failed to approve GL Manual Entry",
            e.getMessage()
          );
        }
      } catch (Exception auditEx) {
        logger.error("Failed to log audit for GL entry approval: " + auditEx.getMessage());
      }
      
      throw e;
    }
  }

  /**
   * Reject a pending manual entry (Admin only)
   */
  @PutMapping("/manual-entries/{entryId}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLManualEntryDTO>> rejectEntry(
    @PathVariable Integer entryId,
    Authentication authentication
  ) {
    String username = authentication.getName();
    Integer userId = userService.getUserIdByUsername(username);
    if (userId == null) {
      throw new RuntimeException("User not found: " + username);
    }
    
    try {
      GLManualEntryDTO entry = glManualEntryService.rejectEntry(entryId, userId);
      logger.info("Manual GL entry rejected by " + username + ": " + entry.getGlAccountCode());
      
      // Capture in audit trail
      Optional<User> userOptional = userService.getUserById(userId.longValue());
      if (userOptional.isPresent()) {
        User user = userOptional.get();
        auditService.logAction(
          user,
          "GL_ENTRY_REJECTED",
          "GLManualEntry",
          Long.valueOf(entryId),
          "Account: " + entry.getGlAccountCode() + ", Amount: " + entry.getAmount(),
          "Rejected GL Manual Entry - Reason: " + entry.getEntryReason(),
          "SUCCESS"
        );
      }
      
      return ResponseEntity.ok(ApiResponse.success("Entry rejected", entry));
    } catch (Exception e) {
      logger.error("Error rejecting GL entry: " + e.getMessage(), e);
      
      // Capture failure in audit trail
      try {
        Optional<User> userOptional = userService.getUserById(userId.longValue());
        if (userOptional.isPresent()) {
          User user = userOptional.get();
          auditService.logActionWithError(
            user,
            "GL_ENTRY_REJECTED",
            "GLManualEntry",
            Long.valueOf(entryId),
            null,
            "Failed to reject GL Manual Entry",
            e.getMessage()
          );
        }
      } catch (Exception auditEx) {
        logger.error("Failed to log audit for GL entry rejection: " + auditEx.getMessage());
      }
      
      throw e;
    }
  }

  /**
   * Delete a pending manual entry (Treasurer can delete own entries, Admin can delete any)
   */
  @DeleteMapping("/manual-entries/{entryId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<String>> deleteEntry(
    @PathVariable Integer entryId,
    Authentication authentication
  ) {
    String username = authentication != null ? authentication.getName() : "UNKNOWN";
    Integer userId = null;
    
    try {
      if (authentication != null) {
        userId = userService.getUserIdByUsername(username);
      }
      
      glManualEntryService.deleteEntry(entryId);
      logger.info("Manual GL entry deleted: " + entryId + " by user: " + username);
      
      // Capture in audit trail
      if (userId != null) {
        Optional<User> userOptional = userService.getUserById(userId.longValue());
        if (userOptional.isPresent()) {
          User user = userOptional.get();
          auditService.logAction(
            user,
            "GL_ENTRY_DELETED",
            "GLManualEntry",
            Long.valueOf(entryId),
            null,
            "Deleted GL Manual Entry (PENDING status)",
            "SUCCESS"
          );
        }
      }
      
      return ResponseEntity.ok(ApiResponse.success("Entry deleted", ""));
    } catch (Exception e) {
      logger.error("Error deleting GL entry: " + e.getMessage(), e);
      
      // Capture failure in audit trail
      if (userId != null) {
        try {
          Optional<User> userOptional = userService.getUserById(userId.longValue());
          if (userOptional.isPresent()) {
            User user = userOptional.get();
            auditService.logActionWithError(
              user,
              "GL_ENTRY_DELETED",
              "GLManualEntry",
              Long.valueOf(entryId),
              null,
              "Failed to delete GL Manual Entry",
              e.getMessage()
            );
          }
        } catch (Exception auditEx) {
          logger.error("Failed to log audit for GL entry deletion: " + auditEx.getMessage());
        }
      }
      
      throw e;
    }
  }
  
  /**
   * Helper: Build grouped trial balance by section
   */
  private GLTrialBalanceReportDTO buildGroupedTrialBalance(
      LocalDate asOfDate, Integer periodMonth, Integer periodYear) {
    // Fetch all active GL accounts
    List<GLAccount> accounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
    
    // Group accounts by sectionLabel (null section = "Other")
    Map<String, List<GLAccount>> sectionMap = new TreeMap<>();
    Map<String, Integer> sectionMinDisplay = new TreeMap<>();
    
    for (GLAccount account : accounts) {
      String section = account.getSectionLabel() != null ? 
                       account.getSectionLabel() : "Other";
      sectionMap.computeIfAbsent(section, k -> new ArrayList<>()).add(account);
      sectionMinDisplay.putIfAbsent(section, 
        account.getDisplayOrder() != null ? account.getDisplayOrder() : 999);
      if (account.getDisplayOrder() != null) {
        sectionMinDisplay.put(section, 
          Math.min(sectionMinDisplay.get(section), account.getDisplayOrder()));
      }
    }
    
    // Sort sections by minimum displayOrder
    List<String> sortedSections = sectionMap.keySet().stream()
      .sorted((s1, s2) -> sectionMinDisplay.get(s1).compareTo(sectionMinDisplay.get(s2)))
      .collect(Collectors.toList());
    
    // Build report
    GLTrialBalanceReportDTO report = new GLTrialBalanceReportDTO();
    report.setReportTitle("TRIAL BALANCE");
    report.setSaccoName("MINET SACCO");
    report.setAsOfDate(asOfDate);
    report.setPeriodMonth(periodMonth);
    report.setPeriodYear(periodYear);
    
    List<GLTrialBalanceSectionDTO> sections = new ArrayList<>();
    BigDecimal grandDebit = BigDecimal.ZERO;
    BigDecimal grandCredit = BigDecimal.ZERO;
    
    for (String sectionName : sortedSections) {
      List<GLTrialBalanceLineDTO> lines = new ArrayList<>();
      BigDecimal sectionDebit = BigDecimal.ZERO;
      BigDecimal sectionCredit = BigDecimal.ZERO;
      
      for (GLAccount account : sectionMap.get(sectionName)) {
        // Calculate balance using GL calculation service with period filtering
        BigDecimal balance = glCalculationService.calculateGLAccountBalance(
          account.getId(), asOfDate, periodMonth, periodYear);
        
        // Determine if account goes on DEBIT or CREDIT side based on normalBalance
        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;
        
        if (account.getNormalBalance() == GLAccount.NormalBalance.DEBIT) {
          if (balance.compareTo(BigDecimal.ZERO) >= 0) {
            debitAmount = balance;
          } else {
            creditAmount = balance.abs();
          }
        } else if (account.getNormalBalance() == GLAccount.NormalBalance.CREDIT) {
          if (balance.compareTo(BigDecimal.ZERO) >= 0) {
            creditAmount = balance;
          } else {
            debitAmount = balance.abs();
          }
        } else {
          // Default: positive balance on debit side if no normalBalance specified
          if (balance.compareTo(BigDecimal.ZERO) >= 0) {
            debitAmount = balance;
          } else {
            creditAmount = balance.abs();
          }
        }
        
        // Determine source type (AUTO or MANUAL)
        String sourceType = account.getBalanceCalculationType() == 
                           GLAccount.CalculationType.AGGREGATION ? "AUTO" : "MANUAL";
        
        GLTrialBalanceLineDTO line = new GLTrialBalanceLineDTO(
          account.getCode(),
          account.getName(),
          debitAmount,
          creditAmount,
          sourceType
        );
        lines.add(line);
        
        sectionDebit = sectionDebit.add(debitAmount);
        sectionCredit = sectionCredit.add(creditAmount);
      }
      
      GLTrialBalanceSectionDTO section = new GLTrialBalanceSectionDTO(
        sectionName, lines, sectionDebit, sectionCredit);
      sections.add(section);
      
      grandDebit = grandDebit.add(sectionDebit);
      grandCredit = grandCredit.add(sectionCredit);
    }
    
    report.setSections(sections);
    report.setGrandTotalDebit(grandDebit);
    report.setGrandTotalCredit(grandCredit);
    
    // Calculate difference and balanced status
    BigDecimal difference = grandDebit.subtract(grandCredit).abs();
    report.setDifference(difference);
    report.setIsBalanced(difference.compareTo(BigDecimal.ZERO) == 0);
    
    return report;
  }
}
