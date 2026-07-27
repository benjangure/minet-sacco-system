package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GLTrialBalanceReportDTO {
  private String reportTitle;
  private String saccoName;
  private LocalDate asOfDate;
  private Integer periodMonth;
  private Integer periodYear;
  private List<GLTrialBalanceSectionDTO> sections;
  private BigDecimal grandTotalDebit;
  private BigDecimal grandTotalCredit;
  private Boolean isBalanced;
  private BigDecimal difference;
  
  public GLTrialBalanceReportDTO() {}
  
  public String getReportTitle() { return reportTitle; }
  public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }
  
  public String getSaccoName() { return saccoName; }
  public void setSaccoName(String saccoName) { this.saccoName = saccoName; }
  
  public LocalDate getAsOfDate() { return asOfDate; }
  public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
  
  public Integer getPeriodMonth() { return periodMonth; }
  public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
  
  public Integer getPeriodYear() { return periodYear; }
  public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
  
  public List<GLTrialBalanceSectionDTO> getSections() { return sections; }
  public void setSections(List<GLTrialBalanceSectionDTO> sections) { this.sections = sections; }
  
  public BigDecimal getGrandTotalDebit() { return grandTotalDebit; }
  public void setGrandTotalDebit(BigDecimal grandTotalDebit) { this.grandTotalDebit = grandTotalDebit; }
  
  public BigDecimal getGrandTotalCredit() { return grandTotalCredit; }
  public void setGrandTotalCredit(BigDecimal grandTotalCredit) { this.grandTotalCredit = grandTotalCredit; }
  
  public Boolean getIsBalanced() { return isBalanced; }
  public void setIsBalanced(Boolean isBalanced) { this.isBalanced = isBalanced; }
  
  public BigDecimal getDifference() { return difference; }
  public void setDifference(BigDecimal difference) { this.difference = difference; }
}
