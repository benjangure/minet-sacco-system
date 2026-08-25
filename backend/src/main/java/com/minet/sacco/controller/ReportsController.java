package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.MemberContributionsReportDTO;
import com.minet.sacco.dto.ProfitLossReportDTO;
import com.minet.sacco.dto.WithdrawalMonitoringReportDTO;
import com.minet.sacco.dto.GuarantorReportDTO;
import com.minet.sacco.dto.OverCommittedGuarantorDTO;
import com.minet.sacco.dto.LoanEligibilityReportDTO;
import com.minet.sacco.dto.MonthlyContributionTrackingDTO;
import com.minet.sacco.dto.ExitedMemberLoanDTO;
import com.minet.sacco.service.ReportsService;
import com.minet.sacco.service.ReportExportService;
import com.minet.sacco.service.ProfitLossReportService;
import com.minet.sacco.service.WithdrawalMonitoringReportService;
import com.minet.sacco.service.GuarantorReportService;
import com.minet.sacco.service.LoanEligibilityReportService;
import com.minet.sacco.service.MonthlyContributionTrackingService;
import com.minet.sacco.service.GLCalculationService;
import com.minet.sacco.service.BalanceSheetService;
import com.minet.sacco.service.ExitedMemberLoanReportService;
import com.minet.sacco.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin
public class ReportsController {

    @Autowired
    private ReportsService reportsService;

    @Autowired
    private ReportExportService reportExportService;

    @Autowired
    private ProfitLossReportService profitLossReportService;

    @Autowired
    private WithdrawalMonitoringReportService withdrawalMonitoringReportService;

    @Autowired
    private GuarantorReportService guarantorReportService;

    @Autowired
    private LoanEligibilityReportService loanEligibilityReportService;

    @Autowired
    private MonthlyContributionTrackingService monthlyContributionTrackingService;

    @Autowired
    private GLCalculationService glCalculationService;

    @Autowired
    private BalanceSheetService balanceSheetService;

    @Autowired
    private ExitedMemberLoanReportService exitedMemberLoanReportService;

    @Autowired
    private AccountService accountService;

    // ===== CASHBOOK ENDPOINTS =====
    @GetMapping("/cashbook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    @org.springframework.cache.annotation.Cacheable(value = "cashbookReportAPI", key = "#startDate + '-' + #endDate + '-' + #memberNumber + '-' + #transactionType + '-' + #accountType", unless = "#result == null")
    public ResponseEntity<ApiResponse<ReportsService.CashbookReport>> getCashbook(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) BigDecimal openingBalance) {
        
        if (openingBalance == null) {
            openingBalance = BigDecimal.ZERO;
        }
        
        ReportsService.CashbookReport report = reportsService.generateCashbook(startDate, endDate, memberNumber, transactionType, accountType, openingBalance);
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.maxAge(5, java.util.concurrent.TimeUnit.MINUTES))
                .body(ApiResponse.success("Cashbook report generated successfully", report));
    }

    @GetMapping("/cashbook/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportCashbookExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) BigDecimal openingBalance) throws Exception {
        
        if (openingBalance == null) {
            openingBalance = BigDecimal.ZERO;
        }
        
        ReportsService.CashbookReport report = reportsService.generateCashbook(startDate, endDate, memberNumber, transactionType, accountType, openingBalance);
        byte[] excelFile = reportExportService.exportCashbookToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cashbook_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/cashbook/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportCashbookPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) BigDecimal openingBalance) throws Exception {
        
        if (openingBalance == null) {
            openingBalance = BigDecimal.ZERO;
        }
        
        ReportsService.CashbookReport report = reportsService.generateCashbook(startDate, endDate, memberNumber, transactionType, accountType, openingBalance);
        byte[] pdfFile = reportExportService.exportCashbookToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cashbook_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== TRIAL BALANCE ENDPOINTS =====
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<com.minet.sacco.dto.TrialBalanceDTO>> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Integer periodYear) {

        if (asOfDate == null) asOfDate = LocalDate.now();
        if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
        if (periodYear == null) periodYear = asOfDate.getYear();

        com.minet.sacco.dto.TrialBalanceDTO report =
                glCalculationService.generateTrialBalance(asOfDate, periodMonth, periodYear);
        return ResponseEntity.ok(ApiResponse.success("Trial balance report generated successfully", report));
    }

    @GetMapping("/trial-balance/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportTrialBalanceExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Integer periodYear) throws Exception {
        
        if (asOfDate == null) asOfDate = LocalDate.now();
        if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
        if (periodYear == null) periodYear = asOfDate.getYear();
        
        Object report = glCalculationService.generateTrialBalance(asOfDate, periodMonth, periodYear);
        byte[] excelFile = reportExportService.exportTrialBalanceToExcel((com.minet.sacco.dto.TrialBalanceDTO) report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trial_balance_gl_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/trial-balance/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportTrialBalancePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Integer periodYear) throws Exception {
        
        if (asOfDate == null) asOfDate = LocalDate.now();
        if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
        if (periodYear == null) periodYear = asOfDate.getYear();
        
        Object report = glCalculationService.generateTrialBalance(asOfDate, periodMonth, periodYear);
        byte[] pdfFile = reportExportService.exportTrialBalanceToPdf((com.minet.sacco.dto.TrialBalanceDTO) report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trial_balance_gl_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== BALANCE SHEET ENDPOINTS =====
    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<com.minet.sacco.dto.BalanceSheetDTO>> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Integer periodYear) {

        if (asOfDate == null) asOfDate = LocalDate.now();
        if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
        if (periodYear == null) periodYear = asOfDate.getYear();

        com.minet.sacco.dto.BalanceSheetDTO report =
                balanceSheetService.generateBalanceSheet(asOfDate, periodMonth, periodYear);
        return ResponseEntity.ok(ApiResponse.success("Balance sheet report generated successfully", report));
    }

    @GetMapping("/balance-sheet/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportBalanceSheetExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Integer periodYear) throws Exception {
        
        if (asOfDate == null) asOfDate = LocalDate.now();
        if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
        if (periodYear == null) periodYear = asOfDate.getYear();
        
        com.minet.sacco.dto.BalanceSheetDTO report = balanceSheetService.generateBalanceSheet(asOfDate, periodMonth, periodYear);
        byte[] excelFile = reportExportService.exportBalanceSheetToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_sheet_gl_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/balance-sheet/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportBalanceSheetPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Integer periodYear) throws Exception {
        
        if (asOfDate == null) asOfDate = LocalDate.now();
        if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
        if (periodYear == null) periodYear = asOfDate.getYear();
        
        com.minet.sacco.dto.BalanceSheetDTO report = balanceSheetService.generateBalanceSheet(asOfDate, periodMonth, periodYear);
        byte[] pdfFile = reportExportService.exportBalanceSheetToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_sheet_gl_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== MEMBER STATEMENT ENDPOINTS =====
    @GetMapping("/member-statement/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<ReportsService.MemberStatementReport>> getMemberStatement(
            @PathVariable Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        ReportsService.MemberStatementReport report = reportsService.generateMemberStatement(memberId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Member statement generated successfully", report));
    }

    @GetMapping("/member-statement/{memberId}/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportMemberStatementExcel(
            @PathVariable Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        
        ReportsService.MemberStatementReport report = reportsService.generateMemberStatement(memberId, startDate, endDate);
        byte[] excelFile = reportExportService.exportMemberStatementToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member_statement_" + report.getMemberNumber() + "_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/member-statement/{memberId}/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CUSTOMER_SUPPORT', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportMemberStatementPdf(
            @PathVariable Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        
        ReportsService.MemberStatementReport report = reportsService.generateMemberStatement(memberId, startDate, endDate);
        byte[] pdfFile = reportExportService.exportMemberStatementToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member_statement_" + report.getMemberNumber() + "_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== LOAN REGISTER ENDPOINTS =====
    @GetMapping("/loan-register")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<ReportsService.LoanRegisterReport>> getLoanRegister(
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String loanStatus,
            @RequestParam(required = false) String loanProduct) {
        
        ReportsService.LoanRegisterReport report = reportsService.generateLoanRegister(memberNumber, loanStatus, loanProduct);
        return ResponseEntity.ok(ApiResponse.success("Loan register report generated successfully", report));
    }

    @GetMapping("/loan-register/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportLoanRegisterExcel(
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String loanStatus,
            @RequestParam(required = false) String loanProduct) throws Exception {
        
        ReportsService.LoanRegisterReport report = reportsService.generateLoanRegister(memberNumber, loanStatus, loanProduct);
        byte[] excelFile = reportExportService.exportLoanRegisterToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loan_register_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/loan-register/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportLoanRegisterPdf(
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String loanStatus,
            @RequestParam(required = false) String loanProduct) throws Exception {
        
        ReportsService.LoanRegisterReport report = reportsService.generateLoanRegister(memberNumber, loanStatus, loanProduct);
        byte[] pdfFile = reportExportService.exportLoanRegisterToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loan_register_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== PROFIT & LOSS REPORT ENDPOINTS =====
    @GetMapping("/profit-loss")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<ProfitLossReportDTO>> getProfitLossReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Start date and end date are required"));
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Start date must be before or equal to end date"));
        }
        
        ProfitLossReportDTO report = profitLossReportService.generateProfitLossReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Profit & Loss report generated successfully", report));
    }

    @GetMapping("/profit-loss/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportProfitLossExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().build();
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        ProfitLossReportDTO report = profitLossReportService.generateProfitLossReport(startDate, endDate);
        byte[] excelFile = reportExportService.exportProfitLossToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profit_loss_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/profit-loss/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportProfitLossPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().build();
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        ProfitLossReportDTO report = profitLossReportService.generateProfitLossReport(startDate, endDate);
        byte[] pdfFile = reportExportService.exportProfitLossToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profit_loss_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== WITHDRAWAL MONITORING REPORT ENDPOINTS =====
    @GetMapping("/withdrawal-monitoring")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<WithdrawalMonitoringReportDTO>> getWithdrawalMonitoringReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String withdrawalMethod,
            @RequestParam(required = false) String transactionStatus) {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Start date and end date are required"));
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Start date must be before or equal to end date"));
        }
        
        WithdrawalMonitoringReportDTO report = 
            withdrawalMonitoringReportService.generateWithdrawalMonitoringReport(startDate, endDate, memberNumber, withdrawalMethod, transactionStatus);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal monitoring report generated successfully", report));
    }

    @GetMapping("/withdrawal-monitoring/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportWithdrawalMonitoringExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String withdrawalMethod,
            @RequestParam(required = false) String transactionStatus) throws Exception {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().build();
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        WithdrawalMonitoringReportDTO report = 
            withdrawalMonitoringReportService.generateWithdrawalMonitoringReport(startDate, endDate, memberNumber, withdrawalMethod, transactionStatus);
        byte[] excelFile = reportExportService.exportWithdrawalMonitoringToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=withdrawal_monitoring_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/withdrawal-monitoring/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportWithdrawalMonitoringPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String withdrawalMethod,
            @RequestParam(required = false) String transactionStatus) throws Exception {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().build();
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        WithdrawalMonitoringReportDTO report = 
            withdrawalMonitoringReportService.generateWithdrawalMonitoringReport(startDate, endDate, memberNumber, withdrawalMethod, transactionStatus);
        byte[] pdfFile = reportExportService.exportWithdrawalMonitoringToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=withdrawal_monitoring_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== GUARANTOR REPORT ENDPOINTS =====
    @GetMapping("/guarantor/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<GuarantorReportDTO>> getGuarantorReport(
            @PathVariable Long memberId,
            @RequestParam(required = false) String guarantorStatus) {
        
        GuarantorReportDTO report = guarantorReportService.generateGuarantorReport(memberId, guarantorStatus);
        return ResponseEntity.ok(ApiResponse.success("Guarantor report generated successfully", report));
    }

    @GetMapping("/guarantor/all")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<GuarantorReportDTO>> getGuarantorReportAll() {
        GuarantorReportDTO report = guarantorReportService.generateGuarantorReportAll();
        return ResponseEntity.ok(ApiResponse.success("Guarantor report (all members) generated successfully", report));
    }

    @GetMapping("/guarantor/{memberId}/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER')")
    public ResponseEntity<byte[]> exportGuarantorReportExcel(@PathVariable Long memberId) throws Exception {
        GuarantorReportDTO report = guarantorReportService.generateGuarantorReport(memberId, null);
        byte[] excelFile = reportExportService.exportGuarantorReportToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guarantor_report_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/guarantor/{memberId}/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER')")
    public ResponseEntity<byte[]> exportGuarantorReportPdf(@PathVariable Long memberId) throws Exception {
        GuarantorReportDTO report = guarantorReportService.generateGuarantorReport(memberId, null);
        byte[] pdfFile = reportExportService.exportGuarantorReportToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guarantor_report_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== LOAN ELIGIBILITY REPORT ENDPOINTS =====
    @GetMapping("/loan-eligibility/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<LoanEligibilityReportDTO>> getLoanEligibilityReport(@PathVariable Long memberId) {
        LoanEligibilityReportDTO report = loanEligibilityReportService.generateLoanEligibilityReport(memberId);
        return ResponseEntity.ok(ApiResponse.success("Loan eligibility report generated successfully", report));
    }

    @GetMapping("/loan-eligibility/{memberId}/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<byte[]> exportLoanEligibilityExcel(@PathVariable Long memberId) throws Exception {
        LoanEligibilityReportDTO report = loanEligibilityReportService.generateLoanEligibilityReport(memberId);
        byte[] excelFile = reportExportService.exportLoanEligibilityToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loan_eligibility_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/loan-eligibility/{memberId}/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<byte[]> exportLoanEligibilityPdf(@PathVariable Long memberId) throws Exception {
        LoanEligibilityReportDTO report = loanEligibilityReportService.generateLoanEligibilityReport(memberId);
        byte[] pdfFile = reportExportService.exportLoanEligibilityToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loan_eligibility_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== MONTHLY CONTRIBUTION TRACKING REPORT ENDPOINTS =====
    @GetMapping("/monthly-contribution-tracking")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<MonthlyContributionTrackingDTO>> getMonthlyContributionTrackingReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String batchStatus) {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Start date and end date are required"));
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Start date must be before or equal to end date"));
        }
        
        MonthlyContributionTrackingDTO report = 
            monthlyContributionTrackingService.generateMonthlyContributionTrackingReport(startDate, endDate, batchStatus);
        return ResponseEntity.ok(ApiResponse.success("Monthly contribution tracking report generated successfully", report));
    }

    @GetMapping("/monthly-contribution-tracking/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportMonthlyContributionTrackingExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String batchStatus) throws Exception {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().build();
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        MonthlyContributionTrackingDTO report = 
            monthlyContributionTrackingService.generateMonthlyContributionTrackingReport(startDate, endDate, batchStatus);
        byte[] excelFile = reportExportService.exportMonthlyContributionTrackingToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=monthly_contribution_tracking_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/monthly-contribution-tracking/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportMonthlyContributionTrackingPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String batchStatus) throws Exception {
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().build();
        }
        
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        MonthlyContributionTrackingDTO report = 
            monthlyContributionTrackingService.generateMonthlyContributionTrackingReport(startDate, endDate, batchStatus);
        byte[] pdfFile = reportExportService.exportMonthlyContributionTrackingToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=monthly_contribution_tracking_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }


    /**
     * Get Over-Committed Guarantor Report
     * Identifies guarantors where frozen pledges EXCEED available savings
     * CRITICAL RISK REPORT: Shows guarantors who may not have funds to cover their guarantees
     * 
     * Business Logic:
     * - Over-Committed = Frozen Pledges > (Total Savings - Frozen Self-Guarantees)
     * - Example: Member with KES 100k savings, KES 40k frozen for self-guarantees = KES 60k available
     *            But pledged KES 75k as guarantor on others = Over-committed by KES 15k
     */
    @GetMapping("/over-committed-guarantors")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<OverCommittedGuarantorDTO>> getOverCommittedGuarantorReport() {
        OverCommittedGuarantorDTO report = guarantorReportService.generateOverCommittedGuarantorReport();
        return ResponseEntity.ok(ApiResponse.success("Over-committed guarantor report generated successfully", report));
    }

    @GetMapping("/over-committed-guarantors/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportOverCommittedGuarantorExcel() throws Exception {
        OverCommittedGuarantorDTO report = guarantorReportService.generateOverCommittedGuarantorReport();
        byte[] excelFile = reportExportService.exportOverCommittedGuarantorToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=over_committed_guarantors_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/over-committed-guarantors/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportOverCommittedGuarantorPdf() throws Exception {
        OverCommittedGuarantorDTO report = guarantorReportService.generateOverCommittedGuarantorReport();
        byte[] pdfFile = reportExportService.exportOverCommittedGuarantorToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=over_committed_guarantors_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(pdfFile);
    }

    // ===== EXITED MEMBERS WITH OUTSTANDING LOANS ENDPOINTS =====
    @GetMapping("/exited-members-outstanding-loans")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR', 'ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<ExitedMemberLoanDTO>> getExitedMembersOutstandingLoansReport() {
        ExitedMemberLoanDTO report = exitedMemberLoanReportService.generateExitedMembersOutstandingLoansReport();
        return ResponseEntity.ok(ApiResponse.success("Exited members with outstanding loans report generated successfully", report));
    }

    @GetMapping("/exited-members-outstanding-loans/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportExitedMemberLoanExcel() throws Exception {
        ExitedMemberLoanDTO report = exitedMemberLoanReportService.generateExitedMembersOutstandingLoansReport();
        byte[] excelFile = reportExportService.exportExitedMemberLoanToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exited_members_outstanding_loans_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/exited-members-outstanding-loans/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportExitedMemberLoanPdf() throws Exception {
        ExitedMemberLoanDTO report = exitedMemberLoanReportService.generateExitedMembersOutstandingLoansReport();
        byte[] pdfFile = reportExportService.exportExitedMemberLoanToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=exited_members_outstanding_loans_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(pdfFile);
    }

    // ===== MEMBER CONTRIBUTIONS REPORT ENDPOINTS =====

    /**
     * Fetch (JSON) a member's full contribution / transaction history.
     * Accessible to ADMIN, TREASURER, TELLER, LOAN_OFFICER, AUDITOR, and CUSTOMER_SUPPORT.
     */
    @GetMapping("/member-contributions/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TREASURER','ROLE_TELLER','ROLE_AUDITOR','ROLE_LOAN_OFFICER','ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<MemberContributionsReportDTO>> getMemberContributionsReport(
            @PathVariable String memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountType) {

        MemberContributionsReportDTO report =
                accountService.getMemberContributionsReport(memberId, startDate, endDate, accountType);
        return ResponseEntity.ok(ApiResponse.success(
                "Member contributions report generated successfully", report));
    }

    @GetMapping("/member-contributions/{memberId}/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TREASURER','ROLE_TELLER','ROLE_AUDITOR','ROLE_LOAN_OFFICER','ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<byte[]> exportMemberContributionsExcel(
            @PathVariable String memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountType) throws Exception {

        MemberContributionsReportDTO report =
                accountService.getMemberContributionsReport(memberId, startDate, endDate, accountType);
        byte[] excelFile = reportExportService.exportMemberContributionsToExcel(report);

        String filename = "member_contributions_"
                + (report.getMemberNumber() != null ? report.getMemberNumber() : memberId)
                + "_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/member-contributions/{memberId}/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TREASURER','ROLE_TELLER','ROLE_AUDITOR','ROLE_LOAN_OFFICER','ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<byte[]> exportMemberContributionsPdf(
            @PathVariable String memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountType) throws Exception {

        MemberContributionsReportDTO report =
                accountService.getMemberContributionsReport(memberId, startDate, endDate, accountType);
        byte[] pdfFile = reportExportService.exportMemberContributionsToPdf(report);

        String filename = "member_contributions_"
                + (report.getMemberNumber() != null ? report.getMemberNumber() : memberId)
                + "_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(pdfFile);
    }
}
