package com.minet.sacco.dto;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public class IncomeStatementDTO {
  private LocalDate fromDate;
  private LocalDate toDate;
  
  // Revenues
  private List<IncomeStatementLineDTO> revenues;
  private BigDecimal totalRevenues;
  
  // Expenses
  private List<IncomeStatementLineDTO> expenses;
  private BigDecimal totalExpenses;
  
  // Net Income
  private BigDecimal netIncome;
  private BigDecimal profitMarginPercent;
  
  public IncomeStatementDTO() {}
  
  public IncomeStatementDTO(LocalDate fromDate, LocalDate toDate,
                            List<IncomeStatementLineDTO> revenues, BigDecimal totalRevenues,
                            List<IncomeStatementLineDTO> expenses, BigDecimal totalExpenses,
                            BigDecimal netIncome) {
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.revenues = revenues;
    this.totalRevenues = totalRevenues;
    this.expenses = expenses;
    this.totalExpenses = totalExpenses;
    this.netIncome = netIncome;
    
    // Calculate profit margin
    if (totalRevenues != null && totalRevenues.compareTo(BigDecimal.ZERO) > 0) {
      this.profitMarginPercent = netIncome.divide(totalRevenues, 4, java.math.RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
    } else {
      this.profitMarginPercent = BigDecimal.ZERO;
    }
  }
  
  public LocalDate getFromDate() { return fromDate; }
  public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
  
  public LocalDate getToDate() { return toDate; }
  public void setToDate(LocalDate toDate) { this.toDate = toDate; }
  
  public List<IncomeStatementLineDTO> getRevenues() { return revenues; }
  public void setRevenues(List<IncomeStatementLineDTO> revenues) { this.revenues = revenues; }
  
  public BigDecimal getTotalRevenues() { return totalRevenues; }
  public void setTotalRevenues(BigDecimal totalRevenues) { this.totalRevenues = totalRevenues; }
  
  public List<IncomeStatementLineDTO> getExpenses() { return expenses; }
  public void setExpenses(List<IncomeStatementLineDTO> expenses) { this.expenses = expenses; }
  
  public BigDecimal getTotalExpenses() { return totalExpenses; }
  public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }
  
  public BigDecimal getNetIncome() { return netIncome; }
  public void setNetIncome(BigDecimal netIncome) { this.netIncome = netIncome; }
  
  public BigDecimal getProfitMarginPercent() { return profitMarginPercent; }
  public void setProfitMarginPercent(BigDecimal profitMarginPercent) { this.profitMarginPercent = profitMarginPercent; }
}
