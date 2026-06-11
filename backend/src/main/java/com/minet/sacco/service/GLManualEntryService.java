package com.minet.sacco.service;

import com.minet.sacco.dto.GLManualEntryDTO;
import com.minet.sacco.dto.GLManualEntryRequest;
import com.minet.sacco.entity.GLManualEntry;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.GLManualEntryRepository;
import com.minet.sacco.repository.GLAccountRepository;
import com.minet.sacco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GLManualEntryService {

  @Autowired
  private GLManualEntryRepository manualEntryRepository;

  @Autowired
  private GLAccountRepository glAccountRepository;

  @Autowired
  private UserRepository userRepository;

  /**
   * Create a new GL manual entry (PENDING status)
   */
  public GLManualEntryDTO createManualEntry(GLManualEntryRequest request, Integer userId) {
    GLAccount glAccount = glAccountRepository.findById(request.getGlAccountId())
      .orElseThrow(() -> new RuntimeException("GL Account not found: " + request.getGlAccountId()));

    User user = userRepository.findById(userId.longValue())
      .orElseThrow(() -> new RuntimeException("User not found: " + userId));

    GLManualEntry.EntryReason reason;
    try {
      reason = GLManualEntry.EntryReason.valueOf(request.getEntryReason().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid entry reason: " + request.getEntryReason());
    }

    GLManualEntry entry = new GLManualEntry(
      glAccount,
      request.getEntryDate(),
      request.getDescription(),
      request.getAmount(),
      request.getIsDebit(),
      reason,
      user
    );

    entry = manualEntryRepository.save(entry);
    return mapToDTO(entry);
  }

  /**
   * Get all pending entries (both old manual entries and new period entries awaiting approval)
   */
  public List<GLManualEntryDTO> getPendingEntries() {
    // Get old-style manual entries only: approvalStatus = PENDING and not tied to a specific period
    List<GLManualEntry> oldStylePending = manualEntryRepository
      .findByApprovalStatusOrderByCreatedAtDesc(GLManualEntry.ApprovalStatus.PENDING)
      .stream()
      .filter(entry -> entry.getPeriodMonth() == null && entry.getPeriodYear() == null)
      .collect(Collectors.toList());
    
    // Get new-style period entries: periodStatus = POSTED
    List<GLManualEntry> periodEntries = manualEntryRepository.findByPeriodStatusOrderByCreatedAtDesc(GLManualEntry.PeriodStatus.POSTED);
    
    // Combine and sort by created date (most recent first)
    List<GLManualEntry> allPending = new java.util.ArrayList<>();
    allPending.addAll(oldStylePending);
    allPending.addAll(periodEntries);
    
    return allPending.stream()
      .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
      .map(this::mapToDTO)
      .collect(Collectors.toList());
  }

  /**
   * Get all entries (any status)
   */
  public List<GLManualEntryDTO> getAllEntries() {
    return manualEntryRepository.findAll()
      .stream()
      .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
      .map(this::mapToDTO)
      .collect(Collectors.toList());
  }

  /**
   * Get entries by account
   */
  public List<GLManualEntryDTO> getEntriesByAccount(Integer glAccountId) {
    return manualEntryRepository.findByGlAccountIdOrderByCreatedAtDesc(glAccountId)
      .stream()
      .map(this::mapToDTO)
      .collect(Collectors.toList());
  }

  /**
   * Approve a pending entry
   */
  public GLManualEntryDTO approveEntry(Integer entryId, Integer approverId) {
    GLManualEntry entry = manualEntryRepository.findById(entryId)
      .orElseThrow(() -> new RuntimeException("Manual entry not found: " + entryId));

    if (!entry.getApprovalStatus().equals(GLManualEntry.ApprovalStatus.PENDING)) {
      throw new RuntimeException("Entry is not in PENDING status");
    }

    User approver = userRepository.findById(approverId.longValue())
      .orElseThrow(() -> new RuntimeException("User not found: " + approverId));

    entry.setApprovalStatus(GLManualEntry.ApprovalStatus.APPROVED);
    entry.setApprovedByUser(approver);
    entry.setApprovedAt(java.time.LocalDateTime.now());

    entry = manualEntryRepository.save(entry);
    return mapToDTO(entry);
  }

  /**
   * Reject a pending entry
   */
  public GLManualEntryDTO rejectEntry(Integer entryId, Integer approverId) {
    GLManualEntry entry = manualEntryRepository.findById(entryId)
      .orElseThrow(() -> new RuntimeException("Manual entry not found: " + entryId));

    if (!entry.getApprovalStatus().equals(GLManualEntry.ApprovalStatus.PENDING)) {
      throw new RuntimeException("Entry is not in PENDING status");
    }

    User approver = userRepository.findById(approverId.longValue())
      .orElseThrow(() -> new RuntimeException("User not found: " + approverId));

    entry.setApprovalStatus(GLManualEntry.ApprovalStatus.REJECTED);
    entry.setApprovedByUser(approver);
    entry.setApprovedAt(java.time.LocalDateTime.now());

    entry = manualEntryRepository.save(entry);
    return mapToDTO(entry);
  }

  /**
   * Delete a pending entry (only pending entries can be deleted)
   */
  public void deleteEntry(Integer entryId) {
    GLManualEntry entry = manualEntryRepository.findById(entryId)
      .orElseThrow(() -> new RuntimeException("Manual entry not found: " + entryId));

    if (!entry.getApprovalStatus().equals(GLManualEntry.ApprovalStatus.PENDING)) {
      throw new RuntimeException("Only PENDING entries can be deleted");
    }

    manualEntryRepository.delete(entry);
  }

  /**
   * Map entity to DTO
   */
  private GLManualEntryDTO mapToDTO(GLManualEntry entry) {
    String periodStatus = entry.getPeriodStatus() != null ? entry.getPeriodStatus().toString() : null;
    boolean isPeriodEntry = entry.getPeriodMonth() != null && entry.getPeriodYear() != null;
    String workflowStatus = isPeriodEntry && periodStatus != null
      ? periodStatus
      : entry.getApprovalStatus().toString();

    return new GLManualEntryDTO(
      entry.getId(),
      entry.getGlAccount().getId(),
      entry.getGlAccount().getCode(),
      entry.getGlAccount().getName(),
      entry.getEntryDate(),
      entry.getDescription(),
      entry.getAmount(),
      entry.getIsDebit(),
      entry.getEntryReason().toString(),
      entry.getApprovalStatus().toString(),
      entry.getCreatedByUser().getUsername(),
      entry.getApprovedByUser() != null ? entry.getApprovedByUser().getUsername() : null,
      entry.getCreatedAt(),
      entry.getApprovedAt(),
      periodStatus,
      workflowStatus,
      isPeriodEntry ? "PERIOD_ENTRY" : "MANUAL_ENTRY",
      entry.getPeriodMonth(),
      entry.getPeriodYear()
    );
  }
}
