package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BalanceDTO {
  private BigDecimal balance;
  private LocalDate asOfDate;

  public BalanceDTO() {}

  public BalanceDTO(BigDecimal balance, LocalDate asOfDate) {
    this.balance = balance;
    this.asOfDate = asOfDate;
  }

  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }

  public LocalDate getAsOfDate() { return asOfDate; }
  public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
}
