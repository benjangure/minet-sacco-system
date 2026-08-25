package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.AccountType;
import com.minet.sacco.entity.GLManualEntry;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.repository.*;
import com.minet.sacco.dto.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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

  @Autowired
  private TransactionRepository transactionRepository;
  
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
   * Calculate balance for a GL account as of a specific date with optional period filtering
   */
  public BigDecimal calculateGLAccountBalance(Integer glAccountId, LocalDate asOfDate,
                                               Integer periodMonth, Integer periodYear) {
    GLAccount account = glAccountRepository.findById(glAccountId)
      .orElseThrow(() -> new RuntimeException("GL Account not found: " + glAccountId));
    
    return calculateBalance(account, asOfDate, periodMonth, periodYear);
  }
  
  /**
   * Main calculation dispatcher
   */
  private BigDecimal calculateBalance(GLAccount account, LocalDate asOfDate) {
    return calculateBalance(account, asOfDate, null, null);
  }
  
  /**
   * Main calculation dispatcher with optional period params
   */
  private BigDecimal calculateBalance(GLAccount account, LocalDate asOfDate,
                                      Integer periodMonth, Integer periodYear) {
    try {
      switch (account.getBalanceCalculationType()) {
        case AGGREGATION:
          return calculateAggregation(account.getCalculationConfig(), asOfDate, account.getCode(), periodMonth, periodYear);
        
        case FORMULA:
          return calculateFormula(account.getCalculationConfig(), asOfDate);
        
        case MANUAL_ENTRY:
          return calculateManualEntry(account.getId(), asOfDate, periodMonth, periodYear);
        
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
   * AGGREGATION: SUM from table/field - dynamic routing based on table field in config
   */
  private BigDecimal calculateAggregation(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate,
                                          String code, Integer periodMonth, Integer periodYear) {
    if (config == null) return ZERO;
    String table = config.has("table") ? config.get("table").asText() : null;
    if (table == null) return ZERO;
    
    switch (table) {
      case "loans":
        return calculateLoansAggregation(config, asOfDate);
      case "accounts":
        return calculateAccountsAggregation(config, asOfDate);
      case "transactions":
        return calculateTransactionsAggregation(config, asOfDate, periodMonth, periodYear);
      default:
        logger.warn("Unknown aggregation table: " + table);
        return ZERO;
    }
  }
  
  /**
   * Calculate loan aggregations from config — DATE-AWARE via disbursementDate
   */
  private BigDecimal calculateLoansAggregation(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate) {
    try {
      Integer loanProductId = config.has("loanProductId") ? config.get("loanProductId").asInt() : null;
      LocalDateTime asOf = asOfDate.atTime(23, 59, 59);

      if (loanProductId != null) {
        BigDecimal result = loanRepository.sumOutstandingBalanceAsOfByProduct(asOf, loanProductId);
        return result != null ? result : ZERO;
      } else {
        BigDecimal result = loanRepository.sumOutstandingBalanceAsOf(asOf);
        return result != null ? result : ZERO;
      }
    } catch (Exception e) {
      logger.warn("Error calculating loans aggregation", e);
      return ZERO;
    }
  }

  /**
   * Calculate account aggregations from config — DATE-AWARE via transaction history
   */
  private BigDecimal calculateAccountsAggregation(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate) {
    try {
      String accountTypeStr = config.has("accountType") ? config.get("accountType").asText() : null;
      if (accountTypeStr == null || accountTypeStr.isBlank()) {
        // Fallback: also check old "where" key for backwards compatibility
        if (config.has("where")) {
          accountTypeStr = config.get("where").asText().trim().toUpperCase();
        }
      }

      if (accountTypeStr == null || accountTypeStr.isBlank()) {
        // No filter — sum all accounts (use live balance, no type filter)
        return accountRepository.findAll().stream()
            .map(a -> a.getBalance() != null ? a.getBalance() : ZERO)
            .reduce(ZERO, BigDecimal::add);
      }

      try {
        com.minet.sacco.entity.Account.AccountType accountType =
            com.minet.sacco.entity.Account.AccountType.valueOf(accountTypeStr.toUpperCase());
        // Use date-aware query: reconstruct balance from transactions up to asOfDate
        java.time.LocalDateTime asOf = asOfDate.atTime(23, 59, 59);
        BigDecimal result = accountRepository.sumAccountBalanceAsOf(accountType, asOf);
        return result != null ? result : ZERO;
      } catch (IllegalArgumentException ex) {
        logger.warn("Unknown account type in GL config: " + accountTypeStr);
        return ZERO;
      }
    } catch (Exception e) {
      logger.warn("Error calculating accounts aggregation", e);
      return ZERO;
    }
  }
  
  /**
   * Calculate transaction aggregations from config
   */
  private BigDecimal calculateTransactionsAggregation(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate,
                                                      Integer periodMonth, Integer periodYear) {
    try {
      LocalDateTime computedStartDateTime = null;
      LocalDateTime computedEndDateTime;

      if (periodMonth != null && periodYear != null) {
        YearMonth yearMonth = YearMonth.of(periodYear, periodMonth);
        computedStartDateTime = yearMonth.atDay(1).atStartOfDay();
        computedEndDateTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);
      } else {
        computedEndDateTime = asOfDate.atTime(23, 59, 59);
      }

      // Must be effectively final for lambda usage below
      final LocalDateTime startDateTime = computedStartDateTime;
      final LocalDateTime endDateTime = computedEndDateTime;

      String transactionType = extractTransactionType(config);
      String descriptionKeyword = config.has("keyword") ? config.get("keyword").asText(null) : null;

      if ("INTEREST".equalsIgnoreCase(transactionType)) {
        return calculateLoanInterestAggregation(startDateTime, endDateTime);
      }

      return transactionRepository.findAll().stream()
        .filter(transaction -> isWithinTransactionRange(transaction.getTransactionDate(), startDateTime, endDateTime))
        .filter(transaction -> transactionType == null ||
          (transaction.getTransactionType() != null &&
            transaction.getTransactionType().name().equalsIgnoreCase(transactionType)))
        .filter(transaction -> {
          if (descriptionKeyword == null || descriptionKeyword.isBlank()) {
            return true;
          }
          return transaction.getDescription() != null &&
            transaction.getDescription().toLowerCase().contains(descriptionKeyword.toLowerCase());
        })
        .map(Transaction::getAmount)
        .filter(Objects::nonNull)
        .reduce(ZERO, BigDecimal::add);
    } catch (Exception e) {
      logger.warn("Error calculating transactions aggregation", e);
      return ZERO;
    }
  }

  private String extractTransactionType(com.fasterxml.jackson.databind.JsonNode config) {
    if (config == null) {
      return null;
    }

    if (config.has("transactionType")) {
      return config.get("transactionType").asText();
    }

    if (!config.has("where")) {
      return null;
    }

    String where = config.get("where").asText();
    if (where == null || where.isBlank()) {
      return null;
    }

    String normalized = where.trim();
    if ("INTEREST".equalsIgnoreCase(normalized)) {
      return "INTEREST";
    }

    String upper = normalized.toUpperCase(Locale.ROOT);
    if (upper.contains("INTEREST")) {
      return "INTEREST";
    }
    if (upper.contains("LOAN_REPAYMENT")) {
      return "LOAN_REPAYMENT";
    }
    if (upper.contains("DEPOSIT")) {
      return "DEPOSIT";
    }
    if (upper.contains("WITHDRAWAL")) {
      return "WITHDRAWAL";
    }
    if (upper.contains("LOAN_DISBURSEMENT")) {
      return "LOAN_DISBURSEMENT";
    }

    return null;
  }

  private boolean isWithinTransactionRange(LocalDateTime transactionDate, LocalDateTime startDateTime, LocalDateTime endDateTime) {
    if (transactionDate == null) {
      return false;
    }

    if (startDateTime != null) {
      return !transactionDate.isBefore(startDateTime) && !transactionDate.isAfter(endDateTime);
    }

    return !transactionDate.isAfter(endDateTime);
  }

  private BigDecimal calculateLoanInterestAggregation(LocalDateTime startDateTime, LocalDateTime endDateTime) {
    BigDecimal transactionInterest = transactionRepository.findAll().stream()
      .filter(transaction -> transaction.getTransactionType() == Transaction.TransactionType.INTEREST)
      .filter(transaction -> isWithinTransactionRange(transaction.getTransactionDate(), startDateTime, endDateTime))
      .map(Transaction::getAmount)
      .filter(Objects::nonNull)
      .reduce(ZERO, BigDecimal::add);

    if (transactionInterest.compareTo(ZERO) > 0) {
      return transactionInterest;
    }

    if (startDateTime != null) {
      BigDecimal periodInterest = loanRepository.sumInterestIncomeInPeriod(startDateTime, endDateTime);
      return periodInterest != null ? periodInterest : ZERO;
    }

    return loanRepository.findAll().stream()
      .filter(loan -> loan.getStatus() == com.minet.sacco.entity.Loan.Status.DISBURSED ||
                      loan.getStatus() == com.minet.sacco.entity.Loan.Status.REPAID)
      .filter(loan -> loan.getDisbursementDate() != null && !loan.getDisbursementDate().isAfter(endDateTime))
      .map(loan -> loan.getTotalInterest() != null ? loan.getTotalInterest() : ZERO)
      .reduce(ZERO, BigDecimal::add);
  }
  
  /**
   * FORMULA: Derives this account's balance from other GL account balances.
   *
   * Config examples:
   *   {"formula": "REVENUE - EXPENSE"}          → net income
   *   {"formula": "ASSET - LIABILITY - EQUITY"}  → retained earnings check
   *   {"formula": "1001 - 2001"}                 → difference of two specific codes
   *
   * Operators supported: +  -  *
   * Operands: a GL account CODE (string), or an AccountType keyword
   *           (ASSET / LIABILITY / EQUITY / REVENUE / EXPENSE) which sums all accounts of that type.
   */
  private BigDecimal calculateFormula(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate) {
    if (config == null || !config.has("formula")) {
      logger.warn("FORMULA account has no 'formula' key in config — returning 0");
      return ZERO;
    }

    String formula = config.get("formula").asText("").trim();
    if (formula.isBlank()) return ZERO;

    try {
      // Tokenise: split on +/- while keeping the operator
      // e.g. "REVENUE - EXPENSE + 9001" → ["REVENUE", "-", "EXPENSE", "+", "9001"]
      String[] tokens = formula.split("(?<=[+\\-*])|(?=[+\\-*])");
      BigDecimal result = null;
      String pendingOp = "+";

      for (String raw : tokens) {
        String token = raw.trim();
        if (token.isEmpty()) continue;
        if (token.equals("+") || token.equals("-") || token.equals("*")) {
          pendingOp = token;
          continue;
        }

        BigDecimal operandValue = resolveFormulaOperand(token, asOfDate);
        if (result == null) {
          result = operandValue;
        } else {
          switch (pendingOp) {
            case "+": result = result.add(operandValue); break;
            case "-": result = result.subtract(operandValue); break;
            case "*": result = result.multiply(operandValue); break;
            default:  result = result.add(operandValue);
          }
        }
        pendingOp = "+";
      }

      return result != null ? result.abs() : ZERO;

    } catch (Exception e) {
      logger.error("Error evaluating GL formula '" + formula + "': " + e.getMessage(), e);
      return ZERO;
    }
  }

  /**
   * Resolve a single formula operand — either a GL account CODE or an AccountType keyword.
   */
  private BigDecimal resolveFormulaOperand(String token, LocalDate asOfDate) {
    // Check if it is an AccountType keyword
    try {
      AccountType type = AccountType.valueOf(token.toUpperCase());
      return glAccountRepository.findByAccountTypeAndIsActiveTrue(type).stream()
          .map(acc -> calculateBalance(acc, asOfDate, null, null))
          .reduce(ZERO, BigDecimal::add);
    } catch (IllegalArgumentException ignored) {
      // Not an account type keyword — treat as account code
    }

    // Look up by account code
    return glAccountRepository.findByCode(token)
        .map(acc -> calculateBalance(acc, asOfDate, null, null))
        .orElseGet(() -> {
          // Could be a literal numeric constant
          try { return new BigDecimal(token); }
          catch (NumberFormatException ex) {
            logger.warn("Formula operand '" + token + "' is neither an account code, account type, nor a number");
            return ZERO;
          }
        });
  }

  /**
   * COMPUTED: Well-known derived values that don't map to a simple formula.
   *
   * Supported 'compute' keys:
   *   RETAINED_EARNINGS  → cumulative net income (all REVENUE - all EXPENSE)
   *   NET_INCOME         → same as RETAINED_EARNINGS but semantically period income
   *   TOTAL_EQUITY       → sum of all EQUITY accounts (excluding self)
   *   BALANCE_CHECK      → Assets - (Liabilities + Equity)  [should be 0]
   */
  private BigDecimal calculateComputed(com.fasterxml.jackson.databind.JsonNode config, LocalDate asOfDate, String selfCode) {
    String compute = config != null && config.has("compute")
        ? config.get("compute").asText("RETAINED_EARNINGS").toUpperCase()
        : "RETAINED_EARNINGS";

    try {
      switch (compute) {
        case "RETAINED_EARNINGS":
        case "NET_INCOME": {
          BigDecimal totalRevenue = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.REVENUE).stream()
              .map(acc -> calculateBalance(acc, asOfDate, null, null).abs())
              .reduce(ZERO, BigDecimal::add);
          BigDecimal totalExpense = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EXPENSE).stream()
              .map(acc -> calculateBalance(acc, asOfDate, null, null).abs())
              .reduce(ZERO, BigDecimal::add);
          return totalRevenue.subtract(totalExpense);
        }

        case "TOTAL_EQUITY": {
          return glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EQUITY).stream()
              .filter(acc -> !acc.getCode().equals(selfCode)) // exclude self to avoid recursion
              .map(acc -> calculateBalance(acc, asOfDate, null, null))
              .reduce(ZERO, BigDecimal::add);
        }

        case "BALANCE_CHECK": {
          BigDecimal totalAssets = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.ASSET).stream()
              .map(acc -> calculateBalance(acc, asOfDate, null, null).abs())
              .reduce(ZERO, BigDecimal::add);
          BigDecimal totalLiabilities = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.LIABILITY).stream()
              .map(acc -> calculateBalance(acc, asOfDate, null, null).abs())
              .reduce(ZERO, BigDecimal::add);
          BigDecimal totalEquity = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EQUITY).stream()
              .filter(acc -> !acc.getCode().equals(selfCode))
              .map(acc -> calculateBalance(acc, asOfDate, null, null).abs())
              .reduce(ZERO, BigDecimal::add);
          return totalAssets.subtract(totalLiabilities.add(totalEquity));
        }

        default:
          logger.warn("Unknown computed type '{}' for GL account '{}'", compute, selfCode);
          return ZERO;
      }
    } catch (Exception e) {
      logger.error("Error calculating computed account {}: {}", selfCode, e.getMessage(), e);
      return ZERO;
    }
  }
  
  /**
   * MANUAL_ENTRY: Sum of approved treasurer-entered values
   */
  private BigDecimal calculateManualEntry(Integer glAccountId, LocalDate asOfDate) {
    return calculateManualEntry(glAccountId, asOfDate, null, null);
  }
  
  /**
   * MANUAL_ENTRY: Sum of approved treasurer-entered values, with optional period filtering
   */
  private BigDecimal calculateManualEntry(Integer glAccountId, LocalDate asOfDate,
                                          Integer periodMonth, Integer periodYear) {
    try {
      GLAccount account = glAccountRepository.findById(glAccountId)
        .orElse(null);
      if (account == null) return ZERO;
      
      return glManualEntryRepository.findByGlAccountIdOrderByCreatedAtDesc(glAccountId)
        .stream()
        .filter(e -> e.getApprovalStatus() ==
                     GLManualEntry.ApprovalStatus.APPROVED)
        .filter(e -> {
          if (Boolean.TRUE.equals(account.getPeriodSensitive())
                  && periodMonth != null && periodYear != null) {
            // Filter to exact period only
            return periodMonth.equals(e.getPeriodMonth()) &&
                   periodYear.equals(e.getPeriodYear());
          } else {
            // Cumulative — all entries up to asOfDate
            return e.getEntryDate() != null &&
                   !e.getEntryDate().isAfter(asOfDate);
          }
        })
        .map(e -> Boolean.TRUE.equals(e.getIsDebit()) ?
                  e.getAmount() : e.getAmount().negate())
        .reduce(ZERO, BigDecimal::add);
    } catch (Exception e) {
      logger.warn("Error calculating manual entries for account " + glAccountId, e);
      return ZERO;
    }
  }
  
  /**
   * Generate Trial Balance for a date
   */
  public TrialBalanceDTO generateTrialBalance(LocalDate asOfDate) {
    return generateTrialBalance(asOfDate, null, null);
  }
  
  /**
   * Generate Trial Balance for a date with optional period filtering
   */
  public TrialBalanceDTO generateTrialBalance(LocalDate asOfDate,
                                               Integer periodMonth, Integer periodYear) {
    List<GLAccount> activeAccounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
    
    List<TrialBalanceLineDTO> lines = new ArrayList<>();
    BigDecimal totalDebit = ZERO;
    BigDecimal totalCredit = ZERO;
    
    for (GLAccount acc : activeAccounts) {
      BigDecimal balance = calculateBalance(acc, asOfDate, periodMonth, periodYear);
      
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
