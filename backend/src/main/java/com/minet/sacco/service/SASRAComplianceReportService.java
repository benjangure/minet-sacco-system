package com.minet.sacco.service;

import com.minet.sacco.entity.Loan;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SASRA Compliance Report Service
 * Generates reports required by SASRA (Sacco Societies Regulatory Authority)
 * for regulatory compliance and inspection
 */
@Service
public class SASRAComplianceReportService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Generate Portfolio At Risk (PAR) Report
     * SASRA requires PAR 30 < 5% for good standing
     */
    public PARReport generatePARReport(LocalDate asAtDate) {
        PARReport report = new PARReport();
        report.setGeneratedAt(LocalDateTime.now());
        report.setAsAtDate(asAtDate);

        // Get all disbursed loans
        List<Loan> allLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.Status.DISBURSED)
                .toList();

        if (allLoans.isEmpty()) {
            report.setTotalLoans(0);
            report.setTotalPortfolio(BigDecimal.ZERO);
            report.setPar30Amount(BigDecimal.ZERO);
            report.setPar90Amount(BigDecimal.ZERO);
            report.setPar30Ratio(BigDecimal.ZERO);
            report.setPar90Ratio(BigDecimal.ZERO);
            report.setComplianceStatus("PASS");
            return report;
        }

        BigDecimal totalPortfolio = BigDecimal.ZERO;
        BigDecimal par30Amount = BigDecimal.ZERO;
        BigDecimal par90Amount = BigDecimal.ZERO;

        for (Loan loan : allLoans) {
            totalPortfolio = totalPortfolio.add(loan.getAmount());

            // Calculate days overdue
            LocalDate expectedEndDate = loan.getDisbursementDate().toLocalDate()
                    .plusMonths(loan.getTermMonths());
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(expectedEndDate, asAtDate);

            if (daysOverdue >= 30) {
                par30Amount = par30Amount.add(loan.getOutstandingBalance());
            }
            if (daysOverdue >= 90) {
                par90Amount = par90Amount.add(loan.getOutstandingBalance());
            }
        }

        // Calculate ratios
        BigDecimal par30Ratio = totalPortfolio.compareTo(BigDecimal.ZERO) > 0
                ? par30Amount.divide(totalPortfolio, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal par90Ratio = totalPortfolio.compareTo(BigDecimal.ZERO) > 0
                ? par90Amount.divide(totalPortfolio, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        report.setTotalLoans(allLoans.size());
        report.setTotalPortfolio(totalPortfolio);
        report.setPar30Amount(par30Amount);
        report.setPar90Amount(par90Amount);
        report.setPar30Ratio(par30Ratio);
        report.setPar90Ratio(par90Ratio);

        // SASRA compliance: PAR 30 must be < 5%
        boolean par30Compliant = par30Ratio.compareTo(new BigDecimal("5")) < 0;
        boolean par90Compliant = par90Ratio.compareTo(new BigDecimal("2")) < 0;

        report.setComplianceStatus(par30Compliant && par90Compliant ? "PASS" : "FAIL");
        report.setPar30Compliant(par30Compliant);
        report.setPar90Compliant(par90Compliant);

        return report;
    }

    /**
     * Generate Capital Adequacy Report
     * SASRA requires: Core capital ≥ 10% of total assets
     */
    public CapitalAdequacyReport generateCapitalAdequacyReport(LocalDate asAtDate) {
        CapitalAdequacyReport report = new CapitalAdequacyReport();
        report.setGeneratedAt(LocalDateTime.now());
        report.setAsAtDate(asAtDate);

        // Calculate total assets (loans outstanding)
        BigDecimal totalAssets = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.Status.DISBURSED || l.getStatus() == Loan.Status.APPROVED)
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate core capital (member shares)
        BigDecimal coreCapital = accountRepository.findAll().stream()
                .filter(a -> a.getAccountType().toString().equals("SHARES"))
                .map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate institutional capital (retained earnings/surplus)
        // For now, we'll use a simplified approach: total savings as institutional capital
        BigDecimal institutionalCapital = accountRepository.findAll().stream()
                .filter(a -> a.getAccountType().toString().equals("SAVINGS"))
                .map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate ratios
        BigDecimal coreCapitalRatio = totalAssets.compareTo(BigDecimal.ZERO) > 0
                ? coreCapital.divide(totalAssets, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal institutionalCapitalRatio = totalAssets.compareTo(BigDecimal.ZERO) > 0
                ? institutionalCapital.divide(totalAssets, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        report.setTotalAssets(totalAssets);
        report.setCoreCapital(coreCapital);
        report.setInstitutionalCapital(institutionalCapital);
        report.setCoreCapitalRatio(coreCapitalRatio);
        report.setInstitutionalCapitalRatio(institutionalCapitalRatio);

        // SASRA compliance: Core capital ≥ 10%, Institutional capital ≥ 8%
        boolean coreCapitalCompliant = coreCapitalRatio.compareTo(new BigDecimal("10")) >= 0;
        boolean institutionalCapitalCompliant = institutionalCapitalRatio.compareTo(new BigDecimal("8")) >= 0;

        report.setComplianceStatus(coreCapitalCompliant && institutionalCapitalCompliant ? "PASS" : "FAIL");
        report.setCoreCapitalCompliant(coreCapitalCompliant);
        report.setInstitutionalCapitalCompliant(institutionalCapitalCompliant);

        return report;
    }

    /**
     * Generate Provision for Bad Debts Report
     * Uses SASRA provisioning matrix
     */
    public ProvisionForBadDebtsReport generateProvisionForBadDebtsReport(LocalDate asAtDate) {
        ProvisionForBadDebtsReport report = new ProvisionForBadDebtsReport();
        report.setGeneratedAt(LocalDateTime.now());
        report.setAsAtDate(asAtDate);

        List<Loan> allLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.Status.DISBURSED)
                .toList();

        BigDecimal currentLoansProvision = BigDecimal.ZERO;
        BigDecimal overdue1to3Provision = BigDecimal.ZERO;
        BigDecimal overdue3to12Provision = BigDecimal.ZERO;
        BigDecimal overdue12PlusProvision = BigDecimal.ZERO;
        BigDecimal totalProvision = BigDecimal.ZERO;

        int currentLoansCount = 0;
        int overdue1to3Count = 0;
        int overdue3to12Count = 0;
        int overdue12PlusCount = 0;

        for (Loan loan : allLoans) {
            LocalDate expectedEndDate = loan.getDisbursementDate().toLocalDate()
                    .plusMonths(loan.getTermMonths());
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(expectedEndDate, asAtDate);

            BigDecimal outstanding = loan.getOutstandingBalance();

            if (daysOverdue <= 0) {
                // Current loans: 1% provision
                currentLoansProvision = currentLoansProvision.add(
                        outstanding.multiply(new BigDecimal("0.01")));
                currentLoansCount++;
            } else if (daysOverdue <= 90) {
                // 1-3 months overdue: 25% provision
                overdue1to3Provision = overdue1to3Provision.add(
                        outstanding.multiply(new BigDecimal("0.25")));
                overdue1to3Count++;
            } else if (daysOverdue <= 365) {
                // 3-12 months overdue: 50% provision
                overdue3to12Provision = overdue3to12Provision.add(
                        outstanding.multiply(new BigDecimal("0.50")));
                overdue3to12Count++;
            } else {
                // 12+ months overdue: 100% provision
                overdue12PlusProvision = overdue12PlusProvision.add(outstanding);
                overdue12PlusCount++;
            }
        }

        totalProvision = currentLoansProvision.add(overdue1to3Provision)
                .add(overdue3to12Provision).add(overdue12PlusProvision);

        report.setCurrentLoansCount(currentLoansCount);
        report.setCurrentLoansProvision(currentLoansProvision);
        report.setOverdue1to3Count(overdue1to3Count);
        report.setOverdue1to3Provision(overdue1to3Provision);
        report.setOverdue3to12Count(overdue3to12Count);
        report.setOverdue3to12Provision(overdue3to12Provision);
        report.setOverdue12PlusCount(overdue12PlusCount);
        report.setOverdue12PlusProvision(overdue12PlusProvision);
        report.setTotalProvision(totalProvision);

        return report;
    }

    /**
     * Generate comprehensive SASRA Regulatory Compliance Report
     * Combines all SASRA-monitored ratios
     */
    public SASRAComplianceReport generateSASRAComplianceReport(LocalDate asAtDate) {
        SASRAComplianceReport report = new SASRAComplianceReport();
        report.setGeneratedAt(LocalDateTime.now());
        report.setAsAtDate(asAtDate);

        // Generate all sub-reports
        PARReport parReport = generatePARReport(asAtDate);
        CapitalAdequacyReport capitalReport = generateCapitalAdequacyReport(asAtDate);
        ProvisionForBadDebtsReport provisionReport = generateProvisionForBadDebtsReport(asAtDate);

        // Set PAR metrics
        report.setPar30Ratio(parReport.getPar30Ratio());
        report.setPar90Ratio(parReport.getPar90Ratio());
        report.setPar30Compliant(parReport.isPar30Compliant());
        report.setPar90Compliant(parReport.isPar90Compliant());

        // Set Capital Adequacy metrics
        report.setCoreCapitalRatio(capitalReport.getCoreCapitalRatio());
        report.setInstitutionalCapitalRatio(capitalReport.getInstitutionalCapitalRatio());
        report.setCoreCapitalCompliant(capitalReport.isCoreCapitalCompliant());
        report.setInstitutionalCapitalCompliant(capitalReport.isInstitutionalCapitalCompliant());

        // Calculate Liquidity Ratio (Liquid assets / Total liabilities)
        BigDecimal liquidAssets = accountRepository.findAll().stream()
                .filter(a -> a.getAccountType().toString().equals("SAVINGS"))
                .map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = accountRepository.findAll().stream()
                .map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal liquidityRatio = totalLiabilities.compareTo(BigDecimal.ZERO) > 0
                ? liquidAssets.divide(totalLiabilities, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        report.setLiquidityRatio(liquidityRatio);
        report.setLiquidityCompliant(liquidityRatio.compareTo(new BigDecimal("20")) >= 0);

        // Calculate Savings to Loans Ratio
        BigDecimal totalSavings = liquidAssets;
        BigDecimal totalLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.Status.DISBURSED || l.getStatus() == Loan.Status.APPROVED)
                .map(Loan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal savingsToLoansRatio = totalLoans.compareTo(BigDecimal.ZERO) > 0
                ? totalSavings.divide(totalLoans, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        report.setSavingsToLoansRatio(savingsToLoansRatio);
        report.setSavingsToLoansCompliant(savingsToLoansRatio.compareTo(new BigDecimal("1")) >= 0);

        // Overall compliance status
        boolean overallCompliant = report.isPar30Compliant() && report.isPar90Compliant()
                && report.isCoreCapitalCompliant() && report.isInstitutionalCapitalCompliant()
                && report.isLiquidityCompliant() && report.isSavingsToLoansCompliant();

        report.setOverallComplianceStatus(overallCompliant ? "COMPLIANT" : "NON-COMPLIANT");

        return report;
    }

    // ===== DTO Classes =====

    public static class PARReport {
        private LocalDateTime generatedAt;
        private LocalDate asAtDate;
        private int totalLoans;
        private BigDecimal totalPortfolio;
        private BigDecimal par30Amount;
        private BigDecimal par90Amount;
        private BigDecimal par30Ratio;
        private BigDecimal par90Ratio;
        private String complianceStatus;
        private boolean par30Compliant;
        private boolean par90Compliant;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public LocalDate getAsAtDate() { return asAtDate; }
        public void setAsAtDate(LocalDate asAtDate) { this.asAtDate = asAtDate; }

        public int getTotalLoans() { return totalLoans; }
        public void setTotalLoans(int totalLoans) { this.totalLoans = totalLoans; }

        public BigDecimal getTotalPortfolio() { return totalPortfolio; }
        public void setTotalPortfolio(BigDecimal totalPortfolio) { this.totalPortfolio = totalPortfolio; }

        public BigDecimal getPar30Amount() { return par30Amount; }
        public void setPar30Amount(BigDecimal par30Amount) { this.par30Amount = par30Amount; }

        public BigDecimal getPar90Amount() { return par90Amount; }
        public void setPar90Amount(BigDecimal par90Amount) { this.par90Amount = par90Amount; }

        public BigDecimal getPar30Ratio() { return par30Ratio; }
        public void setPar30Ratio(BigDecimal par30Ratio) { this.par30Ratio = par30Ratio; }

        public BigDecimal getPar90Ratio() { return par90Ratio; }
        public void setPar90Ratio(BigDecimal par90Ratio) { this.par90Ratio = par90Ratio; }

        public String getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

        public boolean isPar30Compliant() { return par30Compliant; }
        public void setPar30Compliant(boolean par30Compliant) { this.par30Compliant = par30Compliant; }

        public boolean isPar90Compliant() { return par90Compliant; }
        public void setPar90Compliant(boolean par90Compliant) { this.par90Compliant = par90Compliant; }
    }

    public static class CapitalAdequacyReport {
        private LocalDateTime generatedAt;
        private LocalDate asAtDate;
        private BigDecimal totalAssets;
        private BigDecimal coreCapital;
        private BigDecimal institutionalCapital;
        private BigDecimal coreCapitalRatio;
        private BigDecimal institutionalCapitalRatio;
        private String complianceStatus;
        private boolean coreCapitalCompliant;
        private boolean institutionalCapitalCompliant;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public LocalDate getAsAtDate() { return asAtDate; }
        public void setAsAtDate(LocalDate asAtDate) { this.asAtDate = asAtDate; }

        public BigDecimal getTotalAssets() { return totalAssets; }
        public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }

        public BigDecimal getCoreCapital() { return coreCapital; }
        public void setCoreCapital(BigDecimal coreCapital) { this.coreCapital = coreCapital; }

        public BigDecimal getInstitutionalCapital() { return institutionalCapital; }
        public void setInstitutionalCapital(BigDecimal institutionalCapital) { this.institutionalCapital = institutionalCapital; }

        public BigDecimal getCoreCapitalRatio() { return coreCapitalRatio; }
        public void setCoreCapitalRatio(BigDecimal coreCapitalRatio) { this.coreCapitalRatio = coreCapitalRatio; }

        public BigDecimal getInstitutionalCapitalRatio() { return institutionalCapitalRatio; }
        public void setInstitutionalCapitalRatio(BigDecimal institutionalCapitalRatio) { this.institutionalCapitalRatio = institutionalCapitalRatio; }

        public String getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

        public boolean isCoreCapitalCompliant() { return coreCapitalCompliant; }
        public void setCoreCapitalCompliant(boolean coreCapitalCompliant) { this.coreCapitalCompliant = coreCapitalCompliant; }

        public boolean isInstitutionalCapitalCompliant() { return institutionalCapitalCompliant; }
        public void setInstitutionalCapitalCompliant(boolean institutionalCapitalCompliant) { this.institutionalCapitalCompliant = institutionalCapitalCompliant; }
    }

    public static class ProvisionForBadDebtsReport {
        private LocalDateTime generatedAt;
        private LocalDate asAtDate;
        private int currentLoansCount;
        private BigDecimal currentLoansProvision;
        private int overdue1to3Count;
        private BigDecimal overdue1to3Provision;
        private int overdue3to12Count;
        private BigDecimal overdue3to12Provision;
        private int overdue12PlusCount;
        private BigDecimal overdue12PlusProvision;
        private BigDecimal totalProvision;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public LocalDate getAsAtDate() { return asAtDate; }
        public void setAsAtDate(LocalDate asAtDate) { this.asAtDate = asAtDate; }

        public int getCurrentLoansCount() { return currentLoansCount; }
        public void setCurrentLoansCount(int currentLoansCount) { this.currentLoansCount = currentLoansCount; }

        public BigDecimal getCurrentLoansProvision() { return currentLoansProvision; }
        public void setCurrentLoansProvision(BigDecimal currentLoansProvision) { this.currentLoansProvision = currentLoansProvision; }

        public int getOverdue1to3Count() { return overdue1to3Count; }
        public void setOverdue1to3Count(int overdue1to3Count) { this.overdue1to3Count = overdue1to3Count; }

        public BigDecimal getOverdue1to3Provision() { return overdue1to3Provision; }
        public void setOverdue1to3Provision(BigDecimal overdue1to3Provision) { this.overdue1to3Provision = overdue1to3Provision; }

        public int getOverdue3to12Count() { return overdue3to12Count; }
        public void setOverdue3to12Count(int overdue3to12Count) { this.overdue3to12Count = overdue3to12Count; }

        public BigDecimal getOverdue3to12Provision() { return overdue3to12Provision; }
        public void setOverdue3to12Provision(BigDecimal overdue3to12Provision) { this.overdue3to12Provision = overdue3to12Provision; }

        public int getOverdue12PlusCount() { return overdue12PlusCount; }
        public void setOverdue12PlusCount(int overdue12PlusCount) { this.overdue12PlusCount = overdue12PlusCount; }

        public BigDecimal getOverdue12PlusProvision() { return overdue12PlusProvision; }
        public void setOverdue12PlusProvision(BigDecimal overdue12PlusProvision) { this.overdue12PlusProvision = overdue12PlusProvision; }

        public BigDecimal getTotalProvision() { return totalProvision; }
        public void setTotalProvision(BigDecimal totalProvision) { this.totalProvision = totalProvision; }
    }

    public static class SASRAComplianceReport {
        private LocalDateTime generatedAt;
        private LocalDate asAtDate;
        private BigDecimal par30Ratio;
        private BigDecimal par90Ratio;
        private boolean par30Compliant;
        private boolean par90Compliant;
        private BigDecimal coreCapitalRatio;
        private BigDecimal institutionalCapitalRatio;
        private boolean coreCapitalCompliant;
        private boolean institutionalCapitalCompliant;
        private BigDecimal liquidityRatio;
        private boolean liquidityCompliant;
        private BigDecimal savingsToLoansRatio;
        private boolean savingsToLoansCompliant;
        private String overallComplianceStatus;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public LocalDate getAsAtDate() { return asAtDate; }
        public void setAsAtDate(LocalDate asAtDate) { this.asAtDate = asAtDate; }

        public BigDecimal getPar30Ratio() { return par30Ratio; }
        public void setPar30Ratio(BigDecimal par30Ratio) { this.par30Ratio = par30Ratio; }

        public BigDecimal getPar90Ratio() { return par90Ratio; }
        public void setPar90Ratio(BigDecimal par90Ratio) { this.par90Ratio = par90Ratio; }

        public boolean isPar30Compliant() { return par30Compliant; }
        public void setPar30Compliant(boolean par30Compliant) { this.par30Compliant = par30Compliant; }

        public boolean isPar90Compliant() { return par90Compliant; }
        public void setPar90Compliant(boolean par90Compliant) { this.par90Compliant = par90Compliant; }

        public BigDecimal getCoreCapitalRatio() { return coreCapitalRatio; }
        public void setCoreCapitalRatio(BigDecimal coreCapitalRatio) { this.coreCapitalRatio = coreCapitalRatio; }

        public BigDecimal getInstitutionalCapitalRatio() { return institutionalCapitalRatio; }
        public void setInstitutionalCapitalRatio(BigDecimal institutionalCapitalRatio) { this.institutionalCapitalRatio = institutionalCapitalRatio; }

        public boolean isCoreCapitalCompliant() { return coreCapitalCompliant; }
        public void setCoreCapitalCompliant(boolean coreCapitalCompliant) { this.coreCapitalCompliant = coreCapitalCompliant; }

        public boolean isInstitutionalCapitalCompliant() { return institutionalCapitalCompliant; }
        public void setInstitutionalCapitalCompliant(boolean institutionalCapitalCompliant) { this.institutionalCapitalCompliant = institutionalCapitalCompliant; }

        public BigDecimal getLiquidityRatio() { return liquidityRatio; }
        public void setLiquidityRatio(BigDecimal liquidityRatio) { this.liquidityRatio = liquidityRatio; }

        public boolean isLiquidityCompliant() { return liquidityCompliant; }
        public void setLiquidityCompliant(boolean liquidityCompliant) { this.liquidityCompliant = liquidityCompliant; }

        public BigDecimal getSavingsToLoansRatio() { return savingsToLoansRatio; }
        public void setSavingsToLoansRatio(BigDecimal savingsToLoansRatio) { this.savingsToLoansRatio = savingsToLoansRatio; }

        public boolean isSavingsToLoansCompliant() { return savingsToLoansCompliant; }
        public void setSavingsToLoansCompliant(boolean savingsToLoansCompliant) { this.savingsToLoansCompliant = savingsToLoansCompliant; }

        public String getOverallComplianceStatus() { return overallComplianceStatus; }
        public void setOverallComplianceStatus(String overallComplianceStatus) { this.overallComplianceStatus = overallComplianceStatus; }
    }
}
