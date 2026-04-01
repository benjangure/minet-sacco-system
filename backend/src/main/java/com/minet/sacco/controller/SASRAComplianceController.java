package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.service.SASRAComplianceReportService;
import com.minet.sacco.service.SASRAReportExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * SASRA Compliance Report Controller
 * Endpoints for generating SASRA-required regulatory reports
 * Access: ADMIN, AUDITOR, TREASURER
 */
@RestController
@RequestMapping("/api/reports/sasra")
@CrossOrigin
public class SASRAComplianceController {

    @Autowired
    private SASRAComplianceReportService sasraReportService;

    @Autowired
    private SASRAReportExportService reportExportService;

    // ===== PORTFOLIO AT RISK (PAR) REPORT =====
    @GetMapping("/par")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<SASRAComplianceReportService.PARReport>> getPARReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.PARReport report = sasraReportService.generatePARReport(asAtDate);
        return ResponseEntity.ok(ApiResponse.success("PAR Report generated successfully", report));
    }

    @GetMapping("/par/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportPARExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.PARReport report = sasraReportService.generatePARReport(asAtDate);
        byte[] excelFile = reportExportService.exportPARToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=par_report_" + asAtDate + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/par/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportPARPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.PARReport report = sasraReportService.generatePARReport(asAtDate);
        byte[] pdfFile = reportExportService.exportPARToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=par_report_" + asAtDate + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== CAPITAL ADEQUACY REPORT =====
    @GetMapping("/capital-adequacy")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<SASRAComplianceReportService.CapitalAdequacyReport>> getCapitalAdequacyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.CapitalAdequacyReport report = sasraReportService.generateCapitalAdequacyReport(asAtDate);
        return ResponseEntity.ok(ApiResponse.success("Capital Adequacy Report generated successfully", report));
    }

    @GetMapping("/capital-adequacy/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportCapitalAdequacyExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.CapitalAdequacyReport report = sasraReportService.generateCapitalAdequacyReport(asAtDate);
        byte[] excelFile = reportExportService.exportCapitalAdequacyToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=capital_adequacy_" + asAtDate + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/capital-adequacy/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportCapitalAdequacyPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.CapitalAdequacyReport report = sasraReportService.generateCapitalAdequacyReport(asAtDate);
        byte[] pdfFile = reportExportService.exportCapitalAdequacyToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=capital_adequacy_" + asAtDate + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== PROVISION FOR BAD DEBTS REPORT =====
    @GetMapping("/provision-bad-debts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<SASRAComplianceReportService.ProvisionForBadDebtsReport>> getProvisionForBadDebtsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.ProvisionForBadDebtsReport report = sasraReportService.generateProvisionForBadDebtsReport(asAtDate);
        return ResponseEntity.ok(ApiResponse.success("Provision for Bad Debts Report generated successfully", report));
    }

    @GetMapping("/provision-bad-debts/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportProvisionForBadDebtsExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.ProvisionForBadDebtsReport report = sasraReportService.generateProvisionForBadDebtsReport(asAtDate);
        byte[] excelFile = reportExportService.exportProvisionForBadDebtsToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=provision_bad_debts_" + asAtDate + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/provision-bad-debts/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportProvisionForBadDebtsPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.ProvisionForBadDebtsReport report = sasraReportService.generateProvisionForBadDebtsReport(asAtDate);
        byte[] pdfFile = reportExportService.exportProvisionForBadDebtsToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=provision_bad_debts_" + asAtDate + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    // ===== SASRA COMPLIANCE REPORT (COMPOSITE) =====
    @GetMapping("/compliance")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<SASRAComplianceReportService.SASRAComplianceReport>> getSASRAComplianceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.SASRAComplianceReport report = sasraReportService.generateSASRAComplianceReport(asAtDate);
        return ResponseEntity.ok(ApiResponse.success("SASRA Compliance Report generated successfully", report));
    }

    @GetMapping("/compliance/export/excel")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportSASRAComplianceExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.SASRAComplianceReport report = sasraReportService.generateSASRAComplianceReport(asAtDate);
        byte[] excelFile = reportExportService.exportSASRAComplianceToExcel(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sasra_compliance_" + asAtDate + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @GetMapping("/compliance/export/pdf")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_TREASURER')")
    public ResponseEntity<byte[]> exportSASRACompliancePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asAtDate) throws Exception {
        
        if (asAtDate == null) {
            asAtDate = LocalDate.now();
        }
        
        SASRAComplianceReportService.SASRAComplianceReport report = sasraReportService.generateSASRAComplianceReport(asAtDate);
        byte[] pdfFile = reportExportService.exportSASRAComplianceToPdf(report);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sasra_compliance_" + asAtDate + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }
}
