package com.minet.sacco.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.minet.sacco.dto.*;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLManualEntry;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.GLAccountRepository;
import com.minet.sacco.repository.GLManualEntryRepository;
import com.minet.sacco.service.GLCalculationService;
import com.minet.sacco.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/gl/period-entry")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GLPeriodEntryController {
  private static final Logger logger = LoggerFactory.getLogger(GLPeriodEntryController.class);
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  @Autowired
  private GLManualEntryRepository glManualEntryRepository;
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private UserService userService;
  
  /**
   * Get all GL accounts for a period with their balances/entered amounts
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<List<GLPeriodEntryLineDTO>>> getPeriodEntries(
      @RequestParam Integer periodMonth,
      @RequestParam Integer periodYear) {
    try {
      List<GLAccount> accounts = glAccountRepository.findByIsActiveTrueOrderByDisplayOrder();
      List<GLPeriodEntryLineDTO> lines = new ArrayList<>();
      
      LocalDate lastDayOfPeriod = getLastDayOfPeriod(periodMonth, periodYear);
      
      for (GLAccount account : accounts) {
        GLPeriodEntryLineDTO line = new GLPeriodEntryLineDTO();
        line.setGlAccountId(account.getId());
        line.setCode(account.getCode());
        line.setName(account.getName());
        line.setAccountType(account.getAccountType().toString());
        if (account.getNormalBalance() != null) {
          line.setNormalBalance(account.getNormalBalance().toString());
        }
        line.setSectionLabel(account.getSectionLabel());
        
        // Handle AUTO accounts (AGGREGATION)
        if (account.getBalanceCalculationType() == GLAccount.CalculationType.AGGREGATION) {
          line.setSourceType("AUTO");
          BigDecimal balance = glCalculationService.calculateGLAccountBalance(
            account.getId(), lastDayOfPeriod, periodMonth, periodYear);
          line.setAmount(balance);
          line.setReadOnly(true);
        }
        // Handle MANUAL accounts (MANUAL_ENTRY)
        else if (account.getBalanceCalculationType() == GLAccount.CalculationType.MANUAL_ENTRY) {
          line.setSourceType("MANUAL");
          line.setReadOnly(false);
          
          // Find existing entry for this period
          Optional<GLManualEntry> existingEntry = findEntryForPeriod(
            account.getId(), periodMonth, periodYear);
          
          if (existingEntry.isPresent()) {
            GLManualEntry entry = existingEntry.get();
            line.setAmount(entry.getAmount());
            line.setPeriodStatus(entry.getPeriodStatus().toString());
            line.setEntryId(entry.getId());
          } else {
            line.setAmount(BigDecimal.ZERO);
            line.setPeriodStatus("DRAFT");
            line.setEntryId(null);
          }
        }
        
        lines.add(line);
      }
      
      logger.info("Retrieved GL period entries for " + periodMonth + "/" + periodYear);
      return ResponseEntity.ok(ApiResponse.success("Period entries retrieved", lines));
    } catch (Exception e) {
      logger.error("Error retrieving period entries", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to retrieve period entries: " + e.getMessage()));
    }
  }
  
  /**
   * Create or update a manual entry for a period
   */
  @PostMapping
  @PreAuthorize("hasAnyRole('TREASURER')")
  public ResponseEntity<ApiResponse<GLManualEntry>> createOrUpdateEntry(
      @RequestBody GLPeriodEntryRequestDTO request,
      Authentication authentication) {
    try {
      GLAccount account = glAccountRepository.findById(request.getGlAccountId())
        .orElseThrow(() -> new RuntimeException("GL Account not found"));
      
      // Only MANUAL_ENTRY accounts can have period entries
      if (account.getBalanceCalculationType() != GLAccount.CalculationType.MANUAL_ENTRY) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Only MANUAL_ENTRY accounts can have period entries"));
      }
      
      // Get current user
      Optional<User> currentUserOpt = userService.getUserByUsername(authentication.getName());
      if (!currentUserOpt.isPresent()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("User not found"));
      }
      User currentUser = currentUserOpt.get();
      
      // Calculate entry date as last day of period
      LocalDate entryDate = getLastDayOfPeriod(request.getPeriodMonth(), request.getPeriodYear());
      
      // Check if entry exists for this period
      Optional<GLManualEntry> existingEntry = findEntryForPeriod(
        request.getGlAccountId(), 
        request.getPeriodMonth(), 
        request.getPeriodYear()
      );
      
      GLManualEntry entry;
      if (existingEntry.isPresent()) {
        // Update existing entry (only if still DRAFT)
        entry = existingEntry.get();
        if (entry.getPeriodStatus() != GLManualEntry.PeriodStatus.DRAFT) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Cannot update entry that is not in DRAFT status"));
        }
        entry.setAmount(request.getAmount());
        entry.setDescription(request.getDescription());
        entry.setEntryReason(GLManualEntry.EntryReason.valueOf(request.getEntryReason()));
      } else {
        // Create new entry
        entry = new GLManualEntry();
        entry.setGlAccount(account);
        entry.setAmount(request.getAmount());
        entry.setIsDebit(true); // Default to debit; can be adjusted as needed
        entry.setDescription(request.getDescription());
        entry.setEntryReason(GLManualEntry.EntryReason.valueOf(request.getEntryReason()));
        entry.setCreatedByUser(currentUser);
        entry.setEntryDate(entryDate);
        entry.setApprovalStatus(GLManualEntry.ApprovalStatus.PENDING);
        entry.setPeriodStatus(GLManualEntry.PeriodStatus.DRAFT);
        entry.setPeriodMonth(request.getPeriodMonth());
        entry.setPeriodYear(request.getPeriodYear());
      }
      
      GLManualEntry saved = glManualEntryRepository.save(entry);
      logger.info("Created/updated GL manual entry for account " + account.getCode() + 
                  " period " + request.getPeriodMonth() + "/" + request.getPeriodYear());
      return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Entry created/updated", saved));
    } catch (IllegalArgumentException e) {
      logger.error("Invalid enum value", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Invalid value: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Error creating/updating period entry", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to create/update entry: " + e.getMessage()));
    }
  }
  
  /**
   * Treasurer submits entry for approval (DRAFT → POSTED)
   */
  @PutMapping("/{id}/submit")
  @PreAuthorize("hasAnyRole('TREASURER')")
  public ResponseEntity<ApiResponse<GLManualEntry>> submitEntry(
      @PathVariable Integer id,
      Authentication authentication) {
    try {
      GLManualEntry entry = glManualEntryRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Entry not found"));
      
      // Check status is DRAFT
      if (entry.getPeriodStatus() != GLManualEntry.PeriodStatus.DRAFT) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Entry must be in DRAFT status to submit"));
      }
      
      // Verify treasurer ownership
      Optional<User> currentUserOpt = userService.getUserByUsername(authentication.getName());
      if (!currentUserOpt.isPresent()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("User not found"));
      }
      User currentUser = currentUserOpt.get();
      
      if (!entry.getCreatedByUser().getId().equals(currentUser.getId())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("Only the entry creator can submit for approval"));
      }
      
      entry.setPeriodStatus(GLManualEntry.PeriodStatus.POSTED);
      GLManualEntry updated = glManualEntryRepository.save(entry);
      logger.info("Submitted GL entry " + id + " for approval");
      return ResponseEntity.ok(ApiResponse.success("Entry submitted for approval", updated));
    } catch (Exception e) {
      logger.error("Error submitting entry", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to submit entry: " + e.getMessage()));
    }
  }
  
  /**
   * Admin approves entry (POSTED → APPROVED)
   */
  @PutMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLManualEntry>> approveEntry(@PathVariable Integer id) {
    try {
      GLManualEntry entry = glManualEntryRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Entry not found"));
      
      // Check status is POSTED
      if (entry.getPeriodStatus() != GLManualEntry.PeriodStatus.POSTED) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Entry must be in POSTED status to approve"));
      }
      
      entry.setApprovalStatus(GLManualEntry.ApprovalStatus.APPROVED);
      entry.setPeriodStatus(GLManualEntry.PeriodStatus.APPROVED);
      GLManualEntry updated = glManualEntryRepository.save(entry);
      logger.info("Approved GL entry " + id);
      return ResponseEntity.ok(ApiResponse.success("Entry approved", updated));
    } catch (Exception e) {
      logger.error("Error approving entry", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to approve entry: " + e.getMessage()));
    }
  }
  
  /**
   * Admin rejects entry (back to DRAFT)
   */
  @PutMapping("/{id}/reject")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLManualEntry>> rejectEntry(
      @PathVariable Integer id,
      @RequestBody GLPeriodEntryRejectDTO request) {
    try {
      GLManualEntry entry = glManualEntryRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Entry not found"));
      
      // Check status is POSTED
      if (entry.getPeriodStatus() != GLManualEntry.PeriodStatus.POSTED) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Entry must be in POSTED status to reject"));
      }
      
      entry.setApprovalStatus(GLManualEntry.ApprovalStatus.REJECTED);
      entry.setPeriodStatus(GLManualEntry.PeriodStatus.DRAFT);
      // Store rejection reason in description or create a new field if needed
      if (request.getRejectReason() != null) {
        entry.setDescription("REJECTED: " + request.getRejectReason() + "\nOriginal: " + entry.getDescription());
      }
      GLManualEntry updated = glManualEntryRepository.save(entry);
      logger.info("Rejected GL entry " + id);
      return ResponseEntity.ok(ApiResponse.success("Entry rejected", updated));
    } catch (Exception e) {
      logger.error("Error rejecting entry", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to reject entry: " + e.getMessage()));
    }
  }
  
  /**
   * Admin locks entry (no further edits allowed)
   */
  @PutMapping("/{id}/lock")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<GLManualEntry>> lockEntry(@PathVariable Integer id) {
    try {
      GLManualEntry entry = glManualEntryRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Entry not found"));
      
      // Check status is APPROVED
      if (entry.getPeriodStatus() != GLManualEntry.PeriodStatus.APPROVED) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Entry must be in APPROVED status to lock"));
      }
      
      entry.setPeriodStatus(GLManualEntry.PeriodStatus.LOCKED);
      GLManualEntry updated = glManualEntryRepository.save(entry);
      logger.info("Locked GL entry " + id);
      return ResponseEntity.ok(ApiResponse.success("Entry locked", updated));
    } catch (Exception e) {
      logger.error("Error locking entry", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to lock entry: " + e.getMessage()));
    }
  }
  
  /**
   * Get distinct periods that have entries
   */
  @GetMapping("/periods")
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<List<GLPeriodDTO>>> getAvailablePeriods() {
    try {
      List<GLManualEntry> allEntries = glManualEntryRepository.findAll();
      
      List<GLPeriodDTO> periods = allEntries.stream()
        .filter(e -> e.getPeriodMonth() != null && e.getPeriodYear() != null)
        .map(e -> new GLPeriodDTO(e.getPeriodMonth(), e.getPeriodYear()))
        .distinct()
        .sorted((p1, p2) -> {
          // Sort by most recent first
          if (!p2.getPeriodYear().equals(p1.getPeriodYear())) {
            return p2.getPeriodYear().compareTo(p1.getPeriodYear());
          }
          return p2.getPeriodMonth().compareTo(p1.getPeriodMonth());
        })
        .collect(Collectors.toList());
      
      logger.info("Retrieved " + periods.size() + " available periods");
      return ResponseEntity.ok(ApiResponse.success("Periods retrieved", periods));
    } catch (Exception e) {
      logger.error("Error retrieving periods", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to retrieve periods: " + e.getMessage()));
    }
  }
  
  /**
   * Helper: Find entry for a specific account and period
   */
  private Optional<GLManualEntry> findEntryForPeriod(Integer glAccountId, Integer periodMonth, Integer periodYear) {
    return glManualEntryRepository.findByGlAccountIdOrderByCreatedAtDesc(glAccountId)
      .stream()
      .filter(e -> periodMonth.equals(e.getPeriodMonth()) && periodYear.equals(e.getPeriodYear()))
      .findFirst();
  }
  
  /**
   * Helper: Get last day of the given month/year
   */
  private LocalDate getLastDayOfPeriod(Integer month, Integer year) {
    YearMonth yearMonth = YearMonth.of(year, month);
    return yearMonth.atEndOfMonth();
  }
}
