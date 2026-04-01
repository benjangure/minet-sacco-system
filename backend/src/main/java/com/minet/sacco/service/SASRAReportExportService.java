package com.minet.sacco.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SASRA Report Export Service
 * Handles Excel and PDF export for SASRA compliance reports
 */
@Service
public class SASRAReportExportService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String COMPANY_NAME = "MINET SACCO";

    /**
     * Export PAR Report to Excel
     */
    public byte[] exportPARToExcel(SASRAComplianceReportService.PARReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PAR Report");
            
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Portfolio At Risk (PAR) Report");
            headerRow.createCell(1).setCellValue("As at: " + report.getAsAtDate());
            
            rowNum++; // Blank row
            
            // Summary
            Row summaryHeader = sheet.createRow(rowNum++);
            summaryHeader.createCell(0).setCellValue("SUMMARY");
            
            Row totalLoansRow = sheet.createRow(rowNum++);
            totalLoansRow.createCell(0).setCellValue("Total Loans:");
            totalLoansRow.createCell(1).setCellValue(report.getTotalLoans());
            
            Row totalPortfolioRow = sheet.createRow(rowNum++);
            totalPortfolioRow.createCell(0).setCellValue("Total Portfolio:");
            totalPortfolioRow.createCell(1).setCellValue(report.getTotalPortfolio().doubleValue());
            
            rowNum++; // Blank row
            
            Row par30Row = sheet.createRow(rowNum++);
            par30Row.createCell(0).setCellValue("PAR 30 Amount:");
            par30Row.createCell(1).setCellValue(report.getPar30Amount().doubleValue());
            
            Row par30RatioRow = sheet.createRow(rowNum++);
            par30RatioRow.createCell(0).setCellValue("PAR 30 Ratio (%):");
            par30RatioRow.createCell(1).setCellValue(report.getPar30Ratio().doubleValue());
            
            Row par30CompliantRow = sheet.createRow(rowNum++);
            par30CompliantRow.createCell(0).setCellValue("PAR 30 Compliant (< 5%):");
            par30CompliantRow.createCell(1).setCellValue(report.isPar30Compliant() ? "YES" : "NO");
            
            rowNum++; // Blank row
            
            Row par90Row = sheet.createRow(rowNum++);
            par90Row.createCell(0).setCellValue("PAR 90 Amount:");
            par90Row.createCell(1).setCellValue(report.getPar90Amount().doubleValue());
            
            Row par90RatioRow = sheet.createRow(rowNum++);
            par90RatioRow.createCell(0).setCellValue("PAR 90 Ratio (%):");
            par90RatioRow.createCell(1).setCellValue(report.getPar90Ratio().doubleValue());
            
            Row par90CompliantRow = sheet.createRow(rowNum++);
            par90CompliantRow.createCell(0).setCellValue("PAR 90 Compliant (< 2%):");
            par90CompliantRow.createCell(1).setCellValue(report.isPar90Compliant() ? "YES" : "NO");
            
            rowNum++; // Blank row
            
            Row complianceRow = sheet.createRow(rowNum++);
            complianceRow.createCell(0).setCellValue("Overall Compliance Status:");
            complianceRow.createCell(1).setCellValue(report.getComplianceStatus());
            
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export PAR Report to PDF
     */
    public byte[] exportPARToPdf(SASRAComplianceReportService.PARReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            addReportHeader(document, "PORTFOLIO AT RISK (PAR) REPORT");
            document.add(new Paragraph("As at: " + report.getAsAtDate()).setFontSize(11));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER)).setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            Table table = new Table(2);
            table.setWidth(400);
            addSummaryRow(table, "Total Loans:", String.valueOf(report.getTotalLoans()));
            addSummaryRow(table, "Total Portfolio:", formatCurrency(report.getTotalPortfolio()));
            addSummaryRow(table, "PAR 30 Amount:", formatCurrency(report.getPar30Amount()));
            addSummaryRow(table, "PAR 30 Ratio (%):", report.getPar30Ratio().setScale(2, java.math.RoundingMode.HALF_UP).toString());
            addSummaryRow(table, "PAR 30 Compliant (< 5%):", report.isPar30Compliant() ? "YES ✓" : "NO ✗");
            addSummaryRow(table, "PAR 90 Amount:", formatCurrency(report.getPar90Amount()));
            addSummaryRow(table, "PAR 90 Ratio (%):", report.getPar90Ratio().setScale(2, java.math.RoundingMode.HALF_UP).toString());
            addSummaryRow(table, "PAR 90 Compliant (< 2%):", report.isPar90Compliant() ? "YES ✓" : "NO ✗");
            addSummaryRow(table, "Overall Status:", report.getComplianceStatus());
            
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Capital Adequacy Report to Excel
     */
    public byte[] exportCapitalAdequacyToExcel(SASRAComplianceReportService.CapitalAdequacyReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Capital Adequacy");
            
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Capital Adequacy Report");
            headerRow.createCell(1).setCellValue("As at: " + report.getAsAtDate());
            
            rowNum++; // Blank row
            
            Row totalAssetsRow = sheet.createRow(rowNum++);
            totalAssetsRow.createCell(0).setCellValue("Total Assets:");
            totalAssetsRow.createCell(1).setCellValue(report.getTotalAssets().doubleValue());
            
            rowNum++; // Blank row
            
            Row coreCapitalRow = sheet.createRow(rowNum++);
            coreCapitalRow.createCell(0).setCellValue("Core Capital:");
            coreCapitalRow.createCell(1).setCellValue(report.getCoreCapital().doubleValue());
            
            Row coreCapitalRatioRow = sheet.createRow(rowNum++);
            coreCapitalRatioRow.createCell(0).setCellValue("Core Capital Ratio (%):");
            coreCapitalRatioRow.createCell(1).setCellValue(report.getCoreCapitalRatio().doubleValue());
            
            Row coreCapitalCompliantRow = sheet.createRow(rowNum++);
            coreCapitalCompliantRow.createCell(0).setCellValue("Core Capital Compliant (≥ 10%):");
            coreCapitalCompliantRow.createCell(1).setCellValue(report.isCoreCapitalCompliant() ? "YES" : "NO");
            
            rowNum++; // Blank row
            
            Row institutionalCapitalRow = sheet.createRow(rowNum++);
            institutionalCapitalRow.createCell(0).setCellValue("Institutional Capital:");
            institutionalCapitalRow.createCell(1).setCellValue(report.getInstitutionalCapital().doubleValue());
            
            Row institutionalCapitalRatioRow = sheet.createRow(rowNum++);
            institutionalCapitalRatioRow.createCell(0).setCellValue("Institutional Capital Ratio (%):");
            institutionalCapitalRatioRow.createCell(1).setCellValue(report.getInstitutionalCapitalRatio().doubleValue());
            
            Row institutionalCapitalCompliantRow = sheet.createRow(rowNum++);
            institutionalCapitalCompliantRow.createCell(0).setCellValue("Institutional Capital Compliant (≥ 8%):");
            institutionalCapitalCompliantRow.createCell(1).setCellValue(report.isInstitutionalCapitalCompliant() ? "YES" : "NO");
            
            rowNum++; // Blank row
            
            Row complianceRow = sheet.createRow(rowNum++);
            complianceRow.createCell(0).setCellValue("Overall Compliance Status:");
            complianceRow.createCell(1).setCellValue(report.getComplianceStatus());
            
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Capital Adequacy Report to PDF
     */
    public byte[] exportCapitalAdequacyToPdf(SASRAComplianceReportService.CapitalAdequacyReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            addReportHeader(document, "CAPITAL ADEQUACY REPORT");
            document.add(new Paragraph("As at: " + report.getAsAtDate()).setFontSize(11));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER)).setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            Table table = new Table(2);
            table.setWidth(400);
            addSummaryRow(table, "Total Assets:", formatCurrency(report.getTotalAssets()));
            addSummaryRow(table, "Core Capital:", formatCurrency(report.getCoreCapital()));
            addSummaryRow(table, "Core Capital Ratio (%):", report.getCoreCapitalRatio().setScale(2, java.math.RoundingMode.HALF_UP).toString());
            addSummaryRow(table, "Core Capital Compliant (≥ 10%):", report.isCoreCapitalCompliant() ? "YES ✓" : "NO ✗");
            addSummaryRow(table, "Institutional Capital:", formatCurrency(report.getInstitutionalCapital()));
            addSummaryRow(table, "Institutional Capital Ratio (%):", report.getInstitutionalCapitalRatio().setScale(2, java.math.RoundingMode.HALF_UP).toString());
            addSummaryRow(table, "Institutional Capital Compliant (≥ 8%):", report.isInstitutionalCapitalCompliant() ? "YES ✓" : "NO ✗");
            addSummaryRow(table, "Overall Status:", report.getComplianceStatus());
            
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Provision for Bad Debts Report to Excel
     */
    public byte[] exportProvisionForBadDebtsToExcel(SASRAComplianceReportService.ProvisionForBadDebtsReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Provision");
            
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Provision for Bad Debts Report");
            headerRow.createCell(1).setCellValue("As at: " + report.getAsAtDate());
            
            rowNum++; // Blank row
            
            Row currentLoansRow = sheet.createRow(rowNum++);
            currentLoansRow.createCell(0).setCellValue("Current Loans (1% provision):");
            currentLoansRow.createCell(1).setCellValue(report.getCurrentLoansCount());
            currentLoansRow.createCell(2).setCellValue(report.getCurrentLoansProvision().doubleValue());
            
            Row overdue1to3Row = sheet.createRow(rowNum++);
            overdue1to3Row.createCell(0).setCellValue("Overdue 1-3 Months (25% provision):");
            overdue1to3Row.createCell(1).setCellValue(report.getOverdue1to3Count());
            overdue1to3Row.createCell(2).setCellValue(report.getOverdue1to3Provision().doubleValue());
            
            Row overdue3to12Row = sheet.createRow(rowNum++);
            overdue3to12Row.createCell(0).setCellValue("Overdue 3-12 Months (50% provision):");
            overdue3to12Row.createCell(1).setCellValue(report.getOverdue3to12Count());
            overdue3to12Row.createCell(2).setCellValue(report.getOverdue3to12Provision().doubleValue());
            
            Row overdue12PlusRow = sheet.createRow(rowNum++);
            overdue12PlusRow.createCell(0).setCellValue("Overdue 12+ Months (100% provision):");
            overdue12PlusRow.createCell(1).setCellValue(report.getOverdue12PlusCount());
            overdue12PlusRow.createCell(2).setCellValue(report.getOverdue12PlusProvision().doubleValue());
            
            rowNum++; // Blank row
            
            Row totalProvisionRow = sheet.createRow(rowNum++);
            totalProvisionRow.createCell(0).setCellValue("TOTAL PROVISION:");
            totalProvisionRow.createCell(2).setCellValue(report.getTotalProvision().doubleValue());
            
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Provision for Bad Debts Report to PDF
     */
    public byte[] exportProvisionForBadDebtsToPdf(SASRAComplianceReportService.ProvisionForBadDebtsReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            addReportHeader(document, "PROVISION FOR BAD DEBTS REPORT");
            document.add(new Paragraph("As at: " + report.getAsAtDate()).setFontSize(11));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER)).setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            Table table = new Table(3);
            table.setWidth(500);
            addHeaderCell(table, "Category");
            addHeaderCell(table, "Count");
            addHeaderCell(table, "Provision");
            
            addSummaryRow(table, "Current Loans (1%)", String.valueOf(report.getCurrentLoansCount()), formatCurrency(report.getCurrentLoansProvision()));
            addSummaryRow(table, "Overdue 1-3 Months (25%)", String.valueOf(report.getOverdue1to3Count()), formatCurrency(report.getOverdue1to3Provision()));
            addSummaryRow(table, "Overdue 3-12 Months (50%)", String.valueOf(report.getOverdue3to12Count()), formatCurrency(report.getOverdue3to12Provision()));
            addSummaryRow(table, "Overdue 12+ Months (100%)", String.valueOf(report.getOverdue12PlusCount()), formatCurrency(report.getOverdue12PlusProvision()));
            
            document.add(table);
            document.add(new Paragraph(""));
            
            Table totalTable = new Table(2);
            totalTable.setWidth(300);
            addSummaryRow(totalTable, "TOTAL PROVISION:", formatCurrency(report.getTotalProvision()));
            document.add(totalTable);
            
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export SASRA Compliance Report to Excel
     */
    public byte[] exportSASRAComplianceToExcel(SASRAComplianceReportService.SASRAComplianceReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("SASRA Compliance");
            
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("SASRA Regulatory Compliance Report");
            headerRow.createCell(1).setCellValue("As at: " + report.getAsAtDate());
            
            rowNum++; // Blank row
            
            Row par30Row = sheet.createRow(rowNum++);
            par30Row.createCell(0).setCellValue("PAR 30 Ratio (%):");
            par30Row.createCell(1).setCellValue(report.getPar30Ratio().doubleValue());
            par30Row.createCell(2).setCellValue("Target: < 5%");
            par30Row.createCell(3).setCellValue(report.isPar30Compliant() ? "PASS" : "FAIL");
            
            Row par90Row = sheet.createRow(rowNum++);
            par90Row.createCell(0).setCellValue("PAR 90 Ratio (%):");
            par90Row.createCell(1).setCellValue(report.getPar90Ratio().doubleValue());
            par90Row.createCell(2).setCellValue("Target: < 2%");
            par90Row.createCell(3).setCellValue(report.isPar90Compliant() ? "PASS" : "FAIL");
            
            rowNum++; // Blank row
            
            Row coreCapitalRow = sheet.createRow(rowNum++);
            coreCapitalRow.createCell(0).setCellValue("Core Capital Ratio (%):");
            coreCapitalRow.createCell(1).setCellValue(report.getCoreCapitalRatio().doubleValue());
            coreCapitalRow.createCell(2).setCellValue("Target: ≥ 10%");
            coreCapitalRow.createCell(3).setCellValue(report.isCoreCapitalCompliant() ? "PASS" : "FAIL");
            
            Row institutionalCapitalRow = sheet.createRow(rowNum++);
            institutionalCapitalRow.createCell(0).setCellValue("Institutional Capital Ratio (%):");
            institutionalCapitalRow.createCell(1).setCellValue(report.getInstitutionalCapitalRatio().doubleValue());
            institutionalCapitalRow.createCell(2).setCellValue("Target: ≥ 8%");
            institutionalCapitalRow.createCell(3).setCellValue(report.isInstitutionalCapitalCompliant() ? "PASS" : "FAIL");
            
            rowNum++; // Blank row
            
            Row liquidityRow = sheet.createRow(rowNum++);
            liquidityRow.createCell(0).setCellValue("Liquidity Ratio (%):");
            liquidityRow.createCell(1).setCellValue(report.getLiquidityRatio().doubleValue());
            liquidityRow.createCell(2).setCellValue("Target: ≥ 20%");
            liquidityRow.createCell(3).setCellValue(report.isLiquidityCompliant() ? "PASS" : "FAIL");
            
            Row savingsToLoansRow = sheet.createRow(rowNum++);
            savingsToLoansRow.createCell(0).setCellValue("Savings to Loans Ratio:");
            savingsToLoansRow.createCell(1).setCellValue(report.getSavingsToLoansRatio().doubleValue());
            savingsToLoansRow.createCell(2).setCellValue("Target: ≥ 1.0");
            savingsToLoansRow.createCell(3).setCellValue(report.isSavingsToLoansCompliant() ? "PASS" : "FAIL");
            
            rowNum++; // Blank row
            
            Row overallRow = sheet.createRow(rowNum++);
            overallRow.createCell(0).setCellValue("OVERALL COMPLIANCE STATUS:");
            overallRow.createCell(1).setCellValue(report.getOverallComplianceStatus());
            
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export SASRA Compliance Report to PDF
     */
    public byte[] exportSASRAComplianceToPdf(SASRAComplianceReportService.SASRAComplianceReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            addReportHeader(document, "SASRA REGULATORY COMPLIANCE REPORT");
            document.add(new Paragraph("As at: " + report.getAsAtDate()).setFontSize(11));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER)).setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            Table table = new Table(4);
            table.setWidth(100);
            addHeaderCell(table, "Metric");
            addHeaderCell(table, "Value");
            addHeaderCell(table, "Target");
            addHeaderCell(table, "Status");
            
            addComplianceRow(table, "PAR 30 Ratio (%)", report.getPar30Ratio().setScale(2, java.math.RoundingMode.HALF_UP).toString(), "< 5%", report.isPar30Compliant());
            addComplianceRow(table, "PAR 90 Ratio (%)", report.getPar90Ratio().setScale(2, java.math.RoundingMode.HALF_UP).toString(), "< 2%", report.isPar90Compliant());
            addComplianceRow(table, "Core Capital Ratio (%)", report.getCoreCapitalRatio().setScale(2, java.math.RoundingMode.HALF_UP).toString(), "≥ 10%", report.isCoreCapitalCompliant());
            addComplianceRow(table, "Institutional Capital (%)", report.getInstitutionalCapitalRatio().setScale(2, java.math.RoundingMode.HALF_UP).toString(), "≥ 8%", report.isInstitutionalCapitalCompliant());
            addComplianceRow(table, "Liquidity Ratio (%)", report.getLiquidityRatio().setScale(2, java.math.RoundingMode.HALF_UP).toString(), "≥ 20%", report.isLiquidityCompliant());
            addComplianceRow(table, "Savings to Loans", report.getSavingsToLoansRatio().setScale(2, java.math.RoundingMode.HALF_UP).toString(), "≥ 1.0", report.isSavingsToLoansCompliant());
            
            document.add(table);
            document.add(new Paragraph(""));
            
            Paragraph overallStatus = new Paragraph("OVERALL COMPLIANCE STATUS: " + report.getOverallComplianceStatus())
                    .setBold().setFontSize(12);
            if ("COMPLIANT".equals(report.getOverallComplianceStatus())) {
                overallStatus.setFontColor(ColorConstants.GREEN);
            } else {
                overallStatus.setFontColor(ColorConstants.RED);
            }
            document.add(overallStatus);
            
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    // Helper methods
    private void addReportHeader(Document document, String title) {
        document.add(new Paragraph(COMPANY_NAME).setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(title).setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(""));
    }

    private void addHeaderCell(Table table, String text) {
        Cell cell = new Cell();
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell.add(new Paragraph(text).setBold().setFontSize(10));
        table.addCell(cell);
    }

    private void addSummaryRow(Table table, String label, String value) {
        Cell labelCell = new Cell();
        labelCell.add(new Paragraph(label).setFontSize(10));
        table.addCell(labelCell);
        
        Cell valueCell = new Cell();
        valueCell.add(new Paragraph(value).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(valueCell);
    }

    private void addSummaryRow(Table table, String col1, String col2, String col3) {
        table.addCell(new Cell().add(new Paragraph(col1).setFontSize(9)));
        table.addCell(new Cell().add(new Paragraph(col2).setFontSize(9).setTextAlignment(TextAlignment.CENTER)));
        table.addCell(new Cell().add(new Paragraph(col3).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
    }

    private void addComplianceRow(Table table, String metric, String value, String target, boolean compliant) {
        table.addCell(new Cell().add(new Paragraph(metric).setFontSize(9)));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
        table.addCell(new Cell().add(new Paragraph(target).setFontSize(9).setTextAlignment(TextAlignment.CENTER)));
        
        Cell statusCell = new Cell();
        Paragraph statusPara = new Paragraph(compliant ? "PASS ✓" : "FAIL ✗").setFontSize(9).setTextAlignment(TextAlignment.CENTER);
        if (compliant) {
            statusPara.setFontColor(ColorConstants.GREEN);
        } else {
            statusPara.setFontColor(ColorConstants.RED);
        }
        statusCell.add(statusPara);
        table.addCell(statusCell);
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}
