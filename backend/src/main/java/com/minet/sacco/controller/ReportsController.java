package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.ProfitLossReportDTO;
import com.minet.sacco.service.ReportsService;
import com.minet.sacco.service.ReportExportService;
import com.minet.sacco.service.ProfitLossReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    // ===== CASHBOOK ENDPOINTS =====
    @GetMapping("/cashbook")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<ReportsService.CashbookReport>> getCashbook(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String accountType) {
        
        ReportsService.CashbookReport report = reportsService.generateCashbook(startDate, endDate, memberNumber, transactionType, accountType);
        return ResponseEntity.ok(ApiResponse.success("Cashbook report generated successfully", report));
    }

    @GetMapping("/cashbook/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportCashbookExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String accountType) throws Exception {
        
        ReportsService.CashbookReport report = reportsService.generateCashbook(startDate, endDate, memberNumber, transactionType, accountType);
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
            @RequestParam(required = false) String accountType) throws Exception {
        
        ReportsService.CashbookReport report = reportsService.generateCashbook(startDate, endDate, memberNumber, transactionType, accountType);
        byte[] pdfFile = reportExportService.exportCashbookToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cashbook_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== TRIAL BALANCE ENDPOINTS =====
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<ReportsService.TrialBalanceReport>> getTrialBalance(
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String accountType) {
        
        ReportsService.TrialBalanceReport report = reportsService.generateTrialBalance(memberNumber, accountType);
        return ResponseEntity.ok(ApiResponse.success("Trial balance report generated successfully", report));
    }

    @GetMapping("/trial-balance/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportTrialBalanceExcel(
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String accountType) throws Exception {
        
        ReportsService.TrialBalanceReport report = reportsService.generateTrialBalance(memberNumber, accountType);
        byte[] excelFile = reportExportService.exportTrialBalanceToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trial_balance_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/trial-balance/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportTrialBalancePdf(
            @RequestParam(required = false) String memberNumber,
            @RequestParam(required = false) String accountType) throws Exception {
        
        ReportsService.TrialBalanceReport report = reportsService.generateTrialBalance(memberNumber, accountType);
        byte[] pdfFile = reportExportService.exportTrialBalanceToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trial_balance_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== BALANCE SHEET ENDPOINTS =====
    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<ReportsService.BalanceSheetReport>> getBalanceSheet() {
        ReportsService.BalanceSheetReport report = reportsService.generateBalanceSheet();
        return ResponseEntity.ok(ApiResponse.success("Balance sheet report generated successfully", report));
    }

    @GetMapping("/balance-sheet/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportBalanceSheetExcel() throws Exception {
        ReportsService.BalanceSheetReport report = reportsService.generateBalanceSheet();
        byte[] excelFile = reportExportService.exportBalanceSheetToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_sheet_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/balance-sheet/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
    public ResponseEntity<byte[]> exportBalanceSheetPdf() throws Exception {
        ReportsService.BalanceSheetReport report = reportsService.generateBalanceSheet();
        byte[] pdfFile = reportExportService.exportBalanceSheetToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_sheet_" + LocalDate.now() + ".pdf")
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
}




