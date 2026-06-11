package com.minet.sacco.dto;

public class GLPeriodDTO {
  private Integer periodMonth;
  private Integer periodYear;
  
  public GLPeriodDTO() {}
  
  public GLPeriodDTO(Integer periodMonth, Integer periodYear) {
    this.periodMonth = periodMonth;
    this.periodYear = periodYear;
  }
  
  public Integer getPeriodMonth() { return periodMonth; }
  public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
  
  public Integer getPeriodYear() { return periodYear; }
  public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
}
