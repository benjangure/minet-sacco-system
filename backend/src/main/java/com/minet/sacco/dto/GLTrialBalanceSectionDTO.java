package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.util.List;

public class GLTrialBalanceSectionDTO {
  private String sectionName;
  private List<GLTrialBalanceLineDTO> lines;
  private BigDecimal sectionTotalDebit;
  private BigDecimal sectionTotalCredit;
  
  public GLTrialBalanceSectionDTO() {}
  
  public GLTrialBalanceSectionDTO(String sectionName, List<GLTrialBalanceLineDTO> lines,
                                   BigDecimal sectionTotalDebit, BigDecimal sectionTotalCredit) {
    this.sectionName = sectionName;
    this.lines = lines;
    this.sectionTotalDebit = sectionTotalDebit;
    this.sectionTotalCredit = sectionTotalCredit;
  }
  
  public String getSectionName() { return sectionName; }
  public void setSectionName(String sectionName) { this.sectionName = sectionName; }
  
  public List<GLTrialBalanceLineDTO> getLines() { return lines; }
  public void setLines(List<GLTrialBalanceLineDTO> lines) { this.lines = lines; }
  
  public BigDecimal getSectionTotalDebit() { return sectionTotalDebit; }
  public void setSectionTotalDebit(BigDecimal sectionTotalDebit) { this.sectionTotalDebit = sectionTotalDebit; }
  
  public BigDecimal getSectionTotalCredit() { return sectionTotalCredit; }
  public void setSectionTotalCredit(BigDecimal sectionTotalCredit) { this.sectionTotalCredit = sectionTotalCredit; }
}
