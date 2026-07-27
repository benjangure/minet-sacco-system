package com.minet.sacco.dto;

public class GLAccountUpdateRequestDTO {
  private String name;
  private String sectionLabel;
  private Boolean periodSensitive;
  private Integer displayOrder;
  private Boolean isActive;
  
  public GLAccountUpdateRequestDTO() {}
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public String getSectionLabel() { return sectionLabel; }
  public void setSectionLabel(String sectionLabel) { this.sectionLabel = sectionLabel; }
  
  public Boolean getPeriodSensitive() { return periodSensitive; }
  public void setPeriodSensitive(Boolean periodSensitive) { this.periodSensitive = periodSensitive; }
  
  public Integer getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
  
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
