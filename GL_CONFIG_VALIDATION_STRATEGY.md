# GL Configuration Validation Strategy

## Problem
JSON-based calculation configs are flexible but dangerous. Bad configs can:
- ❌ Cause silent NULL returns (no error, just missing data)
- ❌ Break trial balance calculation mid-report
- ❌ SQL injection if not sanitized
- ❌ Circular dependencies (GL account A depends on B depends on A)
- ❌ Invalid field references (query column that doesn't exist)

## Solution: Multi-Layer Validation

---

## Layer 1: Configuration Syntax Validation

**When**: Before saving calculation config
**What**: Validate JSON structure matches known types

### Supported Calculation Types & Required Fields

```java
public enum CalculationType {
  // Type 1: Aggregation from single table
  SUM_FIELD {
    required: ["table", "field", "where"],
    optional: ["date_field"],
    example: {
      "type": "SUM_FIELD",
      "table": "loans",
      "field": "disbursed_amount",
      "where": "loan_type = 'NORMAL' AND status != 'CANCELLED'",
      "date_field": "disbursement_date"
    }
  },
  
  // Type 2: Custom SQL query
  CUSTOM_QUERY {
    required: ["query"],
    optional: ["params", "description"],
    example: {
      "type": "CUSTOM_QUERY",
      "query": "SELECT SUM(amount) FROM transactions WHERE created_at >= ? AND created_at < ?",
      "params": ["date_from", "date_to"],
      "description": "Sum of interest received in period"
    }
  },
  
  // Type 3: Reference another GL account
  GL_LOOKUP {
    required: ["gl_account_code"],
    example: {
      "type": "GL_LOOKUP",
      "gl_account_code": "MEMBER_DEPOSITS"
    }
  },
  
  // Type 4: Formula from other GL accounts
  FORMULA {
    required: ["formula", "references"],
    optional: ["operator"],
    example: {
      "type": "FORMULA",
      "formula": "(MEMBER_DEPOSITS * DIVIDEND_RATE / 100)",
      "references": ["MEMBER_DEPOSITS", "DIVIDEND_RATE"],
      "operator": "+"
    }
  },
  
  // Type 5: Manual entry only
  MANUAL_ENTRY {
    required: ["entry_reason"],
    optional: ["min_value", "max_value"],
    example: {
      "type": "MANUAL_ENTRY",
      "entry_reason": "ACCRUAL",
      "min_value": 0,
      "max_value": 10000000
    }
  }
}
```

### Validation Rules

```java
@Service
public class GLConfigValidator {

  public ValidationResult validateCalculationConfig(
      GLAccount account,
      CalculationConfig config) {
    
    List<ValidationError> errors = new ArrayList<>();
    
    // 1. Type exists
    if (!CalculationType.contains(config.getType())) {
      errors.add(new ValidationError("Invalid calculation type: " + config.getType()));
    }
    
    // 2. Required fields present
    CalculationType type = CalculationType.valueOf(config.getType());
    for (String required : type.getRequiredFields()) {
      if (!config.hasField(required)) {
        errors.add(new ValidationError("Missing required field: " + required));
      }
    }
    
    // 3. Type-specific validation
    switch (config.getType()) {
      case SUM_FIELD:
        errors.addAll(validateSumField(config));
        break;
      case CUSTOM_QUERY:
        errors.addAll(validateCustomQuery(config));
        break;
      case GL_LOOKUP:
        errors.addAll(validateGLLookup(config));
        break;
      case FORMULA:
        errors.addAll(validateFormula(config, account));
        break;
    }
    
    return new ValidationResult(errors.isEmpty(), errors);
  }
  
  // Validate SUM_FIELD config
  private List<ValidationError> validateSumField(CalculationConfig config) {
    List<ValidationError> errors = new ArrayList<>();
    String table = config.getString("table");
    String field = config.getString("field");
    String where = config.getString("where");
    
    // 1. Table exists
    if (!tableExists(table)) {
      errors.add(new ValidationError("Table does not exist: " + table));
      return errors; // Stop here, can't validate further
    }
    
    // 2. Field exists in table
    if (!fieldExistsInTable(table, field)) {
      errors.add(new ValidationError(
        String.format("Field '%s' does not exist in table '%s'", field, table)
      ));
    }
    
    // 3. WHERE clause is valid SQL (sanitize & parse)
    try {
      validateWhereClause(where, table);
    } catch (SQLValidationException e) {
      errors.add(new ValidationError("Invalid WHERE clause: " + e.getMessage()));
    }
    
    // 4. Field is numeric (for SUM)
    if (!isNumericField(table, field)) {
      errors.add(new ValidationError(
        String.format("Field '%s' is not numeric, cannot SUM", field)
      ));
    }
    
    return errors;
  }
  
  // Validate custom SQL
  private List<ValidationError> validateCustomQuery(CalculationConfig config) {
    List<ValidationError> errors = new ArrayList<>();
    String query = config.getString("query");
    
    // 1. Query must be SELECT only (no INSERT, UPDATE, DELETE)
    if (!query.trim().toUpperCase().startsWith("SELECT")) {
      errors.add(new ValidationError("Only SELECT queries allowed"));
    }
    
    // 2. No dangerous keywords
    String[] forbidden = {"DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "GRANT"};
    for (String keyword : forbidden) {
      if (query.toUpperCase().contains(keyword)) {
        errors.add(new ValidationError("Dangerous SQL keyword: " + keyword));
      }
    }
    
    // 3. Must return exactly one numeric value
    // (validate by checking if query structure ends with numeric aggregation)
    if (!query.toUpperCase().contains("SUM(") 
        && !query.toUpperCase().contains("COUNT(")
        && !query.toUpperCase().contains("AVG(")) {
      errors.add(new ValidationError(
        "Query must return a single numeric value (use SUM, COUNT, AVG)"
      ));
    }
    
    return errors;
  }
  
  // Validate GL account reference
  private List<ValidationError> validateGLLookup(CalculationConfig config) {
    List<ValidationError> errors = new ArrayList<>();
    String glCode = config.getString("gl_account_code");
    
    // 1. Referenced GL account exists
    if (!glAccountRepository.existsByCode(glCode)) {
      errors.add(new ValidationError("Referenced GL account does not exist: " + glCode));
    }
    
    return errors;
  }
  
  // Validate formula references
  private List<ValidationError> validateFormula(
      CalculationConfig config,
      GLAccount currentAccount) {
    
    List<ValidationError> errors = new ArrayList<>();
    String formula = config.getString("formula");
    List<String> references = config.getList("references");
    
    // 1. All references exist as GL accounts
    for (String ref : references) {
      if (!glAccountRepository.existsByCode(ref)) {
        errors.add(new ValidationError("Referenced GL account does not exist: " + ref));
      }
    }
    
    // 2. Check for circular dependencies
    if (hasCyclicDependency(currentAccount, references)) {
      errors.add(new ValidationError(
        "Circular dependency detected: " + currentAccount.getCode() + 
        " depends on accounts that depend on it"
      ));
    }
    
    // 3. Formula syntax is valid (basic check)
    try {
      validateFormulaExpression(formula);
    } catch (FormulaException e) {
      errors.add(new ValidationError("Invalid formula syntax: " + e.getMessage()));
    }
    
    return errors;
  }
  
  // Detect circular dependencies
  private boolean hasCyclicDependency(GLAccount account, List<String> references) {
    return hasCyclicDependencyHelper(
      account.getCode(),
      references,
      new HashSet<>()
    );
  }
  
  private boolean hasCyclicDependencyHelper(
      String currentCode,
      List<String> toCheck,
      Set<String> visited) {
    
    if (visited.contains(currentCode)) {
      return true; // Cycle detected
    }
    
    visited.add(currentCode);
    
    for (String ref : toCheck) {
      GLAccount refAccount = glAccountRepository.findByCode(ref);
      if (refAccount != null && refAccount.getCalculationType() == FORMULA) {
        CalculationConfig config = refAccount.getCalculationConfig();
        List<String> nestedRefs = config.getList("references");
        
        if (hasCyclicDependencyHelper(ref, nestedRefs, new HashSet<>(visited))) {
          return true;
        }
      }
    }
    
    return false;
  }
}
```

---

## Layer 2: Test Query Execution (Pre-Save)

**When**: User clicks "Test Query" button
**What**: Execute config against test data, return result or error

### UI Flow

```
User fills in GL Account calculation config:
┌─────────────────────────────────┐
│ Calculation Type: SUM_FIELD      │
│ Table: loans                     │
│ Field: disbursed_amount          │
│ Where: loan_type = 'NORMAL'      │
│                                  │
│ [Test Query] [Save] [Cancel]     │
└─────────────────────────────────┘

User clicks [Test Query]:

BACKEND:
1. Validate config syntax ✓
2. If invalid → show errors
3. If valid → execute query with LIMIT 1 and timeout
4. Return result or error

FRONTEND:
┌─ Test Query Result ──────────────┐
│ ✓ Query executed successfully    │
│                                  │
│ Sample result:                   │
│ Balance as of today: 99,629,963  │
│                                  │
│ Row count: 1                     │
│ Execution time: 145ms            │
│                                  │
│ [Close] [Save]                   │
└──────────────────────────────────┘
```

### Backend Implementation

```java
@RestController
@RequestMapping("/api/gl/accounts")
public class GLAccountController {
  
  @PostMapping("/{id}/test-config")
  public ResponseEntity<GLConfigTestResult> testCalculationConfig(
      @PathVariable Integer id,
      @RequestBody CalculationConfig config,
      @RequestParam(defaultValue = "30") Integer timeoutSeconds) {
    
    try {
      // 1. Validate config syntax
      ValidationResult validation = configValidator.validateCalculationConfig(
        glAccountRepository.findById(id),
        config
      );
      
      if (!validation.isValid()) {
        return ResponseEntity.badRequest().body(
          new GLConfigTestResult(
            false,
            "Configuration validation failed",
            validation.getErrors(),
            null
          )
        );
      }
      
      // 2. Execute with timeout
      BigDecimal result = executeWithTimeout(
        config,
        Duration.ofSeconds(timeoutSeconds)
      );
      
      // 3. Return success
      return ResponseEntity.ok(
        new GLConfigTestResult(
          true,
          "Query executed successfully",
          Collections.emptyList(),
          result
        )
      );
      
    } catch (ExecutionTimeoutException e) {
      return ResponseEntity.ok(
        new GLConfigTestResult(
          false,
          "Query timeout (" + timeoutSeconds + "s). May need LIMIT clause or index.",
          Collections.singletonList(
            new ValidationError(e.getMessage())
          ),
          null
        )
      );
    } catch (Exception e) {
      return ResponseEntity.ok(
        new GLConfigTestResult(
          false,
          "Query execution failed: " + e.getMessage(),
          Collections.singletonList(
            new ValidationError(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
          ),
          null
        )
      );
    }
  }
  
  private BigDecimal executeWithTimeout(CalculationConfig config, Duration timeout)
      throws Exception {
    
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<BigDecimal> future = executor.submit(() -> {
        return glCalculationService.calculateFromConfig(config);
      });
      
      return future.get(timeout.getSeconds(), TimeUnit.SECONDS);
      
    } finally {
      executor.shutdownNow();
    }
  }
}

public class GLConfigTestResult {
  private boolean success;
  private String message;
  private List<ValidationError> errors;
  private BigDecimal result;
  
  // Getters...
}
```

---

## Layer 3: Runtime Calculation Error Handling

**When**: During actual report generation
**What**: Catch errors, log them, mark GL account as broken

### Error Handling During Report Generation

```java
@Service
public class GLCalculationService {
  
  public BigDecimal calculateGLAccountBalance(Integer glAccountId, LocalDate asOfDate) {
    
    try {
      GLAccount account = glAccountRepository.findById(glAccountId);
      
      if (account.getCalculationType() == MANUAL_ENTRY) {
        return calculateManualEntry(account, asOfDate);
      }
      
      // Execute calculation with error tracking
      BigDecimal result = executeCalculation(account, asOfDate);
      
      // Mark as healthy
      account.setLastCalculationStatus("OK");
      account.setLastCalculationError(null);
      glAccountRepository.save(account);
      
      return result;
      
    } catch (Exception e) {
      // Log error but don't crash report
      GLAccount account = glAccountRepository.findById(glAccountId);
      account.setLastCalculationStatus("ERROR");
      account.setLastCalculationError(e.getMessage());
      account.setLastCalculationErrorTime(LocalDateTime.now());
      glAccountRepository.save(account);
      
      // Log to audit trail
      auditService.log(
        "GL_ACCOUNT_CALCULATION_ERROR",
        glAccountId,
        e.getMessage()
      );
      
      // Return zero, but flag for manual review
      return BigDecimal.ZERO;
    }
  }
}
```

---

## Layer 4: Audit Trail & Admin Dashboard

**What**: Track all GL config changes and execution errors

### Database Audit Table

```sql
CREATE TABLE gl_config_audit (
  id INT PRIMARY KEY AUTO_INCREMENT,
  gl_account_id INT NOT NULL,
  change_type ENUM('CREATE', 'UPDATE', 'DELETE', 'TEST', 'ERROR'),
  old_config JSON,
  new_config JSON,
  test_result VARCHAR(255),
  error_message TEXT,
  changed_by_user_id INT,
  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (gl_account_id) REFERENCES gl_accounts(id),
  FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);
```

### GL Account Health Dashboard (Admin View)

```
┌─ GL Account Health ──────────────────────────┐
│                                              │
│ Account           │ Status   │ Last Error   │
│ ─────────────────────────────────────────── │
│ LOAN_NORMAL       │ ✓ OK     │ -            │
│ MEMBER_DEPOSITS   │ ✓ OK     │ -            │
│ INT_LOANS         │ ⚠ ERROR  │ Table not    │
│                   │          │ found        │
│ AUDIT_FEES        │ ✓ OK     │ -            │
│ ─────────────────────────────────────────── │
│                                              │
│ [View Audit Log] [Fix Error] [Test All]     │
│                                              │
└──────────────────────────────────────────────┘
```

---

## Implementation Checklist

- [ ] **GLConfigValidator service** with all validation layers
- [ ] **POST /api/gl/accounts/{id}/test-config** endpoint
- [ ] **gl_config_audit table** for change tracking
- [ ] **Frontend: Test Query button** with result display
- [ ] **Frontend: GL Account Health dashboard** (admin only)
- [ ] **Error handling** in GLCalculationService (catch and log)
- [ ] **Timeout handling** for long-running queries (30s default)
- [ ] **Circular dependency detection** for formula-based accounts
- [ ] **SQL injection prevention** (parameterized queries only)
- [ ] **Unit tests** for each validation layer
- [ ] **Integration tests** for test-config endpoint

---

## Security Constraints

1. **Only SELECT allowed** - All other SQL keywords rejected
2. **Parameterized queries only** - No string concatenation
3. **Timeout enforcement** - Max 30 seconds per query
4. **Role-based access** - Only admin/treasurer can modify configs
5. **Audit trail** - Every config change logged
6. **Test before save** - Can't save without successful test
7. **Circuit breaker** - If GL account errors 5x, disable auto-calculation
8. **Field-level validation** - Know table structure before accepting config

---

## What Can Go Wrong (Handled)

| Issue | Prevention |
|-------|-----------|
| Bad WHERE clause | Syntax validation + test execution |
| Non-existent field | Field existence check + test execution |
| SQL injection | Parameterized queries only, keyword blocking |
| Circular dependency | Dependency graph analysis |
| Timeout | Execute with 30s timeout, user can increase |
| Silent failure | Calculate as ZERO but mark as ERROR, log audit |
| Invalid formula | Formula parser validates syntax |
| Missing GL reference | Reference existence check |
| Bad numeric cast | Field type validation |

---

## User Experience

1. **Admin opens GL Account editor**
2. **Enters calculation config** (or selects template)
3. **Clicks "Test Query"**
   - If valid → Shows result ("Balance: 99,629,963") → Can save
   - If invalid → Shows specific errors → Must fix before saving
4. **Clicks "Save"** → Audited, config stored
5. **Report generation** → Uses cached config, handles errors gracefully
6. **Errors appear in GL Health Dashboard** → Admin can investigate & fix

---

## Templates (Pre-Built Configs)

Provide templates for common cases (no manual entry needed):

```
Templates Available:
├─ Sum Field from Table
│  ├─ Sum with date range
│  ├─ Sum with status filter
│  └─ Sum with member subset
├─ Custom Query
│  ├─ Bank account balances
│  └─ Interest calculations
├─ GL Reference
│  ├─ Single GL account
│  └─ Multiple GL accounts
└─ Manual Entry
   ├─ Accrual entry
   └─ One-time adjustment
```

User selects template → fills blanks → test → save.

