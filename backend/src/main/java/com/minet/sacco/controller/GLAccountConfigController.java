package com.minet.sacco.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.GLAccountCreateRequestDTO;
import com.minet.sacco.dto.GLAccountUpdateRequestDTO;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.NormalBalance;
import com.minet.sacco.entity.GLAccount.AccountType;
import com.minet.sacco.entity.GLAccount.CalculationType;
import com.minet.sacco.repository.GLAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/gl/account-configuration")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GLAccountConfigController {
  private static final Logger logger = LoggerFactory.getLogger(GLAccountConfigController.class);
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  private final ObjectMapper objectMapper = new ObjectMapper();
  
  /**
   * Get all GL accounts ordered by displayOrder
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<List<GLAccount>>> getAllAccounts() {
    try {
      List<GLAccount> accounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
      logger.info("Retrieved " + accounts.size() + " active GL accounts");
      return ResponseEntity.ok(ApiResponse.success("GL Accounts retrieved", accounts));
    } catch (Exception e) {
      logger.error("Error retrieving GL accounts", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to retrieve GL accounts: " + e.getMessage()));
    }
  }
  
  /**
   * Get single GL account by ID
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<GLAccount>> getAccountById(@PathVariable Integer id) {
    try {
      Optional<GLAccount> account = glAccountRepository.findById(id);
      if (account.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("GL Account not found: " + id));
      }
      logger.info("Retrieved GL account: " + account.get().getCode());
      return ResponseEntity.ok(ApiResponse.success("GL Account retrieved", account.get()));
    } catch (Exception e) {
      logger.error("Error retrieving GL account " + id, e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to retrieve GL account: " + e.getMessage()));
    }
  }
  
  /**
   * Create new GL account
   */
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<GLAccount>> createAccount(
      @RequestBody GLAccountCreateRequestDTO request) {
    try {
      // Validate code uniqueness
      if (glAccountRepository.findByCode(request.getCode()).isPresent()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("GL Account code already exists: " + request.getCode()));
      }
      
      // Create new account
      GLAccount account = new GLAccount();
      account.setCode(request.getCode());
      account.setName(request.getName());
      account.setAccountType(AccountType.valueOf(request.getAccountType()));
      account.setBalanceCalculationType(CalculationType.valueOf(request.getBalanceCalculationType()));
      
      if (request.getNormalBalance() != null) {
        account.setNormalBalance(NormalBalance.valueOf(request.getNormalBalance()));
      }
      
      account.setSectionLabel(request.getSectionLabel());
      account.setPeriodSensitive(request.getPeriodSensitive() != null ? 
                                 request.getPeriodSensitive() : false);
      account.setDisplayOrder(request.getDisplayOrder() != null ? 
                              request.getDisplayOrder() : 100);
      account.setIsActive(true);
      
      // Build calculationConfig based on dataSource and balanceCalculationType
      JsonNode calculationConfig = buildCalculationConfig(
        request.getBalanceCalculationType(),
        request.getDataSource(),
        request.getLoanProductId()
      );
      account.setCalculationConfig(calculationConfig);
      
      GLAccount saved = glAccountRepository.save(account);
      logger.info("Created GL account: " + saved.getCode());
      return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("GL Account created successfully", saved));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid enum value in request", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Invalid value: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Error creating GL account", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to create GL account: " + e.getMessage()));
    }
  }
  
  /**
   * Update GL account (only allowed fields)
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<GLAccount>> updateAccount(
      @PathVariable Integer id,
      @RequestBody GLAccountUpdateRequestDTO request) {
    try {
      Optional<GLAccount> existing = glAccountRepository.findById(id);
      if (existing.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("GL Account not found: " + id));
      }
      
      GLAccount account = existing.get();
      
      // Update allowed fields only
      if (request.getName() != null) {
        account.setName(request.getName());
      }
      if (request.getSectionLabel() != null) {
        account.setSectionLabel(request.getSectionLabel());
      }
      if (request.getPeriodSensitive() != null) {
        account.setPeriodSensitive(request.getPeriodSensitive());
      }
      if (request.getDisplayOrder() != null) {
        account.setDisplayOrder(request.getDisplayOrder());
      }
      if (request.getIsActive() != null) {
        account.setIsActive(request.getIsActive());
      }
      
      GLAccount updated = glAccountRepository.save(account);
      logger.info("Updated GL account: " + updated.getCode());
      return ResponseEntity.ok(ApiResponse.success("GL Account updated successfully", updated));
    } catch (Exception e) {
      logger.error("Error updating GL account " + id, e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to update GL account: " + e.getMessage()));
    }
  }
  
  /**
   * Build calculationConfig JSON based on dataSource and balanceCalculationType
   */
  private JsonNode buildCalculationConfig(String balanceCalculationType, String dataSource, Integer loanProductId) {
    ObjectNode config = objectMapper.createObjectNode();
    
    if ("MANUAL_ENTRY".equals(balanceCalculationType)) {
      config.put("type", "manual");
      return config;
    }
    
    if ("AGGREGATION".equals(balanceCalculationType)) {
      switch (dataSource) {
        case "LOANS":
          config.put("table", "loans");
          config.put("field", "outstanding_balance");
          config.put("status", "DISBURSED");
          if (loanProductId != null) {
            config.put("loanProductId", loanProductId);
          }
          break;
        
        case "SAVINGS":
          config.put("table", "accounts");
          config.put("field", "balance");
          config.put("accountType", "SAVINGS");
          break;
        
        case "SHARES":
          config.put("table", "accounts");
          config.put("field", "balance");
          config.put("accountType", "SHARES");
          break;
        
        case "TRANSACTIONS":
          config.put("table", "transactions");
          config.put("field", "amount");
          break;
      }
    }
    
    return config;
  }
}
