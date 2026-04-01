package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProfitLossReportDTO {
    private PeriodDTO period;
    private RevenueDTO revenue;
    private ExpenseDTO expenses;
    private BigDecimal netProfitLoss;
    private BigDecimal profitMargin;
    private LocalDateTime generatedAt;

    // Constructors
    public ProfitLossReportDTO() {
    }

    public ProfitLossReportDTO(PeriodDTO period, RevenueDTO revenue, ExpenseDTO expenses,
                               BigDecimal netProfitLoss, BigDecimal profitMargin) {
        this.period = period;
        this.revenue = revenue;
        this.expenses = expenses;
        this.netProfitLoss = netProfitLoss;
        this.profitMargin = profitMargin;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public PeriodDTO getPeriod() {
        return period;
    }

    public void setPeriod(PeriodDTO period) {
        this.period = period;
    }

    public RevenueDTO getRevenue() {
        return revenue;
    }

    public void setRevenue(RevenueDTO revenue) {
        this.revenue = revenue;
    }

    public ExpenseDTO getExpenses() {
        return expenses;
    }

    public void setExpenses(ExpenseDTO expenses) {
        this.expenses = expenses;
    }

    public BigDecimal getNetProfitLoss() {
        return netProfitLoss;
    }

    public void setNetProfitLoss(BigDecimal netProfitLoss) {
        this.netProfitLoss = netProfitLoss;
    }

    public BigDecimal getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(BigDecimal profitMargin) {
        this.profitMargin = profitMargin;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
