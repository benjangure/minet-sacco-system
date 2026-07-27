# GL System Complete Source Code

**Document Version**: 1.0  
**Generated**: June 8, 2026  
**Purpose**: Complete reference of all GL infrastructure files (migrations, entities, and services)

---

## TABLE OF CONTENTS

1. [V116__Create_GL_Tables.sql](#section-1-v116create_gl_tablessql)
2. [V117__Populate_GL_Accounts.sql](#section-2-v117populate_gl_accountssql)
3. [GLAccount.java (Entity)](#section-3-glaccountjava-entity)
4. [GLManualEntry.java (Entity)](#section-4-glmanualentryjava-entity)
5. [GLCalculationService.java](#section-5-glcalculationservicejava)
6. [BalanceSheetService.java](#section-6-balancesheetservicejava)
7. [IncomeStatementService.java](#section-7-incomestatementservicejava)

---

## SECTION 1: V116__Create_GL_Tables.sql

**Location**: `backend/src/main/resources/db/migration/V116__Create_GL_Tables.sql`

```sql
-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS gl_account_audit;
DROP TABLE IF EXISTS gl_manual_entries;
DROP TABLE IF EXISTS gl_account_calculations;
DROP TABLE IF EXISTS gl_accounts;

-- GL ACCOUNTS MASTER TABLE
CREATE TABLE IF NOT EXISTS gl_accounts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  account_type ENUM('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE') NOT NULL,
  balance_calculation_type ENUM('AGGREGATION', 'FORMULA', 'MANUAL_ENTRY', 'COMPUTED') NOT NULL,
  calculation_config JSON NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  display_order INT DEFAULT 100,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_type (account_type),
  INDEX idx_code (code),
  INDEX idx_active (is_active)
);

-- GL ACCOUNT CALCULATIONS TABLE (For complex accounts with multiple calculations)
CREATE TABLE IF NOT EXISTS gl_account_calculations (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  calculation_name VARCHAR(255),
  calculation_type ENUM('SUM_FIELD', 'CUSTOM_QUERY', 'LOOKUP', 'PERCENTAGE', 'CONDITIONAL') NOT NULL,
  calculation_config JSON NOT NULL,
  weight DECIMAL(5,2) DEFAULT 1.0,
  operator ENUM('+', '-', '*', '/') DEFAULT '+',
  sort_order INT DEFAULT 100,
  is_active BOOLEAN DEFAULT TRUE,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id) ON DELETE CASCADE,
  INDEX idx_gl_account (gl_account_id)
);

-- GL MANUAL ENTRIES TABLE (Treasurer adjustments)
CREATE TABLE IF NOT EXISTS gl_manual_entries (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  entry_date DATE NOT NULL,
  description VARCHAR(500),
  amount DECIMAL(15,2) NOT NULL,
  is_debit BOOLEAN,
  entry_reason ENUM('ACCRUAL', 'ADJUSTMENT', 'ALLOCATION', 'RECLASSIFICATION') NOT NULL,
  approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
  created_by_user_id INT NOT NULL,
  approved_by_user_id INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  approved_at TIMESTAMP,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id) ON DELETE CASCADE,
  INDEX idx_date (entry_date),
  INDEX idx_account (gl_account_id),
  INDEX idx_status (approval_status),
  INDEX idx_created_by (created_by_user_id),
  INDEX idx_approved_by (approved_by_user_id)
);

-- GL CONFIGURATION HISTORY (Audit trail)
CREATE TABLE IF NOT EXISTS gl_account_audit (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  changed_by_user_id INT NOT NULL,
  change_type ENUM('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE') NOT NULL,
  old_config JSON,
  new_config JSON,
  change_reason VARCHAR(500),
  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id) ON DELETE CASCADE,
  INDEX idx_account (gl_account_id),
  INDEX idx_date (changed_at),
  INDEX idx_changed_by (changed_by_user_id)
);
```

---

## SECTION 2: V117__Populate_GL_Accounts.sql

**Location**: `backend/src/main/resources/db/migration/V117__Populate_GL_Accounts.sql`

```sql
-- ASSET ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('LOAN_NORMAL', 'Normal Loans', 'ASSET', 'AGGREGATION', 
  JSON_OBJECT('table','loans','field','outstanding_balance','where','loan_type = ''NORMAL'' AND status = ''DISBURSED'''), 10),

('LOAN_EMERGENCY_1', 'Emergency Loan Type 1', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','loans','field','outstanding_balance','where','loan_type = ''EMERGENCY_1'' AND status = ''DISBURSED'''), 11),

('LOAN_EMERGENCY_2', 'Emergency Loan Type 2', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','loans','field','outstanding_balance','where','loan_type = ''EMERGENCY_2'' AND status = ''DISBURSED'''), 12),

('CBA_CALL_DEPOSITS', 'CBA Call Deposits', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SAVINGS account'), 20),

('CBA_CURRENT', 'CBA Current Account', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SAVINGS account'), 21),

('CO_OP_HOLDINGS', 'Co-op Holdings', 'ASSET', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 30),
('COOP_INSURANCE', 'Co-op Insurance', 'ASSET', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 31),
('KUSCCO', 'KUSCCO', 'ASSET', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 32),
('RECEIVABLES', 'Receivables', 'ASSET', 'AGGREGATION', JSON_OBJECT('table','accounts','field','balance'), 33);

-- LIABILITY ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('MEMBER_DEPOSITS', 'Member Deposits', 'LIABILITY', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SAVINGS'), 40),

('MEMBER_SHARES', 'Member Shares', 'LIABILITY', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SHARES'), 41),

('AUDITOR_PAYABLE', 'Auditor Fees Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 50),
('DIVIDEND_PAYABLE', 'Dividend Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 51),
('INTEREST_PAYABLE', 'Interest Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 52),
('COMMITTEE_ALLOWANCE_PAYABLE', 'Committee Allowance Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 53);

-- EQUITY ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('STATUTORY_RESERVE', 'Statutory Reserve', 'EQUITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 60),
('REVENUE_RESERVE', 'Revenue Reserve', 'EQUITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 61),
('RETAINED_EARNINGS', 'Retained Earnings', 'EQUITY', 'COMPUTED', JSON_OBJECT('type','computed'), 62);

-- REVENUE ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('INT_LOANS', 'Interest - Loans', 'REVENUE', 'AGGREGATION',
  JSON_OBJECT('table','transactions','field','amount','where','INTEREST'), 70),

('INT_DEPOSITS', 'Interest - Deposits', 'REVENUE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 71),
('ENTRANCE_FEES', 'Entrance Fees', 'REVENUE', 'AGGREGATION',
  JSON_OBJECT('table','transactions','field','amount','where','ENTRANCE_FEE'), 72),
('LOAN_PROCESSING_FEE', 'Loan Processing Fees', 'REVENUE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 73);

-- EXPENSE ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('AUDIT_FEES', 'Audit Fees', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 80),
('TRAVEL_EXPENSES', 'Travel Expenses', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 81),
('SASRA_FEES', 'SASRA Fees', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 82),
('TRAINING', 'Training', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 83),
('COMMITTEE_ALLOWANCES', 'Committee Allowances', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 84),
('AGM_EXPENSES', 'AGM Expenses', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 85),
('INSURANCE_PREMIUMS', 'Insurance Premiums', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 86),
('BANK_CHARGES', 'Bank Charges', 'EXPENSE', 'AGGREGATION',
  JSON_OBJECT('table','transactions','field','amount','where','BANK_CHARGE'), 87),
('LOAN_LOSS_PROVISION', 'Loan Loss Provision', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 88),
('INCOME_TAX', 'Income Tax', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 89),
('INTEREST_EXPENSE', 'Interest Expense', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 90);
```

---

## SECTION 3: GLAccount.java (Entity)

**Location**: `backend/src/main/java/com/minet/sacco/entity/GLAccount.java`

```java
package com.minet.sacco.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "gl_accounts")
public class GLAccount implements Serializable {
  private static final long serialVersionUID = 1L;
  
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
  @JdbcTypeCode(SqlTypes.JSON)
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
  
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
  
  // Constructors
  public GLAccount() {}
  
  public GLAccount(String code, String name, AccountType accountType, CalculationType balanceCalculationType) {
    this.code = code;
    this.name = name;
    this.accountType = accountType;
    this.balanceCalculationType = balanceCalculationType;
  }
  
  // Getters and Setters
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public AccountType getAccountType() { return accountType; }
  public void setAccountType(AccountType accountType) { this.accountType = accountType; }
  
  public CalculationType getBalanceCalculationType() { return balanceCalculationType; }
  public void setBalanceCalculationType(CalculationType balanceCalculationType) { this.balanceCalculationType = balanceCalculationType; }
  
  public JsonNode getCalculationConfig() { return calculationConfig; }
  public void setCalculationConfig(JsonNode calculationConfig) { this.calculationConfig = calculationConfig; }
  
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
  
  public Integer getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
  
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

---

## SECTION 4: GLManualEntry.java (Entity)

**Location**: `backend/src/main/java/com/minet/sacco/entity/GLManualEntry.java`

```java
package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gl_manual_entries")
public class GLManualEntry implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gl_account_id", nullable = false)
  private GLAccount glAccount;
  
  @Column(nullable = false)
  private LocalDate entryDate;
  
  @Column(length = 500)
  private String description;
  
  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;
  
  @Column(nullable = false)
  private Boolean isDebit;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EntryReason entryReason;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdByUser;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_user_id")
  private User approvedByUser;
  
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
  
  private LocalDateTime approvedAt;
  
  public enum EntryReason {
    ACCRUAL, ADJUSTMENT, ALLOCATION, RECLASSIFICATION
  }
  
  public enum ApprovalStatus {
    PENDING, APPROVED, REJECTED
  }
  
  // Constructors
  public GLManualEntry() {}
  
  public GLManualEntry(GLAccount glAccount, LocalDate entryDate, String description, 
                       BigDecimal amount, Boolean isDebit, EntryReason entryReason, User createdByUser) {
    this.glAccount = glAccount;
    this.entryDate = entryDate;
    this.description = description;
    this.amount = amount;
    this.isDebit = isDebit;
    this.entryReason = entryReason;
    this.createdByUser = createdByUser;
  }
  
  // Getters and Setters
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  
  public GLAccount getGlAccount() { return glAccount; }
  public void setGlAccount(GLAccount glAccount) { this.glAccount = glAccount; }
  
  public LocalDate getEntryDate() { return entryDate; }
  public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
  
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  
  public Boolean getIsDebit() { return isDebit; }
  public void setIsDebit(Boolean isDebit) { this.isDebit = isDebit; }
  
  public EntryReason getEntryReason() { return entryReason; }
  public void setEntryReason(EntryReason entryReason) { this.entryReason = entryReason; }
  
  public ApprovalStatus getApprovalStatus() { return approvalStatus; }
  public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
  
  public User getCreatedByUser() { return createdByUser; }
  public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }
  
  public User getApprovedByUser() { return approvedByUser; }
  public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
  
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  
  public LocalDateTime getApprovedAt() { return approvedAt; }
  public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
```

---

## SECTION 5: GLCalculationService.java

**Location**: `backend/src/main/java/com/minet/sacco/service/GLCalculationService.java`

```java
package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.AccountType;
import com.minet.sacco.entity.GLManualEntry;
import com.minet.sacco.repository.*;
import com.minet.sacco.dto.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GLCalculationService {
  private static final Logger logger = LoggerFactory.getLogger(GLCalculationService.class);
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  @Autowired
  private GLManualEntryRepository glManualEntryRepository;
  
  @Autowired
  private LoanRepository loanRepository;
  
  @Autowired
  private AccountRepository accountRepository;
  
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
          return calculateAggregation(account.getCalculationConfig(), asOfDate, account.getCode());
        
        case FORMULA:
          return calculateFormula(account.getCalculationConfig(), asOfDate);
        
        case MANUAL_ENTRY:
          return calculateManualEntry(account.getId(), asOfDate);
        
        case COMPUTED:
          return calculateComputed(account.getCalculationConfig(), asOfDate, account.getCode());
        
        default:
          return ZERO;
      }
    } catch (Exception e) {
      logger.error("Error calculating GL account balance: " + account.getCode(), e);
      return ZERO;
    }
  }
  
  /**
   * AGGREGATION: SUM from table/field
   */
  private BigDecimal calculateAggregation(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate, String code) {
    if (config == null) {
      return ZERO;
    }
    
    String table = config.has("table") ? config.get("table").asText() : null;
    String field = config.has("field") ? config.get("field").asText() : null;
    String whereClause = config.has("where") ? config.get("where").asText() : null;
    
    // Route to appropriate repository based on account code
    switch (code) {
      case "LOAN_NORMAL":
      case "LOAN_EMERGENCY_1":
      case "LOAN_EMERGENCY_2":
        return calculateLoansAggregation(code, asOfDate);
      
      case "MEMBER_DEPOSITS":
      case "MEMBER_SHARES":
        return calculateAccountsAggregation(code, asOfDate);
      
      default:
        return ZERO;
    }
  }
  
  /**
   * Calculate loan aggregations
   */
  private BigDecimal calculateLoansAggregation(String code, LocalDate asOfDate) {
    try {
      return loanRepository.findAll().stream()
        .filter(l -> {
          // Filter by loan product type (from loanProduct relationship)
          if (l.getLoanProduct() == null) return false;
          
          String productName = l.getLoanProduct().getName();
          if (productName == null) return false;
          
          if ("LOAN_NORMAL".equals(code)) {
            return productName.contains("NORMAL");
          } else if ("LOAN_EMERGENCY_1".equals(code)) {
            return productName.contains("EMERGENCY_1");
          } else if ("LOAN_EMERGENCY_2".equals(code)) {
            return productName.contains("EMERGENCY_2");
          }
          return false;
        })
        .filter(l -> l.getStatus() == com.minet.sacco.entity.Loan.Status.DISBURSED)
        .map(l -> l.getOutstandingBalance() != null ? 
          new BigDecimal(l.getOutstandingBalance().toString()) : ZERO)
        .reduce(ZERO, BigDecimal::add);
    } catch (Exception e) {
      logger.warn("Error calculating loans aggregation for " + code, e);
      return ZERO;
    }
  }
  
  /**
   * Calculate account aggregations
   */
  private BigDecimal calculateAccountsAggregation(String code, LocalDate asOfDate) {
    try {
      return accountRepository.findAll().stream()
        .filter(a -> {
          if ("MEMBER_DEPOSITS".equals(code)) {
            return "SAVINGS".equals(a.getAccountType());
          } else if ("MEMBER_SHARES".equals(code)) {
            return "SHARES".equals(a.getAccountType());
          }
          return false;
        })
        .map(a -> a.getBalance() != null ? 
          new BigDecimal(a.getBalance().toString()) : ZERO)
        .reduce(ZERO, BigDecimal::add);
    } catch (Exception e) {
      logger.warn("Error calculating accounts aggregation for " + code, e);
      return ZERO;
    }
  }
  
  /**
   * FORMULA: Math calculation on other GL accounts
   */
  private BigDecimal calculateFormula(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate) {
    // TODO: Implement expression evaluation
    return ZERO;
  }
  
  /**
   * MANUAL_ENTRY: Sum of approved treasurer-entered values
   */
  private BigDecimal calculateManualEntry(Integer glAccountId, LocalDate asOfDate) {
    try {
      return glManualEntryRepository.findByGlAccountIdOrderByCreatedAtDesc(glAccountId)
        .stream()
        .filter(entry -> entry.getApprovalStatus() == com.minet.sacco.entity.GLManualEntry.ApprovalStatus.APPROVED)
        .filter(entry -> entry.getEntryDate().compareTo(asOfDate) <= 0)
        .map(entry -> {
          if (entry.getIsDebit()) {
            return entry.getAmount();
          } else {
            return entry.getAmount().negate();
          }
        })
        .reduce(ZERO, BigDecimal::add);
    } catch (Exception e) {
      logger.warn("Error calculating manual entries for account " + glAccountId, e);
      return ZERO;
    }
  }
  
  /**
   * COMPUTED: Custom complex logic
   */
  private BigDecimal calculateComputed(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate, String code) {
    // For now, return zero (computed values not yet implemented)
    return ZERO;
  }
  
  /**
   * Generate Trial Balance for a date
   */
  public TrialBalanceDTO generateTrialBalance(LocalDate asOfDate) {
    List<GLAccount> activeAccounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
    
    List<TrialBalanceLineDTO> lines = new ArrayList<>();
    BigDecimal totalDebit = ZERO;
    BigDecimal totalCredit = ZERO;
    
    for (GLAccount acc : activeAccounts) {
      BigDecimal balance = calculateBalance(acc, asOfDate);
      
      if (balance.compareTo(ZERO) != 0) {
        Boolean isDebit = isDebit(acc.getAccountType(), balance);
        lines.add(new TrialBalanceLineDTO(
          acc.getCode(),
          acc.getName(),
          acc.getAccountType().toString(),
          balance.abs(),
          isDebit
        ));
        
        if (isDebit) {
          totalDebit = totalDebit.add(balance.abs());
        } else {
          totalCredit = totalCredit.add(balance.abs());
        }
      }
    }
    
    return new TrialBalanceDTO(asOfDate, lines);
  }
  
  private Boolean isDebit(AccountType type, BigDecimal balance) {
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

---

## SECTION 6: BalanceSheetService.java

**Location**: `backend/src/main/java/com/minet/sacco/service/BalanceSheetService.java`

```java
package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.dto.*;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.AccountType;
import com.minet.sacco.repository.GLAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BalanceSheetService {
  private static final Logger logger = LoggerFactory.getLogger(BalanceSheetService.class);
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  /**
   * Generate Balance Sheet for a specific date
   * Assets = Liabilities + Equity
   */
  public BalanceSheetDTO generateBalanceSheet(LocalDate asOfDate) {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    
    // Get all active GL accounts by type
    List<GLAccount> assets = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.ASSET);
    List<GLAccount> liabilities = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.LIABILITY);
    List<GLAccount> equity = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EQUITY);
    
    // Calculate lines for each section
    List<BalanceSheetLineDTO> assetLines = calculateBalanceSheetLines(assets, asOfDate);
    List<BalanceSheetLineDTO> liabilityLines = calculateBalanceSheetLines(liabilities, asOfDate);
    List<BalanceSheetLineDTO> equityLines = calculateBalanceSheetLines(equity, asOfDate);
    
    // Calculate totals
    BigDecimal totalAssets = assetLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalLiabilities = liabilityLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalEquity = equityLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Check if balanced: Assets = Liabilities + Equity
    BigDecimal leftSide = totalAssets;
    BigDecimal rightSide = totalLiabilities.add(totalEquity);
    Boolean isBalanced = leftSide.compareTo(rightSide) == 0;
    
    if (!isBalanced) {
      logger.warn("Balance Sheet not balanced for date " + asOfDate + ". Assets: " + leftSide + ", Liabilities+Equity: " + rightSide);
    }
    
    return new BalanceSheetDTO(
      asOfDate,
      assetLines, totalAssets,
      liabilityLines, totalLiabilities,
      equityLines, totalEquity,
      isBalanced
    );
  }
  
  /**
   * Calculate balance sheet lines for a list of GL accounts
   */
  private List<BalanceSheetLineDTO> calculateBalanceSheetLines(List<GLAccount> accounts, LocalDate asOfDate) {
    return accounts.stream()
      .map(acc -> {
        BigDecimal balance = glCalculationService.calculateGLAccountBalance(acc.getId(), asOfDate);
        return new BalanceSheetLineDTO(
          acc.getCode(),
          acc.getName(),
          balance
        );
      })
      .filter(line -> line.getAmount() != null && line.getAmount().compareTo(BigDecimal.ZERO) != 0)
      .sorted((a, b) -> {
        // Sort by amount descending
        return b.getAmount().compareTo(a.getAmount());
      })
      .collect(Collectors.toList());
  }
}
```

---

## SECTION 7: IncomeStatementService.java

**Location**: `backend/src/main/java/com/minet/sacco/service/IncomeStatementService.java`

```java
package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.dto.*;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.AccountType;
import com.minet.sacco.repository.GLAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IncomeStatementService {
  private static final Logger logger = LoggerFactory.getLogger(IncomeStatementService.class);
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  /**
   * Generate Income Statement for a date range
   * Net Income = Total Revenues - Total Expenses
   */
  public IncomeStatementDTO generateIncomeStatement(LocalDate fromDate, LocalDate toDate) {
    if (toDate == null) {
      toDate = LocalDate.now();
    }
    if (fromDate == null) {
      // Default to beginning of current month
      fromDate = toDate.withDayOfMonth(1);
    }
    
    // Get all revenue and expense GL accounts
    List<GLAccount> revenues = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.REVENUE);
    List<GLAccount> expenses = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EXPENSE);
    
    // Calculate lines
    List<IncomeStatementLineDTO> revenueLines = calculateIncomeStatementLines(revenues, toDate);
    List<IncomeStatementLineDTO> expenseLines = calculateIncomeStatementLines(expenses, toDate);
    
    // Calculate totals
    BigDecimal totalRevenues = revenueLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalExpenses = expenseLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Calculate net income
    BigDecimal netIncome = totalRevenues.subtract(totalExpenses);
    
    logger.info("Income Statement generated: From=" + fromDate + ", To=" + toDate + 
             ", Revenue=" + totalRevenues + ", Expenses=" + totalExpenses + ", NetIncome=" + netIncome);
    
    return new IncomeStatementDTO(
      fromDate,
      toDate,
      revenueLines,
      totalRevenues,
      expenseLines,
      totalExpenses,
      netIncome
    );
  }
  
  /**
   * Calculate income statement lines for a list of GL accounts
   */
  private List<IncomeStatementLineDTO> calculateIncomeStatementLines(List<GLAccount> accounts, LocalDate asOfDate) {
    return accounts.stream()
      .map(acc -> {
        BigDecimal balance = glCalculationService.calculateGLAccountBalance(acc.getId(), asOfDate);
        return new IncomeStatementLineDTO(
          acc.getCode(),
          acc.getName(),
          balance.abs()  // Show absolute value
        );
      })
      .filter(line -> line.getAmount() != null && line.getAmount().compareTo(BigDecimal.ZERO) != 0)
      .sorted((a, b) -> {
        // Sort by amount descending
        return b.getAmount().compareTo(a.getAmount());
      })
      .collect(Collectors.toList());
  }
}
```

---

## APPENDIX: Key Concepts

### Account Types (From GLAccount.AccountType enum)
- **ASSET**: Loans, deposits, holdings (normally debit balance)
- **LIABILITY**: Member deposits, shares, payables (normally credit balance)
- **EQUITY**: Reserves, retained earnings (normally credit balance)
- **REVENUE**: Interest, fees, income (normally credit balance)
- **EXPENSE**: Audit fees, travel, training (normally debit balance)

### Calculation Types (From GLAccount.CalculationType enum)
- **AGGREGATION**: Sum from operational tables (loans, accounts, transactions). Example: LOAN_NORMAL sums all disbursed normal loans
- **FORMULA**: Math operations on other GL accounts (not yet implemented)
- **MANUAL_ENTRY**: Sum of approved treasurer-entered values from gl_manual_entries table
- **COMPUTED**: Complex custom logic (not yet implemented)

### Data Flow

**AGGREGATION Example** (Automatic):
1. Treasurer has a Normal Loan GL account (code: LOAN_NORMAL, type: AGGREGATION)
2. System queries: SELECT SUM(outstanding_balance) FROM loans WHERE loan_type='NORMAL' AND status='DISBURSED'
3. Result automatically used in Trial Balance, Balance Sheet, Income Statement

**MANUAL_ENTRY Example** (Treasurer-Driven):
1. Treasurer creates GL account (code: BANK_CHARGES, type: MANUAL_ENTRY)
2. Treasurer creates entry: amount 500, date 2026-06-01, status PENDING
3. Admin approves entry → status becomes APPROVED
4. System queries: SELECT SUM(amount) FROM gl_manual_entries WHERE gl_account_id=X AND approval_status='APPROVED' AND entry_date <= asOfDate
5. Result automatically used in Trial Balance, Balance Sheet, Income Statement

### Database Schema Relationships

```
gl_accounts (master table)
  ├── id (PK)
  ├── code (UNIQUE)
  ├── name
  ├── account_type (ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE)
  ├── balance_calculation_type (AGGREGATION/FORMULA/MANUAL_ENTRY/COMPUTED)
  ├── calculation_config (JSON)
  └── display_order

gl_manual_entries (treasurer entries)
  ├── id (PK)
  ├── gl_account_id (FK to gl_accounts)
  ├── entry_date
  ├── amount
  ├── is_debit
  ├── approval_status (PENDING/APPROVED/REJECTED)
  ├── created_by_user_id (FK to user)
  └── approved_by_user_id (FK to user)

gl_account_audit (configuration history)
  ├── id (PK)
  ├── gl_account_id (FK to gl_accounts)
  ├── change_type (CREATE/UPDATE/DELETE/ACTIVATE/DEACTIVATE)
  ├── old_config (JSON)
  └── new_config (JSON)
```

---

**Document End**
