package com.minet.sacco.dto;

import java.time.LocalDate;

public class PeriodDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodName;

    // Constructors
    public PeriodDTO() {
    }

    public PeriodDTO(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public PeriodDTO(LocalDate startDate, LocalDate endDate, String periodName) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.periodName = periodName;
    }

    // Getters and Setters
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }
}
