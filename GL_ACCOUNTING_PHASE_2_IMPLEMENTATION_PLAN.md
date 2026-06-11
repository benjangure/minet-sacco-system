# GL Accounting Layer - Phase 2 Implementation Plan

**Date**: June 5, 2026  
**Status**: Planned (Ready for Implementation)  
**Duration**: 16-20 hours  
**Scope**: Complete GL foundation + Financial statements + Operational GL reports  

---

## OVERVIEW: WHAT PHASE 2 ACHIEVES

### Current State (After Phase 1)
✅ 4 operational reports working with member-level data:
- Guarantor Report (member guarantor capacity)
- Loan Eligibility Report (member loan eligibility)
- Withdrawal Monitoring Report (transaction audit trail)
- Monthly Contribution Tracking Report (bulk upload monitoring)

### Phase 2 Goal
Build the **GL Accounting Layer** that enables:
- ✅ Trial Balance Report (SASRA requirement)
- ✅ Balance Sheet (financial position)
- ✅ Income Statement (profit/loss)
- ✅ Cash Flow Statement (cash management)
- ✅ GL-based Cashbook (reconciled to bank accounts)
- ✅ GL-based Loan Register (validated to loan data)
- ✅ Member Statements (GL-derived)
- ✅ SASRA Compliance Reports (financial metrics)

### Architecture Philosophy
**GL accounts are configuration, not hard-code.** Each GL account defines HOW to calculate its balance:
- From tables (aggregation)
- From formulas (calculations)
- From manual entry (treasurer input)
- As date-driven snapshots

This makes the system **flexible** when data or business rules change (new loan types, account types, etc.)

---

## PHASE 2A: GL FOUNDATION (6-8 HOURS)

### Step 1: Database Schema (1 hour)

Create 3 new tables:

```sql
-- 1. GL ACCOUNTS MASTER
CREATE TABLE gl_accounts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(20) UNIQUE NOT NULL,           -- "LOAN_NORMAL", "MEMBER_DEPOSITS", etc.
  name VARCHAR(255) NOT NULL,                 -- "Normal Loans", "Member Deposits"
  account_type ENUM(
    'ASSET',
    'LIABILITY', 
    'EQUITY',
    'REVENUE',
    'EXPENSE'
  ) NOT NULL,
  
  balance_calculation_type ENUM(
    'AGGREGATION',      -- SUM from table/field
    'FORMULA',          -- Math on other GL accounts
    'MANUAL_ENTRY',     -- Treasurer enters value
    'COMPUTED'          -- Complex query
  ) NOT NULL,
  
  calculation_config JSON NOT NULL,          -- Type-specific config
  is_active BOOLEAN DEFAULT TRUE,
  display_order INT DEFAULT 100,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_type (account_type),
  INDEX idx_code (code)
);

-- 2. GL ACCOUNT CALCULATIONS (For complex accounts)
CREATE TABLE gl_account_calculations (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  calculation_name VARCHAR(255),             -- "Disbursed Normal Loans", etc.
  calculation_type ENUM(
    'SUM_FIELD',        -- SUM(field) FROM table
    'CUSTOM_QUERY',     -- Raw SQL query
    'LOOKUP',           -- Reference another GL account
    'PERCENTAGE',       -- % of another account
    'CONDITIONAL'       -- IF/THEN logic
  ) NOT NULL,
  
  calculation_config JSON NOT NULL,          -- Type-specific config
  weight DECIMAL(5,2) DEFAULT 1.0,           -- Multiply result by weight
  operator ENUM('+', '-', '*', '/') DEFAULT '+',  -- How to combine
  sort_order INT DEFAULT 100,
  is_active BOOLEAN DEFAULT TRUE,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id),
  INDEX idx_gl_account (gl_account_id)
);

-- 3. GL MANUAL ENTRIES (Treasurer adjustments)
CREATE TABLE gl_manual_entries (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  entry_date DATE NOT NULL,
  description VARCHAR(500),
  amount DECIMAL(15,2) NOT NULL,
  is_debit BOOLEAN,
  
  entry_reason ENUM(
    'ACCRUAL',          -- Interest/dividend accrual
    'ADJUSTMENT',       -- Manual correction
    'ALLOCATION',       -- Manual expense allocation
    'RECLASSIFICATION'  -- Move between accounts
  ) NOT NULL,
  
  approval_status ENUM(
    'PENDING',
    'APPROVED',
    'REJECTED'
  ) DEFAULT 'PENDING',
  
  created_by_user_id INT NOT NULL,
  approved_by_user_id INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  approved_at TIMESTAMP,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id),
  FOREIGN KEY (created_by_user_id) REFERENCES users(id),
  FOREIGN KEY (approved_by_user_id) REFERENCES users(id),
  INDEX idx_date (entry_date),
  INDEX idx_account (gl_account_id)
);

-- 4. GL CONFIGURATION HISTORY (Audit trail)
CREATE TABLE gl_account_audit (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  changed_by_user_id INT NOT NULL,
  change_type ENUM('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE'),
  old_config JSON,
  new_config JSON,
  change_reason VARCHAR(500),
  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id),
  FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);
```

### Step 2: Create GLAccount Entity (30 minutes)

File: `backend/src/main/java/com/minet/sacco/entity/GLAccount.java`

```java
package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gl_accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GLAccount implements Serializable {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  
  @Column(unique = true, nullable = false)
  private String code;
  
  @Column(nullable = false)
  private String name;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountType accountType;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CalculationType balanceCalculationType;
  
  @Column(columnDefinition = "JSON")
  private JsonNode calculationConfig;
  
  @Column(nullable = false)
  private Boolean isActive = true;
  
  @Column(nullable = false)
  private Integer displayOrder = 100;
  
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
  
  @Column(nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
  
  public enum AccountType {
    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
  }
  
  public enum CalculationType {
    AGGREGATION, FORMULA, MANUAL_ENTRY, COMPUTED
  }
}
```

### Step 3: Create Related Entities (30 minutes)

Create entities for:
- `GLManualEntry.java`
- `GLAccountAudit.java`

(Similar structure to GLAccount)

### Step 4: Create Repositories (30 minutes)

File: `backend/src/main/java/com/minet/sacco/repository/GLAccountRepository.java`

```java
package com.minet.sacco.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.AccountType;
import java.util.List;
import java.util.Optional;

@Repository
public interface GLAccountRepository extends JpaRepository<GLAccount, Integer> {
  Optional<GLAccount> findByCode(String code);
  List<GLAccount> findByAccountTypeOrderByDisplayOrder(AccountType type);
  List<GLAccount> findByIsActiveTrueOrderByDisplayOrder();
  List<GLAccount> findByAccountTypeAndIsActiveTrue(AccountType type);
}
```

Also create:
- `GLManualEntryRepository.java`
- `GLAccountAuditRepository.java`

### Step 5: Create GLCalculationService (2-3 hours)

File: `backend/src/main/java/com/minet/sacco/service/GLCalculationService.java`

```java
package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLManualEntry;
import com.minet.sacco.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GLCalculationService {
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  @Autowired
  private GLManualEntryRepository manualEntryRepository;
  
  @Autowired
  private LoanRepository loanRepository;
  @Autowired
  private AccountRepository accountRepository;
  @Autowired
  private TransactionRepository transactionRepository;
  @Autowired
  private GuarantorRepository guarantorRepository;
  
  private static final BigDecimal ZERO = BigDecimal.ZERO;
  
  /**
   * Calculate balance for a GL account as of a specific date
   */
  public BigDecimal calculateGLAccountBalance(Integer glAccountId, LocalDate asOfDate) {
    GLAccount account = glAccountRepository.findById(glAccountId)
      .orElseThrow(() -> new RuntimeException("GL Account not found: " + glAccountId));
    
    return calculateBalance(account, asOfDate);
  }
  
  /**
   * Main calculation dispatcher
   */
  private BigDecimal calculateBalance(GLAccount account, LocalDate asOfDate) {
    try {
      switch (account.getBalanceCalculationType()) {
        case AGGREGATION:
          return calculateAggregation(account.getCalculationConfig(), asOfDate);
        
        case FORMULA:
          return calculateFormula(account.getCalculationConfig(), asOfDate);
        
        case MANUAL_ENTRY:
          return calculateManualEntry(account.getId(), asOfDate);
        
        case COMPUTED:
          return calculateComputed(account.getCalculationConfig(), asOfDate);
        
        default:
          return ZERO;
      }
    } catch (Exception e) {
      log.error("Error calculating GL account balance: " + account.getCode(), e);
      return ZERO;
    }
  }
  
  /**
   * AGGREGATION: SUM from table/field
   */
  private BigDecimal calculateAggregation(JsonNode config, LocalDate asOfDate) {
    String table = config.get("table").asText();
    String field = config.get("field").asText();
    String whereClause = config.has("where") ? config.get("where").asText() : null;
    
    // Route to appropriate repository based on table
    switch (table) {
      case "loans":
        return calculateLoansAggregation(field, whereClause, asOfDate);
      
      case "accounts":
        return calculateAccountsAggregation(field, whereClause, asOfDate);
      
      case "transactions":
        return calculateTransactionsAggregation(field, whereClause, asOfDate);
      
      case "guarantor":
        return calculateGuarantorAggregation(field, whereClause, asOfDate);
      
      default:
        log.warn("Unknown table for aggregation: " + table);
        return ZERO;
    }
  }
  
  private BigDecimal calculateLoansAggregation(String field, String whereClause, LocalDate asOfDate) {
    // Example: Calculate total outstanding balance for all NORMAL loans
    if ("outstanding_balance".equals(field)) {
      if (whereClause != null && whereClause.contains("NORMAL")) {
        return loanRepository.findAll().stream()
          .filter(l -> l.getLoanType().equals("NORMAL"))
          .map(l -> l.getOutstandingBalance() != null ? 
            new BigDecimal(l.getOutstandingBalance().toString()) : ZERO)
          .reduce(ZERO, BigDecimal::add);
      }
    }
    
    // Add more specific calculations based on field + whereClause
    return ZERO;
  }
  
  private BigDecimal calculateAccountsAggregation(String field, String whereClause, LocalDate asOfDate) {
    // Example: Calculate total member deposits
    if ("balance".equals(field)) {
      if (whereClause != null && whereClause.contains("SAVINGS")) {
        return accountRepository.findAll().stream()
          .filter(a -> "SAVINGS".equals(a.getAccountType()))
          .map(a -> a.getBalance() != null ? 
            new BigDecimal(a.getBalance().toString()) : ZERO)
          .reduce(ZERO, BigDecimal::add);
      }
    }
    
    return ZERO;
  }
  
  private BigDecimal calculateTransactionsAggregation(String field, String whereClause, LocalDate asOfDate) {
    // Calculate sum of transactions matching criteria
    if ("amount".equals(field) && whereClause != null) {
      // Parse WHERE clause for transaction type and date
      if (whereClause.contains("INTEREST_RECEIVED")) {
        return transactionRepository.findAll().stream()
          .filter(t -> "INTEREST".equals(t.getTransactionType()))
          .map(t -> t.getAmount() != null ? 
            new BigDecimal(t.getAmount().toString()) : ZERO)
          .reduce(ZERO, BigDecimal::add);
      }
    }
    
    return ZERO;
  }
  
  private BigDecimal calculateGuarantorAggregation(String field, String whereClause, LocalDate asOfDate) {
    // Calculate sum of guarantor pledges
    if ("pledgeAmount".equals(field)) {
      return guarantorRepository.findAll().stream()
        .filter(g -> "ACTIVE".equals(g.getStatus()))
        .map(g -> g.getPledgeAmount() != null ? 
          new BigDecimal(g.getPledgeAmount().toString()) : ZERO)
        .reduce(ZERO, BigDecimal::add);
    }
    
    return ZERO;
  }
  
  /**
   * FORMULA: Math calculation on other GL accounts
   */
  private BigDecimal calculateFormula(JsonNode config, LocalDate asOfDate) {
    // Example: Available Guarantorship = Available Savings - External Pledges
    String formula = config.get("formula").asText();
    // Parse formula and calculate referenced GL accounts recursively
    // This requires expression parsing (e.g., MVEL, SpEL)
    return ZERO;  // TODO: Implement expression evaluation
  }
  
  /**
   * MANUAL_ENTRY: Sum of treasurer-entered values
   */
  private BigDecimal calculateManualEntry(Integer glAccountId, LocalDate asOfDate) {
    return manualEntryRepository.findByGlAccountIdAndEntryDateLessThanEqual(glAccountId, asOfDate)
      .stream()
      .map(e -> e.getIsDebit() ? 
        new BigDecimal(e.getAmount().toString()) : 
        new BigDecimal(e.getAmount().toString()).negate())
      .reduce(ZERO, BigDecimal::add);
  }
  
  /**
   * COMPUTED: Custom complex logic
   */
  private BigDecimal calculateComputed(JsonNode config, LocalDate asOfDate) {
    // For complex cases that don't fit other patterns
    return ZERO;  // TODO: Implement based on config
  }
  
  /**
   * Generate Trial Balance for a date
   */
  public TrialBalanceDTO generateTrialBalance(LocalDate asOfDate) {
    List<GLAccount> activeAccounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
    
    List<TrialBalanceLineDTO> lines = activeAccounts.stream()
      .map(acc -> {
        BigDecimal balance = calculateBalance(acc, asOfDate);
        return new TrialBalanceLineDTO(
          acc.getCode(),
          acc.getName(),
          acc.getAccountType().toString(),
          balance,
          isDebit(acc.getAccountType(), balance)
        );
      })
      .filter(line -> line.getBalance().compareTo(ZERO) != 0)  // Only non-zero lines
      .collect(java.util.stream.Collectors.toList());
    
    return new TrialBalanceDTO(asOfDate, lines);
  }
  
  private Boolean isDebit(GLAccount.AccountType type, BigDecimal balance) {
    switch (type) {
      case ASSET:
      case EXPENSE:
        return balance.compareTo(ZERO) >= 0;
      case LIABILITY:
      case EQUITY:
      case REVENUE:
        return balance.compareTo(ZERO) < 0;
      default:
        return true;
    }
  }
}
```

### Step 6: Create ReportDTOs (30 minutes)

File: `backend/src/main/java/com/minet/sacco/dto/TrialBalanceDTO.java`

```java
package com.minet.sacco.dto;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceDTO {
  private LocalDate asOfDate;
  private List<TrialBalanceLineDTO> lines;
  private TrialBalanceSummaryDTO summary;
  
  public TrialBalanceDTO(LocalDate asOfDate, List<TrialBalanceLineDTO> lines) {
    this.asOfDate = asOfDate;
    this.lines = lines;
    this.summary = calculateSummary(lines);
  }
  
  private TrialBalanceSummaryDTO calculateSummary(List<TrialBalanceLineDTO> lines) {
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    
    for (TrialBalanceLineDTO line : lines) {
      if (line.getIsDebit()) {
        totalDebit = totalDebit.add(line.getBalance());
      } else {
        totalCredit = totalCredit.add(line.getBalance());
      }
    }
    
    return new TrialBalanceSummaryDTO(
      totalDebit,
      totalCredit,
      totalDebit.compareTo(totalCredit) == 0
    );
  }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class TrialBalanceLineDTO {
  private String code;
  private String name;
  private String accountType;
  private BigDecimal balance;
  private Boolean isDebit;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class TrialBalanceSummaryDTO {
  private BigDecimal totalDebit;
  private BigDecimal totalCredit;
  private Boolean isBalanced;
}
```

### Step 7: Create GL Controller Endpoints (1 hour)

File: `backend/src/main/java/com/minet/sacco/controller/GLController.java`

```java
package com.minet.sacco.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.minet.sacco.dto.*;
import com.minet.sacco.service.GLCalculationService;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.repository.GLAccountRepository;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/gl")
public class GLController {
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  /**
   * Get all active GL accounts
   */
  @GetMapping("/accounts")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<List<GLAccount>>> getAllAccounts() {
    List<GLAccount> accounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
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
   * Create new GL account
   */
  @PostMapping("/accounts")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLAccount>> createAccount(@RequestBody GLAccount account) {
    GLAccount saved = glAccountRepository.save(account);
    return ResponseEntity.ok(ApiResponse.success("GL Account created", saved));
  }
  
  /**
   * Update GL account calculation config
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
    
    GLAccount saved = glAccountRepository.save(account);
    return ResponseEntity.ok(ApiResponse.success("GL Account updated", saved));
  }
  
  /**
   * Generate Trial Balance Report
   */
  @GetMapping("/trial-balance")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
  public ResponseEntity<ApiResponse<TrialBalanceDTO>> generateTrialBalance(
    @RequestParam(required = false) LocalDate asOfDate
  ) {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    
    TrialBalanceDTO report = glCalculationService.generateTrialBalance(asOfDate);
    return ResponseEntity.ok(ApiResponse.success("Trial Balance generated", report));
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
}
```

### Step 8: Populate Initial GL Accounts (1 hour)

Create migration SQL file: `backend/src/main/resources/db/migration/V116__Create_GL_Accounts.sql`

```sql
-- Asset Accounts
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('LOAN_NORMAL', 'Normal Loans', 'ASSET', 'AGGREGATION', 
  '{"table":"loans","field":"outstanding_balance","where":"loan_type = ''NORMAL'' AND status = ''DISBURSED''"}', 10),

('LOAN_EMERGENCY_1', 'Emergency Loan Type 1', 'ASSET', 'AGGREGATION',
  '{"table":"loans","field":"outstanding_balance","where":"loan_type = ''EMERGENCY_1'' AND status = ''DISBURSED''"}', 11),

('LOAN_EMERGENCY_2', 'Emergency Loan Type 2', 'ASSET', 'AGGREGATION',
  '{"table":"loans","field":"outstanding_balance","where":"loan_type = ''EMERGENCY_2'' AND status = ''DISBURSED''"}', 12),

('CBA_CALL_DEPOSITS', 'CBA Call Deposits', 'ASSET', 'AGGREGATION',
  '{"table":"accounts","field":"balance","where":"bank_account_type = ''CALL''"}', 20),

('CBA_CURRENT', 'CBA Current Account', 'ASSET', 'AGGREGATION',
  '{"table":"accounts","field":"balance","where":"bank_account_type = ''CURRENT''"}', 21),

('CO_OP_HOLDINGS', 'Co-op Holdings', 'ASSET', 'MANUAL_ENTRY', '{}', 30),
('COOP_INSURANCE', 'Co-op Insurance', 'ASSET', 'MANUAL_ENTRY', '{}', 31),
('KUSCCO', 'KUSCCO', 'ASSET', 'MANUAL_ENTRY', '{}', 32);

-- Liability Accounts
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('MEMBER_DEPOSITS', 'Member Deposits', 'LIABILITY', 'AGGREGATION',
  '{"table":"accounts","field":"balance","where":"account_type = ''SAVINGS''"}', 40),

('MEMBER_SHARES', 'Member Shares', 'LIABILITY', 'AGGREGATION',
  '{"table":"accounts","field":"balance","where":"account_type = ''SHARES''"}', 41),

('AUDITOR_PAYABLE', 'Auditor Fees Payable', 'LIABILITY', 'MANUAL_ENTRY', '{}', 50),
('DIVIDEND_PAYABLE', 'Dividend Payable', 'LIABILITY', 'MANUAL_ENTRY', '{}', 51),
('INTEREST_PAYABLE', 'Interest Payable', 'LIABILITY', 'MANUAL_ENTRY', '{}', 52),
('COMMITTEE_ALLOWANCE_PAYABLE', 'Committee Allowance Payable', 'LIABILITY', 'MANUAL_ENTRY', '{}', 53);

-- Equity Accounts
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('STATUTORY_RESERVE', 'Statutory Reserve', 'EQUITY', 'MANUAL_ENTRY', '{}', 60),
('REVENUE_RESERVE', 'Revenue Reserve', 'EQUITY', 'MANUAL_ENTRY', '{}', 61),
('RETAINED_EARNINGS', 'Retained Earnings', 'EQUITY', 'COMPUTED', '{}', 62);

-- Revenue Accounts
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('INT_LOANS', 'Interest - Loans', 'REVENUE', 'AGGREGATION',
  '{"table":"transactions","field":"amount","where":"transaction_type = ''INTEREST''"}', 70),

('INT_DEPOSITS', 'Interest - Deposits', 'REVENUE', 'MANUAL_ENTRY', '{}', 71),
('ENTRANCE_FEES', 'Entrance Fees', 'REVENUE', 'AGGREGATION',
  '{"table":"transactions","field":"amount","where":"description LIKE ''%entrance%''"}', 72);

-- Expense Accounts
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('AUDIT_FEES', 'Audit Fees', 'EXPENSE', 'MANUAL_ENTRY', '{}', 80),
('TRAVEL_EXPENSES', 'Travel Expenses', 'EXPENSE', 'MANUAL_ENTRY', '{}', 81),
('SASRA_FEES', 'SASRA Fees', 'EXPENSE', 'MANUAL_ENTRY', '{}', 82),
('TRAINING', 'Training', 'EXPENSE', 'MANUAL_ENTRY', '{}', 83),
('COMMITTEE_ALLOWANCES', 'Committee Allowances', 'EXPENSE', 'MANUAL_ENTRY', '{}', 84),
('AGM_EXPENSES', 'AGM Expenses', 'EXPENSE', 'MANUAL_ENTRY', '{}', 85),
('INSURANCE_PREMIUMS', 'Insurance Premiums', 'EXPENSE', 'MANUAL_ENTRY', '{}', 86),
('BANK_CHARGES', 'Bank Charges', 'EXPENSE', 'AGGREGATION',
  '{"table":"transactions","field":"amount","where":"description LIKE ''%bank%charge%''"}', 87),
('LOAN_LOSS_PROVISION', 'Loan Loss Provision', 'EXPENSE', 'MANUAL_ENTRY', '{}', 88),
('INCOME_TAX', 'Income Tax', 'EXPENSE', 'MANUAL_ENTRY', '{}', 89);
```

---

## PHASE 2B: FINANCIAL STATEMENTS (4-6 HOURS)

### Step 1: Balance Sheet Service (1.5 hours)

File: `backend/src/main/java/com/minet/sacco/service/BalanceSheetService.java`

```java
package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.dto.*;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.repository.GLAccountRepository;
import com.minet.sacco.entity.GLAccount.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BalanceSheetService {
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  public BalanceSheetDTO generateBalanceSheet(LocalDate asOfDate) {
    // Get all assets, liabilities, equity
    List<GLAccount> assets = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.ASSET);
    List<GLAccount> liabilities = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.LIABILITY);
    List<GLAccount> equity = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EQUITY);
    
    // Calculate balances
    List<BalanceSheetLineDTO> assetLines = calculateLines(assets, asOfDate);
    List<BalanceSheetLineDTO> liabilityLines = calculateLines(liabilities, asOfDate);
    List<BalanceSheetLineDTO> equityLines = calculateLines(equity, asOfDate);
    
    // Calculate totals
    BigDecimal totalAssets = assetLines.stream()
      .map(BalanceSheetLineDTO::getAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalLiabilities = liabilityLines.stream()
      .map(BalanceSheetLineDTO::getAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalEquity = equityLines.stream()
      .map(BalanceSheetLineDTO::getAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    return new BalanceSheetDTO(
      asOfDate,
      assetLines, totalAssets,
      liabilityLines, totalLiabilities,
      equityLines, totalEquity,
      totalAssets.equals(totalLiabilities.add(totalEquity))
    );
  }
  
  private List<BalanceSheetLineDTO> calculateLines(List<GLAccount> accounts, LocalDate asOfDate) {
    return accounts.stream()
      .map(acc -> new BalanceSheetLineDTO(
        acc.getCode(),
        acc.getName(),
        glCalculationService.calculateGLAccountBalance(acc.getId(), asOfDate)
      ))
      .filter(line -> line.getAmount().compareTo(BigDecimal.ZERO) != 0)
      .collect(Collectors.toList());
  }
}
```

### Step 2: Income Statement Service (1.5 hours)

File: `backend/src/main/java/com/minet/sacco/service/IncomeStatementService.java`

```java
package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.dto.*;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.repository.GLAccountRepository;
import com.minet.sacco.entity.GLAccount.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class IncomeStatementService {
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  public IncomeStatementDTO generateIncomeStatement(LocalDate fromDate, LocalDate toDate) {
    List<GLAccount> revenues = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.REVENUE);
    List<GLAccount> expenses = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EXPENSE);
    
    // For now, using asOfDate = toDate (later implement date range filtering)
    List<IncomeStatementLineDTO> revenueLines = revenues.stream()
      .map(acc -> new IncomeStatementLineDTO(
        acc.getCode(),
        acc.getName(),
        glCalculationService.calculateGLAccountBalance(acc.getId(), toDate)
      ))
      .filter(line -> line.getAmount().compareTo(BigDecimal.ZERO) != 0)
      .collect(java.util.stream.Collectors.toList());
    
    List<IncomeStatementLineDTO> expenseLines = expenses.stream()
      .map(acc -> new IncomeStatementLineDTO(
        acc.getCode(),
        acc.getName(),
        glCalculationService.calculateGLAccountBalance(acc.getId(), toDate)
      ))
      .filter(line -> line.getAmount().compareTo(BigDecimal.ZERO) != 0)
      .collect(java.util.stream.Collectors.toList());
    
    BigDecimal totalRevenue = revenueLines.stream()
      .map(IncomeStatementLineDTO::getAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalExpense = expenseLines.stream()
      .map(IncomeStatementLineDTO::getAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal netIncome = totalRevenue.subtract(totalExpense);
    
    return new IncomeStatementDTO(fromDate, toDate, revenueLines, totalRevenue, 
                                    expenseLines, totalExpense, netIncome);
  }
}
```

### Step 3: Cash Flow Service (1.5 hours)

File: `backend/src/main/java/com/minet/sacco/service/CashFlowService.java`

(Similar structure - track CASH GL account changes by category)

### Step 4: Create Financial Statement DTOs (30 minutes)

```java
// BalanceSheetDTO.java
// IncomeStatementDTO.java
// CashFlowDTO.java
```

### Step 5: Add Financial Statement Endpoints to ReportsController (1 hour)

```java
@GetMapping("/balance-sheet")
@PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
public ResponseEntity<ApiResponse<BalanceSheetDTO>> balanceSheet(
  @RequestParam(required = false) LocalDate asOfDate
)

@GetMapping("/income-statement")
@PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
public ResponseEntity<ApiResponse<IncomeStatementDTO>> incomeStatement(
  @RequestParam LocalDate fromDate,
  @RequestParam LocalDate toDate
)

@GetMapping("/cash-flow")
@PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'AUDITOR')")
public ResponseEntity<ApiResponse<CashFlowDTO>> cashFlow(
  @RequestParam LocalDate fromDate,
  @RequestParam LocalDate toDate
)
```

---

## PHASE 2C: GL-BASED OPERATIONAL REPORTS (3-4 HOURS)

### Step 1: GL Cashbook Service (1 hour)

Uses GL CASH account to reconcile bank transactions

### Step 2: GL Loan Register Service (1 hour)

Uses GL LOAN_* accounts to validate loan portfolio

### Step 3: GL Member Statement Service (1 hour)

Uses GL member-level mappings

---

## PHASE 2D: FRONTEND INTEGRATION (2-3 HOURS)

### Step 1: Create Reports.tsx Extensions

Add sections for:
- Trial Balance
- Balance Sheet
- Income Statement
- Cash Flow
- GL-based Cashbook

### Step 2: Create GL Configuration UI

Allow treasurer to:
- View all GL accounts
- Edit calculation configs
- Test queries
- Enter manual adjustments

### Step 3: Export to Excel/PDF

---

## TESTING CHECKLIST

```
GL Foundation:
  ☐ GL accounts created successfully
  ☐ Calculation engine works for AGGREGATION
  ☐ Calculation engine works for MANUAL_ENTRY
  ☐ Trial Balance generates and balances (Dr = Cr)

Financial Statements:
  ☐ Balance Sheet: Assets = Liabilities + Equity
  ☐ Income Statement: Revenues - Expenses = Net Income
  ☐ Cash Flow: Accounts for all cash movements

Operational Reports:
  ☐ GL Cashbook reconciles to trial balance
  ☐ GL Loan Register sums to LOAN_* GL accounts
  ☐ Member Statements derive from GL

Access Control:
  ☐ Only Treasurer/Admin can create GL accounts
  ☐ Only Auditor/Treasurer can view reports
  ☐ Audit trail captures all GL changes

Data Accuracy:
  ☐ Loan balances match GL LOAN_* accounts
  ☐ Member deposits match GL MEMBER_DEPOSITS
  ☐ Trial balance balances for multiple dates
```

---

## IMPLEMENTATION ORDER

### Day 1 (6-8 hours): Phase 2A Foundation
1. Create database tables
2. Create entities and repositories
3. Create GLCalculationService
4. Create GL controller endpoints
5. Populate initial GL accounts

### Day 2 (4-6 hours): Phase 2B Financial Statements
1. Create BalanceSheetService
2. Create IncomeStatementService
3. Create CashFlowService
4. Add endpoints
5. Create DTOs

### Day 3 (3-4 hours): Phase 2C GL Reports
1. GL Cashbook Service
2. GL Loan Register
3. GL Member Statements

### Day 4 (2-3 hours): Phase 2D Frontend
1. Update Reports.tsx
2. Create GL Configuration UI
3. Test end-to-end

---

## SUCCESS CRITERIA

Phase 2 is complete when:
1. ✅ Trial Balance generates and balances (Dr = Cr)
2. ✅ Balance Sheet: Assets = Liabilities + Equity
3. ✅ Income Statement shows realistic P&L
4. ✅ Cash Flow accounts for all cash movements
5. ✅ All reports reconcile to source operational data
6. ✅ Treasurer can manage GL accounts via UI
7. ✅ All exports work (Excel/PDF)
8. ✅ Access controls enforced
9. ✅ Audit trail recorded

---

## NEXT PHASES (OPTIONAL)

Once Phase 2 is live:

### Phase 3: SASRA Compliance Reports (2-3 hours)
- Capital Adequacy
- PAR (Portfolio at Risk)
- Liquidity Ratios
- SASRA Submission Format

### Phase 4: Advanced Features (4-6 hours)
- Budget vs Actual
- Comparative Analysis (YoY, MoM)
- Variance Analysis
- Forecast Models

---

## KNOWN LIMITATIONS & FUTURE CONSIDERATIONS

1. **Date Range Filtering**: Current implementation uses asOfDate snapshots
   - Future: Implement transaction date filtering for period reports

2. **Formula Evaluation**: FORMULA calculation type needs expression engine
   - Recommendation: Use Spring Expression Language (SpEL) or MVEL

3. **Real-time GL Ledger**: System currently calculates GL from operational data
   - Future: Implement journal entries for real-time GL posting (true double-entry)

4. **Multi-currency**: Currently supports KES only
   - Future: Add currency field to GL accounts and transactions

5. **Consolidation**: Single SACCO only
   - Future: Support multiple SACCOs with consolidation

---

## DEPENDENCIES & PREREQUISITES

- Java 17+
- Spring Boot 3.x
- MySQL 8.0+
- Flyway migrations
- Spring Security (roles/permissions)
- Jackson (JSON processing)

---

## ROLLBACK PLAN

If Phase 2 implementation encounters issues:
1. The 4 Phase 1 operational reports continue working unchanged
2. GL layer is additive - doesn't modify existing data
3. To rollback: Disable GL endpoints in controller, don't migrate GL tables
4. Phase 1 reports continue serving their purpose

---

## Document History

| Date | Author | Version | Changes |
|------|--------|---------|---------|
| 2026-06-05 | Kiro | 1.0 | Initial Phase 2 plan |

---

**Status**: Ready for Implementation ✅

Next Step: Begin Phase 2A (Database Schema + GLCalculationService)

Questions? Review GL_MAPPING_FLEXIBLE_ARCHITECTURE.md for design rationale.
