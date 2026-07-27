package com.minet.sacco.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "gl_account_audit")
public class GLAccountAudit implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gl_account_id", nullable = false)
  private GLAccount glAccount;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by_user_id", nullable = false)
  private User changedByUser;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ChangeType changeType;
  
  @Column(columnDefinition = "JSON")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode oldConfig;
  
  @Column(columnDefinition = "JSON")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode newConfig;
  
  @Column(length = 500)
  private String changeReason;
  
  @Column(nullable = false, updatable = false)
  private LocalDateTime changedAt = LocalDateTime.now();
  
  public enum ChangeType {
    CREATE, UPDATE, DELETE, ACTIVATE, DEACTIVATE
  }
  
  // Constructors
  public GLAccountAudit() {}
  
  public GLAccountAudit(GLAccount glAccount, User changedByUser, ChangeType changeType, 
                       JsonNode oldConfig, JsonNode newConfig, String changeReason) {
    this.glAccount = glAccount;
    this.changedByUser = changedByUser;
    this.changeType = changeType;
    this.oldConfig = oldConfig;
    this.newConfig = newConfig;
    this.changeReason = changeReason;
  }
  
  // Getters and Setters
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  
  public GLAccount getGlAccount() { return glAccount; }
  public void setGlAccount(GLAccount glAccount) { this.glAccount = glAccount; }
  
  public User getChangedByUser() { return changedByUser; }
  public void setChangedByUser(User changedByUser) { this.changedByUser = changedByUser; }
  
  public ChangeType getChangeType() { return changeType; }
  public void setChangeType(ChangeType changeType) { this.changeType = changeType; }
  
  public JsonNode getOldConfig() { return oldConfig; }
  public void setOldConfig(JsonNode oldConfig) { this.oldConfig = oldConfig; }
  
  public JsonNode getNewConfig() { return newConfig; }
  public void setNewConfig(JsonNode newConfig) { this.newConfig = newConfig; }
  
  public String getChangeReason() { return changeReason; }
  public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
  
  public LocalDateTime getChangedAt() { return changedAt; }
  public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
