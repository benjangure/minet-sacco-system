package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
  
  @Column(name = "calculation_config", columnDefinition = "LONGTEXT")
  private String calculationConfigJson;
  
  @Transient
  private JsonNode calculationConfig;
  
  @Column(nullable = false)
  private Boolean isActive = true;
  
  @Column(nullable = false)
  private Integer displayOrder = 100;
  
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
  
  @Column(nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = true)
  private NormalBalance normalBalance;
  
  @Column(length = 100, nullable = true)
  private String sectionLabel;
  
  @Column(nullable = false)
  private Boolean periodSensitive = false;
  
  public enum AccountType {
    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
  }
  
  public enum CalculationType {
    AGGREGATION, FORMULA, MANUAL_ENTRY, COMPUTED
  }
  
  public enum NormalBalance {
    DEBIT, CREDIT
  }
  
  @PostLoad
  public void deserializeConfig() {
    if (calculationConfigJson != null) {
      try {
        this.calculationConfig = new ObjectMapper().readTree(calculationConfigJson);
      } catch (Exception e) {
        this.calculationConfig = null;
      }
    }
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
  public void setCalculationConfig(JsonNode node) {
    this.calculationConfig = node;
    try {
      this.calculationConfigJson = node != null ? new ObjectMapper().writeValueAsString(node) : null;
    } catch (Exception e) {
      this.calculationConfigJson = null;
    }
  }
  
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
  
  public Integer getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
  
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  
  public NormalBalance getNormalBalance() { return normalBalance; }
  public void setNormalBalance(NormalBalance normalBalance) { this.normalBalance = normalBalance; }
  
  public String getSectionLabel() { return sectionLabel; }
  public void setSectionLabel(String sectionLabel) { this.sectionLabel = sectionLabel; }
  
  public Boolean getPeriodSensitive() { return periodSensitive; }
  public void setPeriodSensitive(Boolean periodSensitive) { this.periodSensitive = periodSensitive; }
}
