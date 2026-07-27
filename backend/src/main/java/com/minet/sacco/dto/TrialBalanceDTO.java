package com.minet.sacco.dto;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public class TrialBalanceDTO {
  private LocalDate asOfDate;
  private List<TrialBalanceLineDTO> lines;
  private TrialBalanceSummaryDTO summary;
  
  public TrialBalanceDTO() {}
  
  public TrialBalanceDTO(LocalDate asOfDate, List<TrialBalanceLineDTO> lines) {
    this.asOfDate = asOfDate;
    this.lines = lines;
    this.summary = calculateSummary(lines);
  }
  
  private TrialBalanceSummaryDTO calculateSummary(List<TrialBalanceLineDTO> lines) {
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    
    for (TrialBalanceLineDTO line : lines) {
      if (line.getIsDebit()) {
        totalDebit = totalDebit.add(line.getBalance());
      } else {
        totalCredit = totalCredit.add(line.getBalance());
      }
    }
    
    return new TrialBalanceSummaryDTO(
      totalDebit,
      totalCredit,
      totalDebit.compareTo(totalCredit) == 0
    );
  }
  
  public LocalDate getAsOfDate() { return asOfDate; }
  public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
  
  public List<TrialBalanceLineDTO> getLines() { return lines; }
  public void setLines(List<TrialBalanceLineDTO> lines) { this.lines = lines; }
  
  public TrialBalanceSummaryDTO getSummary() { return summary; }
  public void setSummary(TrialBalanceSummaryDTO summary) { this.summary = summary; }
}
