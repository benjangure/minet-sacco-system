package com.minet.sacco.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.minet.sacco.dto.ProfitLossReportDTO;
import com.minet.sacco.dto.WithdrawalMonitoringReportDTO;
import com.minet.sacco.dto.ExitedMemberLoanDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportExportService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String COMPANY_NAME = "MINET SACCO";

    /**
     * Export Cashbook to Excel
     */
    public byte[] exportCashbookToExcel(ReportsService.CashbookReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cashbook");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Cashbook Report");
            String period = "N/A";
            if (report.getStartDate() != null && report.getEndDate() != null) {
                period = "Period: " + report.getStartDate() + " to " + report.getEndDate();
            }
            headerRow.createCell(1).setCellValue(period);
            
            // Summary
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Opening Balance:");
            r2.createCell(1).setCellValue(toDouble(report.getOpeningBalance()));
            
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("Total Receipts (Deposits + Repayments):");
            r3.createCell(1).setCellValue(toDouble(report.getTotalReceipts()));
            
            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("Total Payments (Withdrawals + Disbursements):");
            r4.createCell(1).setCellValue(toDouble(report.getTotalPayments()));
            
            Row r5 = sheet.createRow(5);
            r5.createCell(0).setCellValue("Closing Balance:");
            r5.createCell(1).setCellValue(toDouble(report.getClosingBalance()));
            
            Row r6 = sheet.createRow(6);
            r6.createCell(0).setCellValue("--- Breakdown ---");
            
            Row r7 = sheet.createRow(7);
            r7.createCell(0).setCellValue("  Deposits:");
            r7.createCell(1).setCellValue(toDouble(report.getTotalDeposits()));
            
            Row r8 = sheet.createRow(8);
            r8.createCell(0).setCellValue("  Loan Repayments:");
            r8.createCell(1).setCellValue(toDouble(report.getTotalRepayments()));
            
            Row r9 = sheet.createRow(9);
            r9.createCell(0).setCellValue("  Withdrawals:");
            r9.createCell(1).setCellValue(toDouble(report.getTotalWithdrawals()));
            
            Row r10 = sheet.createRow(10);
            r10.createCell(0).setCellValue("  Loan Disbursements:");
            r10.createCell(1).setCellValue(toDouble(report.getTotalDisbursements()));
            
            // Column headers
            Row colHeaderRow = sheet.createRow(12);
            colHeaderRow.createCell(0).setCellValue("Date");
            colHeaderRow.createCell(1).setCellValue("Type");
            colHeaderRow.createCell(2).setCellValue("Member");
            colHeaderRow.createCell(3).setCellValue("Account");
            colHeaderRow.createCell(4).setCellValue("Amount");
            colHeaderRow.createCell(5).setCellValue("Description");
            
            // Data rows
            int rowNum = 13;
            if (report.getEntries() != null) {
                for (ReportsService.CashbookEntry entry : report.getEntries()) {
                    if (entry != null) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(entry.getDate() != null ? entry.getDate().toString() : "");
                        row.createCell(1).setCellValue(entry.getTransactionType() != null ? entry.getTransactionType() : "");
                        row.createCell(2).setCellValue(entry.getMemberNumber() != null ? entry.getMemberNumber() : "");
                        row.createCell(3).setCellValue(entry.getAccountType() != null ? entry.getAccountType() : "");
                        row.createCell(4).setCellValue(toDouble(entry.getAmount()));
                        row.createCell(5).setCellValue(entry.getDescription() != null ? entry.getDescription() : "");
                    }
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Cashbook to PDF using iText
     */
    public byte[] exportCashbookToPdf(ReportsService.CashbookReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "CASHBOOK REPORT");
            
            // Period info
            LocalDate startDate = report.getStartDate();
            LocalDate endDate = report.getEndDate();
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }
            if (startDate != null && endDate != null) {
                document.add(new Paragraph("Period: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER))
                    .setFontSize(11));
            }
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            // Summary section
            document.add(new Paragraph("SUMMARY").setBold().setFontSize(12));
            Table summaryTable = new Table(2);
            summaryTable.setWidth(300);
            addSummaryRow(summaryTable, "Opening Balance:", "KES " + formatCurrency(report.getOpeningBalance()));
            addSummaryRow(summaryTable, "Total Receipts (Deposits + Repayments):", "KES " + formatCurrency(report.getTotalReceipts()));
            addSummaryRow(summaryTable, "Total Payments (Withdrawals + Disbursements):", "KES " + formatCurrency(report.getTotalPayments()));
            addSummaryRow(summaryTable, "Closing Balance:", "KES " + formatCurrency(report.getClosingBalance()));
            document.add(summaryTable);
            document.add(new Paragraph(""));
            
            document.add(new Paragraph("BREAKDOWN").setBold().setFontSize(11));
            Table breakdownTable = new Table(2);
            breakdownTable.setWidth(300);
            addSummaryRow(breakdownTable, "  Deposits:", "KES " + formatCurrency(report.getTotalDeposits()));
            addSummaryRow(breakdownTable, "  Loan Repayments:", "KES " + formatCurrency(report.getTotalRepayments()));
            addSummaryRow(breakdownTable, "  Withdrawals:", "KES " + formatCurrency(report.getTotalWithdrawals()));
            addSummaryRow(breakdownTable, "  Loan Disbursements:", "KES " + formatCurrency(report.getTotalDisbursements()));
            document.add(breakdownTable);
            document.add(new Paragraph(""));
            
            // Transactions table
            document.add(new Paragraph("TRANSACTIONS").setBold().setFontSize(12));
            Table table = new Table(6);
            table.setWidth(100);
            addHeaderCell(table, "Date");
            addHeaderCell(table, "Type");
            addHeaderCell(table, "Member");
            addHeaderCell(table, "Account");
            addHeaderCell(table, "Amount");
            addHeaderCell(table, "Description");
            
            List<ReportsService.CashbookEntry> entries = report.getEntries();
            if (entries != null) {
                for (ReportsService.CashbookEntry entry : entries) {
                    if (entry != null) {
                        table.addCell(new Cell().add(new Paragraph(entry.getDate() != null ? entry.getDate().toString() : "").setFontSize(9)));
                        table.addCell(new Cell().add(new Paragraph(entry.getTransactionType() != null ? entry.getTransactionType() : "").setFontSize(9)));
                        table.addCell(new Cell().add(new Paragraph(entry.getMemberNumber() != null ? entry.getMemberNumber() : "").setFontSize(9)));
                        table.addCell(new Cell().add(new Paragraph(entry.getAccountType() != null ? entry.getAccountType() : "").setFontSize(9)));
                        table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getAmount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                        table.addCell(new Cell().add(new Paragraph(entry.getDescription() != null ? entry.getDescription() : "").setFontSize(9)));
                    }
                }
            }
            
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Trial Balance to Excel
     */
    public byte[] exportTrialBalanceToExcel(ReportsService.TrialBalanceReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Trial Balance");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Trial Balance Report");
            headerRow.createCell(1).setCellValue("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER));
            
            // Column headers
            Row colHeaderRow = sheet.createRow(2);
            colHeaderRow.createCell(0).setCellValue("Member");
            colHeaderRow.createCell(1).setCellValue("Account Type");
            colHeaderRow.createCell(2).setCellValue("Debit");
            colHeaderRow.createCell(3).setCellValue("Credit");
            
            // Data rows
            int rowNum = 3;
            for (ReportsService.TrialBalanceEntry entry : report.getEntries()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getMemberNumber());
                row.createCell(1).setCellValue(entry.getAccountType());
                row.createCell(2).setCellValue(entry.getDebit().doubleValue());
                row.createCell(3).setCellValue(entry.getCredit().doubleValue());
            }
            
            // Totals
            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(0).setCellValue("TOTALS");
            totalRow.createCell(2).setCellValue(report.getTotalDebits().doubleValue());
            totalRow.createCell(3).setCellValue(report.getTotalCredits().doubleValue());
            
            // Balance status
            Row balanceRow = sheet.createRow(rowNum + 3);
            balanceRow.createCell(0).setCellValue("Balanced: " + (report.getIsBalanced() ? "YES" : "NO"));
            
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Trial Balance to PDF using iText
     */
    public byte[] exportTrialBalanceToPdf(ReportsService.TrialBalanceReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "TRIAL BALANCE REPORT");
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            // Table
            Table table = new Table(4);
            table.setWidth(100);
            addHeaderCell(table, "Member");
            addHeaderCell(table, "Account Type");
            addHeaderCell(table, "Debit");
            addHeaderCell(table, "Credit");
            
            for (ReportsService.TrialBalanceEntry entry : report.getEntries()) {
                table.addCell(new Cell().add(new Paragraph(entry.getMemberNumber()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(entry.getAccountType()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getDebit())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getCredit())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
            }
            
            // Totals row
            addTotalRow(table, "TOTALS", "", formatCurrency(report.getTotalDebits()), formatCurrency(report.getTotalCredits()));
            
            document.add(table);
            document.add(new Paragraph(""));
            document.add(new Paragraph("Balanced: " + (report.getIsBalanced() ? "YES" : "NO")).setBold());
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Balance Sheet to Excel
     */
    public byte[] exportBalanceSheetToExcel(ReportsService.BalanceSheetReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance Sheet");
            
            int rowNum = 0;
            
            // Header
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("BALANCE SHEET");
            
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("As at: " + LocalDate.now());
            
            rowNum++; // Blank row
            
            // Assets
            Row assetsHeader = sheet.createRow(rowNum++);
            assetsHeader.createCell(0).setCellValue("ASSETS");
            
            Row loansRow = sheet.createRow(rowNum++);
            loansRow.createCell(0).setCellValue("Loans Outstanding");
            loansRow.createCell(1).setCellValue(report.getTotalAssets().doubleValue());
            
            rowNum++; // Blank row
            
            // Liabilities
            Row liabilitiesHeader = sheet.createRow(rowNum++);
            liabilitiesHeader.createCell(0).setCellValue("LIABILITIES");
            
            Row savingsRow = sheet.createRow(rowNum++);
            savingsRow.createCell(0).setCellValue("Member Savings");
            savingsRow.createCell(1).setCellValue(report.getTotalSavings().doubleValue());
            
            Row sharesRow = sheet.createRow(rowNum++);
            sharesRow.createCell(0).setCellValue("Member Shares");
            sharesRow.createCell(1).setCellValue(report.getTotalShares().doubleValue());
            
            Row totalLiabilitiesRow = sheet.createRow(rowNum++);
            totalLiabilitiesRow.createCell(0).setCellValue("Total Liabilities");
            totalLiabilitiesRow.createCell(1).setCellValue(report.getTotalLiabilities().doubleValue());
            
            rowNum++; // Blank row
            
            // Equity
            Row equityHeader = sheet.createRow(rowNum++);
            equityHeader.createCell(0).setCellValue("EQUITY");
            
            Row equityRow = sheet.createRow(rowNum++);
            equityRow.createCell(0).setCellValue("Equity");
            equityRow.createCell(1).setCellValue(report.getEquity().doubleValue());
            
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Balance Sheet to PDF using iText
     */
    public byte[] exportBalanceSheetToPdf(ReportsService.BalanceSheetReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "BALANCE SHEET");
            document.add(new Paragraph("As at: " + LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(11));
            document.add(new Paragraph(""));
            
            // Assets
            document.add(new Paragraph("ASSETS").setBold().setFontSize(12));
            Table assetsTable = new Table(2);
            assetsTable.setWidth(300);
            addSummaryRow(assetsTable, "Loans Outstanding:", "KES " + formatCurrency(report.getTotalAssets()));
            document.add(assetsTable);
            document.add(new Paragraph(""));
            
            // Liabilities
            document.add(new Paragraph("LIABILITIES").setBold().setFontSize(12));
            Table liabilitiesTable = new Table(2);
            liabilitiesTable.setWidth(300);
            addSummaryRow(liabilitiesTable, "Member Savings:", "KES " + formatCurrency(report.getTotalSavings()));
            addSummaryRow(liabilitiesTable, "Member Shares:", "KES " + formatCurrency(report.getTotalShares()));
            addSummaryRow(liabilitiesTable, "Total Liabilities:", "KES " + formatCurrency(report.getTotalLiabilities()));
            document.add(liabilitiesTable);
            document.add(new Paragraph(""));
            
            // Equity
            document.add(new Paragraph("EQUITY").setBold().setFontSize(12));
            Table equityTable = new Table(2);
            equityTable.setWidth(300);
            addSummaryRow(equityTable, "Equity:", "KES " + formatCurrency(report.getEquity()));
            document.add(equityTable);
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Loan Register to Excel
     */
    public byte[] exportLoanRegisterToExcel(ReportsService.LoanRegisterReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Loan Register");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Loan Register Report");
            
            // Column headers
            Row colHeaderRow = sheet.createRow(2);
            colHeaderRow.createCell(0).setCellValue("Loan Number");
            colHeaderRow.createCell(1).setCellValue("Member");
            colHeaderRow.createCell(2).setCellValue("Product");
            colHeaderRow.createCell(3).setCellValue("Amount");
            colHeaderRow.createCell(4).setCellValue("Interest Rate");
            colHeaderRow.createCell(5).setCellValue("Term (Months)");
            colHeaderRow.createCell(6).setCellValue("Monthly Payment");
            colHeaderRow.createCell(7).setCellValue("Status");
            colHeaderRow.createCell(8).setCellValue("Outstanding");
            
            // Data rows
            int rowNum = 3;
            for (ReportsService.LoanRegisterEntry entry : report.getEntries()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getLoanNumber());
                row.createCell(1).setCellValue(entry.getMemberNumber());
                row.createCell(2).setCellValue(entry.getLoanProduct());
                row.createCell(3).setCellValue(entry.getAmount().doubleValue());
                row.createCell(4).setCellValue(entry.getInterestRate().doubleValue());
                row.createCell(5).setCellValue(entry.getTermMonths());
                // Defensive: handle null monthlyRepayment from reducing balance loans
                row.createCell(6).setCellValue(entry.getMonthlyRepayment() != null ? entry.getMonthlyRepayment().doubleValue() : 0.0);
                row.createCell(7).setCellValue(entry.getStatus());
                row.createCell(8).setCellValue(entry.getOutstandingBalance().doubleValue());
            }
            
            // Totals
            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(0).setCellValue("TOTALS");
            totalRow.createCell(3).setCellValue(report.getTotalLoansIssued().doubleValue());
            totalRow.createCell(8).setCellValue(report.getTotalOutstanding().doubleValue());
            
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Loan Register to PDF using iText
     */
    public byte[] exportLoanRegisterToPdf(ReportsService.LoanRegisterReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "LOAN REGISTER REPORT");
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            // Table
            Table table = new Table(9);
            table.setWidth(100);
            addHeaderCell(table, "Loan #");
            addHeaderCell(table, "Member");
            addHeaderCell(table, "Product");
            addHeaderCell(table, "Amount");
            addHeaderCell(table, "Rate %");
            addHeaderCell(table, "Term");
            addHeaderCell(table, "Monthly");
            addHeaderCell(table, "Status");
            addHeaderCell(table, "Outstanding");
            
            for (ReportsService.LoanRegisterEntry entry : report.getEntries()) {
                table.addCell(new Cell().add(new Paragraph(entry.getLoanNumber()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(entry.getMemberNumber()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(entry.getLoanProduct()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getAmount())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
                table.addCell(new Cell().add(new Paragraph(entry.getInterestRate().toString()).setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getTermMonths())).setFontSize(8).setTextAlignment(TextAlignment.CENTER)));
                // Defensive: handle null monthlyRepayment from reducing balance loans
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getMonthlyRepayment() != null ? entry.getMonthlyRepayment() : BigDecimal.ZERO)).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
                table.addCell(new Cell().add(new Paragraph(entry.getStatus()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getOutstandingBalance())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
            }
            
            document.add(table);
            document.add(new Paragraph(""));
            
            Table summaryTable = new Table(2);
            summaryTable.setWidth(300);
            addSummaryRow(summaryTable, "Total Loans Issued:", "KES " + formatCurrency(report.getTotalLoansIssued()));
            addSummaryRow(summaryTable, "Total Outstanding:", "KES " + formatCurrency(report.getTotalOutstanding()));
            addSummaryRow(summaryTable, "Total Repaid:", "KES " + formatCurrency(report.getTotalRepaid()));
            document.add(summaryTable);
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Member Statement to Excel
     */
    public byte[] exportMemberStatementToExcel(ReportsService.MemberStatementReport report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Member Statement");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Member Statement");
            
            Row memberRow = sheet.createRow(1);
            memberRow.createCell(0).setCellValue("Member: " + report.getMemberName() + " (" + report.getMemberNumber() + ")");
            
            Row periodRow = sheet.createRow(2);
            periodRow.createCell(0).setCellValue("Period: " + report.getStartDate() + " to " + report.getEndDate());
            
            // Column headers
            Row colHeaderRow = sheet.createRow(4);
            colHeaderRow.createCell(0).setCellValue("Date");
            colHeaderRow.createCell(1).setCellValue("Account");
            colHeaderRow.createCell(2).setCellValue("Type");
            colHeaderRow.createCell(3).setCellValue("Amount");
            colHeaderRow.createCell(4).setCellValue("Description");
            
            // Data rows
            int rowNum = 5;
            for (ReportsService.MemberStatementEntry entry : report.getEntries()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getDate().toString());
                row.createCell(1).setCellValue(entry.getAccountType());
                row.createCell(2).setCellValue(entry.getTransactionType());
                row.createCell(3).setCellValue(entry.getAmount().doubleValue());
                row.createCell(4).setCellValue(entry.getDescription());
            }
            
            // Totals
            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(0).setCellValue("TOTALS");
            totalRow.createCell(3).setCellValue(report.getTotalDeposits().add(report.getTotalWithdrawals()).doubleValue());
            
            // Current Balances
            Row balanceHeaderRow = sheet.createRow(rowNum + 3);
            balanceHeaderRow.createCell(0).setCellValue("Current Balances");
            
            int balanceRowNum = rowNum + 4;
            for (String accountType : report.getCurrentBalances().keySet()) {
                Row balanceRow = sheet.createRow(balanceRowNum++);
                balanceRow.createCell(0).setCellValue(accountType);
                balanceRow.createCell(1).setCellValue(report.getCurrentBalances().get(accountType).doubleValue());
            }
            
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Member Statement to PDF using iText
     */
    public byte[] exportMemberStatementToPdf(ReportsService.MemberStatementReport report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "MEMBER STATEMENT");
            document.add(new Paragraph("Member: " + report.getMemberName() + " (" + report.getMemberNumber() + ")")
                .setFontSize(11));
            
            LocalDate startDate = report.getStartDate();
            LocalDate endDate = report.getEndDate();
            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }
            document.add(new Paragraph("Period: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER))
                .setFontSize(11));
            document.add(new Paragraph(""));
            
            // Transactions table
            Table table = new Table(5);
            table.setWidth(100);
            addHeaderCell(table, "Date");
            addHeaderCell(table, "Account");
            addHeaderCell(table, "Type");
            addHeaderCell(table, "Amount");
            addHeaderCell(table, "Description");
            
            for (ReportsService.MemberStatementEntry entry : report.getEntries()) {
                table.addCell(new Cell().add(new Paragraph(entry.getDate().toString()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(entry.getAccountType()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(entry.getTransactionType()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getAmount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                table.addCell(new Cell().add(new Paragraph(entry.getDescription()).setFontSize(9)));
            }
            
            document.add(table);
            document.add(new Paragraph(""));
            
            // Current Balances
            document.add(new Paragraph("CURRENT BALANCES").setBold().setFontSize(12));
            Table balancesTable = new Table(2);
            balancesTable.setWidth(300);
            for (String accountType : report.getCurrentBalances().keySet()) {
                addSummaryRow(balancesTable, accountType + ":", "KES " + formatCurrency(report.getCurrentBalances().get(accountType)));
            }
            document.add(balancesTable);
            
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

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

    private void addTotalRow(Table table, String col1, String col2, String col3, String col4) {
        Cell cell1 = new Cell();
        cell1.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell1.add(new Paragraph(col1).setBold().setFontSize(9));
        table.addCell(cell1);
        
        Cell cell2 = new Cell();
        cell2.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell2.add(new Paragraph(col2).setBold().setFontSize(9));
        table.addCell(cell2);
        
        Cell cell3 = new Cell();
        cell3.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell3.add(new Paragraph(col3).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(cell3);
        
        Cell cell4 = new Cell();
        cell4.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell4.add(new Paragraph(col4).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(cell4);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%,.2f", amount);
    }

    private double toDouble(BigDecimal amount) {
        if (amount == null) {
            return 0.0;
        }
        return amount.doubleValue();
    }

    /**
     * Export Profit & Loss Report to Excel
     */
    public byte[] exportProfitLossToExcel(ProfitLossReportDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("P&L Report");
            
            // Title
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(COMPANY_NAME + " - PROFIT & LOSS STATEMENT");
            
            // Period
            Row periodRow = sheet.createRow(1);
            periodRow.createCell(0).setCellValue("Period: " + report.getPeriod().getStartDate() + " to " + report.getPeriod().getEndDate());
            
            // Empty row
            sheet.createRow(2);
            
            // Revenue Section
            int rowNum = 3;
            Row revenueHeaderRow = sheet.createRow(rowNum++);
            revenueHeaderRow.createCell(0).setCellValue("REVENUE");
            
            Row interestLoansRow = sheet.createRow(rowNum++);
            interestLoansRow.createCell(0).setCellValue("Interest from Loans");
            interestLoansRow.createCell(1).setCellValue(report.getRevenue().getInterestIncome().getFromLoans().doubleValue());
            
            Row interestSavingsRow = sheet.createRow(rowNum++);
            interestSavingsRow.createCell(0).setCellValue("Interest from Savings");
            interestSavingsRow.createCell(1).setCellValue(report.getRevenue().getInterestIncome().getFromSavings().doubleValue());
            
            Row totalInterestRow = sheet.createRow(rowNum++);
            totalInterestRow.createCell(0).setCellValue("Total Interest Income");
            totalInterestRow.createCell(1).setCellValue(report.getRevenue().getInterestIncome().getTotal().doubleValue());
            
            Row loanFeesRow = sheet.createRow(rowNum++);
            loanFeesRow.createCell(0).setCellValue("Loan Processing Fees");
            loanFeesRow.createCell(1).setCellValue(report.getRevenue().getFeesAndCharges().getLoanProcessingFees().doubleValue());
            
            Row accountFeesRow = sheet.createRow(rowNum++);
            accountFeesRow.createCell(0).setCellValue("Account Maintenance Fees");
            accountFeesRow.createCell(1).setCellValue(report.getRevenue().getFeesAndCharges().getAccountMaintenanceFees().doubleValue());
            
            Row otherFeesRow = sheet.createRow(rowNum++);
            otherFeesRow.createCell(0).setCellValue("Other Fees");
            otherFeesRow.createCell(1).setCellValue(report.getRevenue().getFeesAndCharges().getOtherFees().doubleValue());
            
            Row totalFeesRow = sheet.createRow(rowNum++);
            totalFeesRow.createCell(0).setCellValue("Total Fees & Charges");
            totalFeesRow.createCell(1).setCellValue(report.getRevenue().getFeesAndCharges().getTotal().doubleValue());
            
            Row otherIncomeRow = sheet.createRow(rowNum++);
            otherIncomeRow.createCell(0).setCellValue("Other Income");
            otherIncomeRow.createCell(1).setCellValue(report.getRevenue().getOtherIncome().doubleValue());
            
            Row totalRevenueRow = sheet.createRow(rowNum++);
            totalRevenueRow.createCell(0).setCellValue("TOTAL REVENUE");
            totalRevenueRow.createCell(1).setCellValue(report.getRevenue().getTotalRevenue().doubleValue());
            
            // Empty row
            sheet.createRow(rowNum++);
            
            // Expenses Section
            Row expenseHeaderRow = sheet.createRow(rowNum++);
            expenseHeaderRow.createCell(0).setCellValue("EXPENSES");
            
            Row salariesRow = sheet.createRow(rowNum++);
            salariesRow.createCell(0).setCellValue("Salaries");
            salariesRow.createCell(1).setCellValue(report.getExpenses().getOperatingExpenses().getSalaries().doubleValue());
            
            Row rentRow = sheet.createRow(rowNum++);
            rentRow.createCell(0).setCellValue("Rent");
            rentRow.createCell(1).setCellValue(report.getExpenses().getOperatingExpenses().getRent().doubleValue());
            
            Row utilitiesRow = sheet.createRow(rowNum++);
            utilitiesRow.createCell(0).setCellValue("Utilities");
            utilitiesRow.createCell(1).setCellValue(report.getExpenses().getOperatingExpenses().getUtilities().doubleValue());
            
            Row otherOpexRow = sheet.createRow(rowNum++);
            otherOpexRow.createCell(0).setCellValue("Other Operating Expenses");
            otherOpexRow.createCell(1).setCellValue(report.getExpenses().getOperatingExpenses().getOther().doubleValue());
            
            Row totalOpexRow = sheet.createRow(rowNum++);
            totalOpexRow.createCell(0).setCellValue("Total Operating Expenses");
            totalOpexRow.createCell(1).setCellValue(report.getExpenses().getOperatingExpenses().getTotal().doubleValue());
            
            Row doubtfulDebtsRow = sheet.createRow(rowNum++);
            doubtfulDebtsRow.createCell(0).setCellValue("Provision for Doubtful Debts");
            doubtfulDebtsRow.createCell(1).setCellValue(report.getExpenses().getLoanLossProvisions().getDoubtfulDebts().doubleValue());
            
            Row writeOffsRow = sheet.createRow(rowNum++);
            writeOffsRow.createCell(0).setCellValue("Write-offs");
            writeOffsRow.createCell(1).setCellValue(report.getExpenses().getLoanLossProvisions().getWriteOffs().doubleValue());
            
            Row totalProvisionsRow = sheet.createRow(rowNum++);
            totalProvisionsRow.createCell(0).setCellValue("Total Loan Loss Provisions");
            totalProvisionsRow.createCell(1).setCellValue(report.getExpenses().getLoanLossProvisions().getTotal().doubleValue());
            
            Row otherExpensesRow = sheet.createRow(rowNum++);
            otherExpensesRow.createCell(0).setCellValue("Other Expenses");
            otherExpensesRow.createCell(1).setCellValue(report.getExpenses().getOtherExpenses().doubleValue());
            
            Row totalExpensesRow = sheet.createRow(rowNum++);
            totalExpensesRow.createCell(0).setCellValue("TOTAL EXPENSES");
            totalExpensesRow.createCell(1).setCellValue(report.getExpenses().getTotalExpenses().doubleValue());
            
            // Empty row
            sheet.createRow(rowNum++);
            
            // Net Profit/Loss
            Row netProfitRow = sheet.createRow(rowNum++);
            netProfitRow.createCell(0).setCellValue("NET PROFIT/LOSS");
            netProfitRow.createCell(1).setCellValue(report.getNetProfitLoss().doubleValue());
            
            Row profitMarginRow = sheet.createRow(rowNum++);
            profitMarginRow.createCell(0).setCellValue("Profit Margin (%)");
            profitMarginRow.createCell(1).setCellValue(report.getProfitMargin().doubleValue());
            
            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Profit & Loss Report to PDF
     * NOTE: This method is incomplete and has been temporarily disabled
     */
    public byte[] exportProfitLossToPdf(ProfitLossReportDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header with company name
            addReportHeader(document, "PROFIT & LOSS STATEMENT");
            
            // Period
            document.add(new Paragraph("Period: " + report.getPeriod().getStartDate() + " to " + report.getPeriod().getEndDate())
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(10));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(9));
            document.add(new Paragraph(""));
            
            // Revenue Section
            document.add(new Paragraph("REVENUE").setBold().setFontSize(12));
            Table revenueTable = new Table(2);
            revenueTable.setWidth(500);
            addHeaderCell(revenueTable, "Description");
            addHeaderCell(revenueTable, "Amount (KES)");
            
            addSummaryRow(revenueTable, "Interest from Loans", formatCurrency(report.getRevenue().getInterestIncome().getFromLoans()));
            addSummaryRow(revenueTable, "Interest from Savings", formatCurrency(report.getRevenue().getInterestIncome().getFromSavings()));
            addSummaryRow(revenueTable, "Total Interest Income", formatCurrency(report.getRevenue().getInterestIncome().getTotal()));
            document.add(revenueTable);
            
            document.add(new Paragraph(""));
            Table feesTable = new Table(2);
            feesTable.setWidth(500);
            addHeaderCell(feesTable, "Description");
            addHeaderCell(feesTable, "Amount (KES)");
            
            addSummaryRow(feesTable, "Loan Processing Fees", formatCurrency(report.getRevenue().getFeesAndCharges().getLoanProcessingFees()));
            addSummaryRow(feesTable, "Account Maintenance Fees", formatCurrency(report.getRevenue().getFeesAndCharges().getAccountMaintenanceFees()));
            addSummaryRow(feesTable, "Other Fees", formatCurrency(report.getRevenue().getFeesAndCharges().getOtherFees()));
            addSummaryRow(feesTable, "Total Fees & Charges", formatCurrency(report.getRevenue().getFeesAndCharges().getTotal()));
            document.add(feesTable);
            
            document.add(new Paragraph(""));
            Table otherRevenueTable = new Table(2);
            otherRevenueTable.setWidth(500);
            addHeaderCell(otherRevenueTable, "Description");
            addHeaderCell(otherRevenueTable, "Amount (KES)");
            addSummaryRow(otherRevenueTable, "Other Income", formatCurrency(report.getRevenue().getOtherIncome()));
            addSummaryRow(otherRevenueTable, "TOTAL REVENUE", formatCurrency(report.getRevenue().getTotalRevenue()));
            document.add(otherRevenueTable);
            
            document.add(new Paragraph(""));
            document.add(new Paragraph(""));
            
            // Expenses Section
            document.add(new Paragraph("EXPENSES").setBold().setFontSize(12));
            Table operatingExpenseTable = new Table(2);
            operatingExpenseTable.setWidth(500);
            addHeaderCell(operatingExpenseTable, "Operating Expenses");
            addHeaderCell(operatingExpenseTable, "Amount (KES)");
            
            addSummaryRow(operatingExpenseTable, "Salaries", formatCurrency(report.getExpenses().getOperatingExpenses().getSalaries()));
            addSummaryRow(operatingExpenseTable, "Rent", formatCurrency(report.getExpenses().getOperatingExpenses().getRent()));
            addSummaryRow(operatingExpenseTable, "Utilities", formatCurrency(report.getExpenses().getOperatingExpenses().getUtilities()));
            addSummaryRow(operatingExpenseTable, "Other", formatCurrency(report.getExpenses().getOperatingExpenses().getOther()));
            addSummaryRow(operatingExpenseTable, "Subtotal", formatCurrency(report.getExpenses().getOperatingExpenses().getTotal()));
            document.add(operatingExpenseTable);
            
            document.add(new Paragraph(""));
            Table loanLossTable = new Table(2);
            loanLossTable.setWidth(500);
            addHeaderCell(loanLossTable, "Loan Loss Provisions");
            addHeaderCell(loanLossTable, "Amount (KES)");
            
            addSummaryRow(loanLossTable, "Doubtful Debts", formatCurrency(report.getExpenses().getLoanLossProvisions().getDoubtfulDebts()));
            addSummaryRow(loanLossTable, "Write-offs", formatCurrency(report.getExpenses().getLoanLossProvisions().getWriteOffs()));
            addSummaryRow(loanLossTable, "Subtotal", formatCurrency(report.getExpenses().getLoanLossProvisions().getTotal()));
            document.add(loanLossTable);
            
            document.add(new Paragraph(""));
            Table totalExpenseTable = new Table(2);
            totalExpenseTable.setWidth(500);
            addHeaderCell(totalExpenseTable, "Description");
            addHeaderCell(totalExpenseTable, "Amount (KES)");
            addSummaryRow(totalExpenseTable, "Other Expenses", formatCurrency(report.getExpenses().getOtherExpenses()));
            addSummaryRow(totalExpenseTable, "TOTAL EXPENSES", formatCurrency(report.getExpenses().getTotalExpenses()));
            document.add(totalExpenseTable);
            
            document.add(new Paragraph(""));
            document.add(new Paragraph(""));
            
            // Summary Section
            document.add(new Paragraph("SUMMARY").setBold().setFontSize(12));
            Table summaryTable = new Table(2);
            summaryTable.setWidth(500);
            addHeaderCell(summaryTable, "Description");
            addHeaderCell(summaryTable, "Amount (KES)");
            
            addSummaryRow(summaryTable, "Total Revenue", formatCurrency(report.getRevenue().getTotalRevenue()));
            addSummaryRow(summaryTable, "Total Expenses", formatCurrency(report.getExpenses().getTotalExpenses()));
            
            Cell netCell = new Cell();
            netCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            netCell.add(new Paragraph("NET PROFIT/LOSS").setBold().setFontSize(11));
            summaryTable.addCell(netCell);
            
            Cell netValueCell = new Cell();
            netValueCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            netValueCell.add(new Paragraph(formatCurrency(report.getNetProfitLoss())).setBold().setFontSize(11).setTextAlignment(TextAlignment.RIGHT));
            summaryTable.addCell(netValueCell);
            
            addSummaryRow(summaryTable, "Profit Margin (%)", String.format("%.2f%%", report.getProfitMargin()));
            document.add(summaryTable);
            
            document.add(new Paragraph(""));
            document.add(new Paragraph(""));
            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(8));
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new Exception("Error generating Profit & Loss PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Export Withdrawal Monitoring Report to Excel
     */
    public byte[] exportWithdrawalMonitoringToExcel(WithdrawalMonitoringReportDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Withdrawal Monitoring");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Withdrawal Monitoring Report");
            
            // Summary Section
            int rowNum = 2;
            Row summaryHeaderRow = sheet.createRow(rowNum++);
            summaryHeaderRow.createCell(0).setCellValue("SUMMARY");
            
            Row totalWithdrawalsRow = sheet.createRow(rowNum++);
            totalWithdrawalsRow.createCell(0).setCellValue("Total Withdrawals:");
            totalWithdrawalsRow.createCell(1).setCellValue(report.getSummaryTotals().getTotalWithdrawals());
            
            Row totalAmountRow = sheet.createRow(rowNum++);
            totalAmountRow.createCell(0).setCellValue("Total Amount Withdrawn:");
            totalAmountRow.createCell(1).setCellValue(report.getSummaryTotals().getTotalAmountWithdrawn().doubleValue());
            
            rowNum++; // Blank row
            
            Row methodHeaderRow = sheet.createRow(rowNum++);
            methodHeaderRow.createCell(0).setCellValue("By Withdrawal Method");
            methodHeaderRow.createCell(1).setCellValue("Count");
            methodHeaderRow.createCell(2).setCellValue("Amount");
            
            Row mpesaRow = sheet.createRow(rowNum++);
            mpesaRow.createCell(0).setCellValue("M-Pesa");
            mpesaRow.createCell(1).setCellValue(report.getSummaryTotals().getMpesaSummary().getCount());
            mpesaRow.createCell(2).setCellValue(report.getSummaryTotals().getMpesaSummary().getAmount().doubleValue());
            
            Row manualRow = sheet.createRow(rowNum++);
            manualRow.createCell(0).setCellValue("Manual Cash");
            manualRow.createCell(1).setCellValue(report.getSummaryTotals().getManualCashSummary().getCount());
            manualRow.createCell(2).setCellValue(report.getSummaryTotals().getManualCashSummary().getAmount().doubleValue());
            
            Row bankRow = sheet.createRow(rowNum++);
            bankRow.createCell(0).setCellValue("Bank Transfer");
            bankRow.createCell(1).setCellValue(report.getSummaryTotals().getBankTransferSummary().getCount());
            bankRow.createCell(2).setCellValue(report.getSummaryTotals().getBankTransferSummary().getAmount().doubleValue());
            
            rowNum += 2; // Blank rows
            
            // Column headers for transactions
            Row colHeaderRow = sheet.createRow(rowNum++);
            colHeaderRow.createCell(0).setCellValue("Transaction ID");
            colHeaderRow.createCell(1).setCellValue("Member Number");
            colHeaderRow.createCell(2).setCellValue("Member Name");
            colHeaderRow.createCell(3).setCellValue("Account Type");
            colHeaderRow.createCell(4).setCellValue("Withdrawal Amount");
            colHeaderRow.createCell(5).setCellValue("Date");
            colHeaderRow.createCell(6).setCellValue("Method");
            colHeaderRow.createCell(7).setCellValue("Processed By");
            colHeaderRow.createCell(8).setCellValue("Status");
            colHeaderRow.createCell(9).setCellValue("Balance Before");
            colHeaderRow.createCell(10).setCellValue("Balance After");
            
            // Data rows
            for (WithdrawalMonitoringReportDTO.WithdrawalTransaction transaction : report.getWithdrawalTransactions()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(transaction.getTransactionId());
                row.createCell(1).setCellValue(transaction.getMemberNumber());
                row.createCell(2).setCellValue(transaction.getMemberName());
                row.createCell(3).setCellValue(transaction.getAccountType());
                row.createCell(4).setCellValue(transaction.getWithdrawalAmount().doubleValue());
                row.createCell(5).setCellValue(transaction.getTransactionDate().toString());
                row.createCell(6).setCellValue(transaction.getWithdrawalMethod());
                row.createCell(7).setCellValue(transaction.getProcessedBy());
                row.createCell(8).setCellValue(transaction.getTransactionStatus());
                row.createCell(9).setCellValue(transaction.getAccountBalanceBefore().doubleValue());
                row.createCell(10).setCellValue(transaction.getAccountBalanceAfter().doubleValue());
            }
            
            // Auto-size columns
            for (int i = 0; i < 11; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Withdrawal Monitoring Report to PDF
     */
    public byte[] exportWithdrawalMonitoringToPdf(WithdrawalMonitoringReportDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "WITHDRAWAL MONITORING REPORT");
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            // Summary section
            document.add(new Paragraph("SUMMARY").setBold().setFontSize(12));
            Table summaryTable = new Table(2);
            summaryTable.setWidth(300);
            addSummaryRow(summaryTable, "Total Withdrawals:", String.valueOf(report.getSummaryTotals().getTotalWithdrawals()));
            addSummaryRow(summaryTable, "Total Amount:", "KES " + formatCurrency(report.getSummaryTotals().getTotalAmountWithdrawn()));
            document.add(summaryTable);
            document.add(new Paragraph(""));
            
            // By Method section
            document.add(new Paragraph("BY WITHDRAWAL METHOD").setBold().setFontSize(12));
            Table methodTable = new Table(3);
            methodTable.setWidth(400);
            addHeaderCell(methodTable, "Method");
            addHeaderCell(methodTable, "Count");
            addHeaderCell(methodTable, "Amount");
            
            addMethodRow(methodTable, "M-Pesa", report.getSummaryTotals().getMpesaSummary());
            addMethodRow(methodTable, "Manual Cash", report.getSummaryTotals().getManualCashSummary());
            addMethodRow(methodTable, "Bank Transfer", report.getSummaryTotals().getBankTransferSummary());
            
            document.add(methodTable);
            document.add(new Paragraph(""));
            
            // Transactions table
            document.add(new Paragraph("WITHDRAWAL TRANSACTIONS").setBold().setFontSize(12));
            Table table = new Table(11);
            table.setWidth(100);
            addHeaderCell(table, "ID");
            addHeaderCell(table, "Member #");
            addHeaderCell(table, "Name");
            addHeaderCell(table, "Account");
            addHeaderCell(table, "Amount");
            addHeaderCell(table, "Date");
            addHeaderCell(table, "Method");
            addHeaderCell(table, "By");
            addHeaderCell(table, "Status");
            addHeaderCell(table, "Before");
            addHeaderCell(table, "After");
            
            for (WithdrawalMonitoringReportDTO.WithdrawalTransaction transaction : report.getWithdrawalTransactions()) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(transaction.getTransactionId())).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(transaction.getMemberNumber()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(transaction.getMemberName()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(transaction.getAccountType()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(transaction.getWithdrawalAmount())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
                table.addCell(new Cell().add(new Paragraph(transaction.getTransactionDate().toLocalDate().toString()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(transaction.getWithdrawalMethod()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(transaction.getProcessedBy()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(transaction.getTransactionStatus()).setFontSize(8)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(transaction.getAccountBalanceBefore())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(transaction.getAccountBalanceAfter())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
            }
            
            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    private void addMethodRow(Table table, String method, WithdrawalMonitoringReportDTO.MethodSummary summary) {
        table.addCell(new Cell().add(new Paragraph(method).setFontSize(9)));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(summary.getCount())).setFontSize(9).setTextAlignment(TextAlignment.CENTER)));
        table.addCell(new Cell().add(new Paragraph(formatCurrency(summary.getAmount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
    }

    /**
     * Export Guarantor Report to Excel
     */
    public byte[] exportGuarantorReportToExcel(com.minet.sacco.dto.GuarantorReportDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Guarantor Report");
            
            int rowNum = 0;
            
            // Header
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("GUARANTOR REPORT");
            headerRow.createCell(1).setCellValue("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER));
            
            rowNum++; // Blank row
            
            if (report.getMemberDetail() != null) {
                com.minet.sacco.dto.GuarantorReportDTO.MemberGuarantorDetail detail = report.getMemberDetail();
                
                // Member info
                Row memberRow = sheet.createRow(rowNum++);
                memberRow.createCell(0).setCellValue("Member: " + detail.getMemberName() + " (" + detail.getMemberNumber() + ")");
                
                Row statusRow = sheet.createRow(rowNum++);
                statusRow.createCell(0).setCellValue("Status: " + detail.getMemberStatus());
                
                rowNum++; // Blank row
                
                // Guarantor Capacity
                Row capacityHeader = sheet.createRow(rowNum++);
                capacityHeader.createCell(0).setCellValue("GUARANTOR CAPACITY");
                
                Row savingsRow = sheet.createRow(rowNum++);
                savingsRow.createCell(0).setCellValue("Total Savings:");
                savingsRow.createCell(1).setCellValue(detail.getTotalSavings().doubleValue());
                
                Row frozenRow = sheet.createRow(rowNum++);
                frozenRow.createCell(0).setCellValue("Frozen Self-Guarantee:");
                frozenRow.createCell(1).setCellValue(detail.getFrozenSelfGuaranteeAmount().doubleValue());
                
                Row availableRow = sheet.createRow(rowNum++);
                availableRow.createCell(0).setCellValue("Available Savings:");
                availableRow.createCell(1).setCellValue(detail.getAvailableSavings().doubleValue());
                
                rowNum++; // Blank row
                
                // Active Guarantor Pledges
                Row pledgesHeader = sheet.createRow(rowNum++);
                pledgesHeader.createCell(0).setCellValue("ACTIVE GUARANTOR PLEDGES");
                
                Row loansCountRow = sheet.createRow(rowNum++);
                loansCountRow.createCell(0).setCellValue("Loans Being Guaranteed:");
                loansCountRow.createCell(1).setCellValue(detail.getNumberOfLoansGuaranteeing());
                
                Row totalPledgeRow = sheet.createRow(rowNum++);
                totalPledgeRow.createCell(0).setCellValue("Total Pledge Amount:");
                totalPledgeRow.createCell(1).setCellValue(detail.getTotalPledgeAmount().doubleValue());
                
                Row capacityRow = sheet.createRow(rowNum++);
                capacityRow.createCell(0).setCellValue("Available Guarantorship Capacity:");
                capacityRow.createCell(1).setCellValue(detail.getAvailableGuarantorshipCapacity().doubleValue());
                
                rowNum += 2; // Blank rows
                
                // Loans being guaranteed table
                if (detail.getLoansGuaranteeing() != null && !detail.getLoansGuaranteeing().isEmpty()) {
                    Row loansTableHeader = sheet.createRow(rowNum++);
                    loansTableHeader.createCell(0).setCellValue("LOANS BEING GUARANTEED");
                    
                    Row colHeaders = sheet.createRow(rowNum++);
                    colHeaders.createCell(0).setCellValue("Loan #");
                    colHeaders.createCell(1).setCellValue("Borrower");
                    colHeaders.createCell(2).setCellValue("Loan Amount");
                    colHeaders.createCell(3).setCellValue("Outstanding");
                    colHeaders.createCell(4).setCellValue("Pledge Amount");
                    colHeaders.createCell(5).setCellValue("Status");
                    
                    for (com.minet.sacco.dto.GuarantorReportDTO.GuarantorLoansDetail loan : detail.getLoansGuaranteeing()) {
                        Row loanRow = sheet.createRow(rowNum++);
                        loanRow.createCell(0).setCellValue(loan.getLoanNumber());
                        loanRow.createCell(1).setCellValue(loan.getBorrowerName());
                        loanRow.createCell(2).setCellValue(loan.getLoanAmount().doubleValue());
                        loanRow.createCell(3).setCellValue(loan.getOutstandingBalance().doubleValue());
                        loanRow.createCell(4).setCellValue(loan.getGuarantorPledgeAmount().doubleValue());
                        loanRow.createCell(5).setCellValue(loan.getStatus());
                    }
                }
            } else if (report.getMemberSummaries() != null) {
                // All Members View
                Row summaryHeader = sheet.createRow(rowNum++);
                summaryHeader.createCell(0).setCellValue("ALL MEMBERS - GUARANTOR CAPACITY SUMMARY");
                
                rowNum++; // Blank row
                
                Row colHeaders = sheet.createRow(rowNum++);
                colHeaders.createCell(0).setCellValue("Member #");
                colHeaders.createCell(1).setCellValue("Member Name");
                colHeaders.createCell(2).setCellValue("Status");
                colHeaders.createCell(3).setCellValue("Available Savings");
                colHeaders.createCell(4).setCellValue("Guarantorship Capacity");
                colHeaders.createCell(5).setCellValue("Loans Guaranteeing");
                
                for (com.minet.sacco.dto.GuarantorReportDTO.MemberGuarantorSummary summary : report.getMemberSummaries()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(summary.getMemberNumber());
                    row.createCell(1).setCellValue(summary.getMemberName());
                    row.createCell(2).setCellValue(summary.getMemberStatus());
                    row.createCell(3).setCellValue(summary.getAvailableSavings().doubleValue());
                    row.createCell(4).setCellValue(summary.getAvailableGuarantorshipCapacity().doubleValue());
                    row.createCell(5).setCellValue(summary.getNumberOfLoansGuaranteeing());
                }
            }
            
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Guarantor Report to PDF
     */
    public byte[] exportGuarantorReportToPdf(com.minet.sacco.dto.GuarantorReportDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            addReportHeader(document, "GUARANTOR REPORT");
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            if (report.getMemberDetail() != null) {
                com.minet.sacco.dto.GuarantorReportDTO.MemberGuarantorDetail detail = report.getMemberDetail();
                document.add(new Paragraph("Member: " + detail.getMemberName() + " (" + detail.getMemberNumber() + ")")
                    .setFontSize(11).setBold());
                document.add(new Paragraph("Status: " + detail.getMemberStatus()).setFontSize(10));
                document.add(new Paragraph(""));
                
                Table capacityTable = new Table(2);
                capacityTable.setWidth(400);
                addSummaryRow(capacityTable, "Total Savings:", "KES " + formatCurrency(detail.getTotalSavings()));
                addSummaryRow(capacityTable, "Frozen Self-Guarantee:", "KES " + formatCurrency(detail.getFrozenSelfGuaranteeAmount()));
                addSummaryRow(capacityTable, "Available Savings:", "KES " + formatCurrency(detail.getAvailableSavings()));
                document.add(capacityTable);
                document.add(new Paragraph(""));
            }
            
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Loan Eligibility Report to Excel
     */
    public byte[] exportLoanEligibilityToExcel(com.minet.sacco.dto.LoanEligibilityReportDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Loan Eligibility");
            
            int rowNum = 0;
            
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("LOAN ELIGIBILITY REPORT");
            
            Row memberRow = sheet.createRow(rowNum++);
            memberRow.createCell(0).setCellValue("Member: " + report.getMemberName() + " (" + report.getMemberNumber() + ")");
            
            rowNum++; // Blank row
            
            Row savingsHeader = sheet.createRow(rowNum++);
            savingsHeader.createCell(0).setCellValue("SAVINGS STATUS");
            
            Row savingsRow = sheet.createRow(rowNum++);
            savingsRow.createCell(0).setCellValue("Savings Balance:");
            savingsRow.createCell(1).setCellValue(report.getSavingsBalance().doubleValue());
            
            Row frozenRow = sheet.createRow(rowNum++);
            frozenRow.createCell(0).setCellValue("Frozen Amount:");
            frozenRow.createCell(1).setCellValue(report.getFrozenAmount().doubleValue());
            
            Row availableRow = sheet.createRow(rowNum++);
            availableRow.createCell(0).setCellValue("Available Savings:");
            availableRow.createCell(1).setCellValue(report.getAvailableSavings().doubleValue());
            
            rowNum++; // Blank row
            
            Row eligibilityHeader = sheet.createRow(rowNum++);
            eligibilityHeader.createCell(0).setCellValue("ELIGIBILITY CALCULATION");
            
            Row grossEligibilityRow = sheet.createRow(rowNum++);
            grossEligibilityRow.createCell(0).setCellValue("Gross Eligibility (Savings Ã— 3):");
            grossEligibilityRow.createCell(1).setCellValue(report.getGrossEligibility().doubleValue());
            
            Row outstandingRow = sheet.createRow(rowNum++);
            outstandingRow.createCell(0).setCellValue("Outstanding Loan Balance:");
            outstandingRow.createCell(1).setCellValue(report.getOutstandingLoanBalance().doubleValue());
            
            Row remainingRow = sheet.createRow(rowNum++);
            remainingRow.createCell(0).setCellValue("Remaining Eligibility:");
            remainingRow.createCell(1).setCellValue(report.getRemainingEligibility().doubleValue());
            
            Row monthsRow = sheet.createRow(rowNum++);
            monthsRow.createCell(0).setCellValue("Months Contributed:");
            monthsRow.createCell(1).setCellValue(report.getMonthsContributed());
            
            rowNum++; // Blank row
            
            Row statusRow = sheet.createRow(rowNum++);
            statusRow.createCell(0).setCellValue("ELIGIBILITY STATUS");
            
            Row statusValueRow = sheet.createRow(rowNum++);
            statusValueRow.createCell(0).setCellValue("Status:");
            statusValueRow.createCell(1).setCellValue(report.getEligibilityStatus());
            
            Row reasonRow = sheet.createRow(rowNum++);
            reasonRow.createCell(0).setCellValue("Reason:");
            reasonRow.createCell(1).setCellValue(report.getEligibilityReason());
            
            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Loan Eligibility Report to PDF
     */
    public byte[] exportLoanEligibilityToPdf(com.minet.sacco.dto.LoanEligibilityReportDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            addReportHeader(document, "LOAN ELIGIBILITY REPORT");
            document.add(new Paragraph("Member: " + report.getMemberName() + " (" + report.getMemberNumber() + ")")
                .setFontSize(11).setBold());
            document.add(new Paragraph("Status: " + report.getEligibilityStatus() + " - " + report.getEligibilityReason())
                .setFontSize(11).setFontColor(report.getEligibilityStatus().equals("ELIGIBLE") ? ColorConstants.GREEN : ColorConstants.RED));
            document.add(new Paragraph(""));
            
            Table eligibilityTable = new Table(2);
            eligibilityTable.setWidth(400);
            addSummaryRow(eligibilityTable, "Savings Balance:", "KES " + formatCurrency(report.getSavingsBalance()));
            addSummaryRow(eligibilityTable, "Gross Eligibility:", "KES " + formatCurrency(report.getGrossEligibility()));
            addSummaryRow(eligibilityTable, "Remaining Eligibility:", "KES " + formatCurrency(report.getRemainingEligibility()));
            addSummaryRow(eligibilityTable, "Months Contributed:", String.valueOf(report.getMonthsContributed()));
            document.add(eligibilityTable);
            
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Monthly Contribution Tracking to Excel
     */
    public byte[] exportMonthlyContributionTrackingToExcel(com.minet.sacco.dto.MonthlyContributionTrackingDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Monthly Tracking");
            
            int rowNum = 0;
            
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("MONTHLY CONTRIBUTION TRACKING REPORT");
            
            rowNum++; // Blank row
            
            if (report.getBatches() != null && !report.getBatches().isEmpty()) {
                for (com.minet.sacco.dto.MonthlyContributionTrackingDTO.BatchSummary batch : report.getBatches()) {
                    Row batchHeader = sheet.createRow(rowNum++);
                    batchHeader.createCell(0).setCellValue("Batch ID: " + batch.getBatchId() + " - " + batch.getBatchDate());
                    
                    Row statusRow = sheet.createRow(rowNum++);
                    statusRow.createCell(0).setCellValue("Status: " + batch.getBatchStatus());
                    
                    Row membersRow = sheet.createRow(rowNum++);
                    membersRow.createCell(0).setCellValue("Members Processed: " + batch.getSuccessfullyProcessed());
                    
                    Row savingsRow = sheet.createRow(rowNum++);
                    savingsRow.createCell(0).setCellValue("Total Savings: " + formatCurrency(batch.getTotalSavingsPosted()));
                    
                    Row repaymentsRow = sheet.createRow(rowNum++);
                    repaymentsRow.createCell(0).setCellValue("Total Repayments: " + formatCurrency(batch.getTotalLoanRepaymentsPosted()));
                    
                    rowNum++; // Blank row
                }
            }
            
            if (report.getAggregatedSummary() != null) {
                rowNum++; // Blank row
                
                Row summaryHeader = sheet.createRow(rowNum++);
                summaryHeader.createCell(0).setCellValue("AGGREGATED SUMMARY");
                
                Row totalBatchesRow = sheet.createRow(rowNum++);
                totalBatchesRow.createCell(0).setCellValue("Total Batches:");
                totalBatchesRow.createCell(1).setCellValue(report.getAggregatedSummary().getTotalBatches());
                
                Row completedRow = sheet.createRow(rowNum++);
                completedRow.createCell(0).setCellValue("Completed:");
                completedRow.createCell(1).setCellValue(report.getAggregatedSummary().getCompletedBatches());
                
                Row totalSavingsRow = sheet.createRow(rowNum++);
                totalSavingsRow.createCell(0).setCellValue("Total Savings All Batches:");
                totalSavingsRow.createCell(1).setCellValue(report.getAggregatedSummary().getTotalSavingsAllBatches().doubleValue());
                
                Row eligibleRow = sheet.createRow(rowNum++);
                eligibleRow.createCell(0).setCellValue("Total Eligible Members:");
                eligibleRow.createCell(1).setCellValue(report.getAggregatedSummary().getTotalEligibleMembers());
            }
            
            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export Monthly Contribution Tracking to PDF
     */
    public byte[] exportMonthlyContributionTrackingToPdf(com.minet.sacco.dto.MonthlyContributionTrackingDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            addReportHeader(document, "MONTHLY CONTRIBUTION TRACKING REPORT");
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            if (report.getAggregatedSummary() != null) {
                Table summaryTable = new Table(2);
                summaryTable.setWidth(400);
                addSummaryRow(summaryTable, "Total Batches:", String.valueOf(report.getAggregatedSummary().getTotalBatches()));
                addSummaryRow(summaryTable, "Completed:", String.valueOf(report.getAggregatedSummary().getCompletedBatches()));
                addSummaryRow(summaryTable, "Total Savings:", "KES " + formatCurrency(report.getAggregatedSummary().getTotalSavingsAllBatches()));
                addSummaryRow(summaryTable, "Eligible Members:", String.valueOf(report.getAggregatedSummary().getTotalEligibleMembers()));
                document.add(summaryTable);
            }
            
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export GL Trial Balance to Excel
     */
    public byte[] exportTrialBalanceToExcel(com.minet.sacco.dto.TrialBalanceDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Trial Balance");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("GL TRIAL BALANCE REPORT");
            
            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("As at: " + report.getAsOfDate());
            
            // Column headers
            Row colHeaderRow = sheet.createRow(3);
            colHeaderRow.createCell(0).setCellValue("Code");
            colHeaderRow.createCell(1).setCellValue("Account Name");
            colHeaderRow.createCell(2).setCellValue("Type");
            colHeaderRow.createCell(3).setCellValue("Debit");
            colHeaderRow.createCell(4).setCellValue("Credit");
            
            // Data rows
            int rowNum = 4;
            for (com.minet.sacco.dto.TrialBalanceLineDTO line : report.getLines()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(line.getCode());
                row.createCell(1).setCellValue(line.getName());
                row.createCell(2).setCellValue(line.getAccountType());
                
                // Debit or Credit based on isDebit flag
                if (line.getIsDebit()) {
                    row.createCell(3).setCellValue(line.getBalance().doubleValue());
                    row.createCell(4).setCellValue(0);
                } else {
                    row.createCell(3).setCellValue(0);
                    row.createCell(4).setCellValue(line.getBalance().doubleValue());
                }
            }
            
            // Summary rows
            rowNum++;
            Row totalRow = sheet.createRow(rowNum++);
            totalRow.createCell(0).setCellValue("TOTALS");
            totalRow.createCell(3).setCellValue(report.getSummary().getTotalDebit().doubleValue());
            totalRow.createCell(4).setCellValue(report.getSummary().getTotalCredit().doubleValue());
            
            Row balanceRow = sheet.createRow(rowNum++);
            balanceRow.createCell(0).setCellValue("Balanced: " + (report.getSummary().getIsBalanced() ? "YES" : "NO"));
            
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export GL Trial Balance to PDF
     */
    public byte[] exportTrialBalanceToPdf(com.minet.sacco.dto.TrialBalanceDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "GL TRIAL BALANCE REPORT");
            document.add(new Paragraph("As at: " + report.getAsOfDate().format(DATE_FORMATTER))
                .setFontSize(11));
            document.add(new Paragraph(""));
            
            // Table
            Table table = new Table(5);
            table.setWidth(100);
            addHeaderCell(table, "Code");
            addHeaderCell(table, "Account Name");
            addHeaderCell(table, "Type");
            addHeaderCell(table, "Debit");
            addHeaderCell(table, "Credit");
            
            for (com.minet.sacco.dto.TrialBalanceLineDTO line : report.getLines()) {
                table.addCell(new Cell().add(new Paragraph(line.getCode()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(line.getName()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(line.getAccountType()).setFontSize(9)));
                
                if (line.getIsDebit()) {
                    table.addCell(new Cell().add(new Paragraph(formatCurrency(line.getBalance())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                    table.addCell(new Cell().add(new Paragraph("").setFontSize(9)));
                } else {
                    table.addCell(new Cell().add(new Paragraph("").setFontSize(9)));
                    table.addCell(new Cell().add(new Paragraph(formatCurrency(line.getBalance())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                }
            }
            
            // Totals row
            table.addCell(new Cell().add(new Paragraph("TOTALS").setBold().setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph("").setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph("").setFontSize(9)));
            table.addCell(new Cell().add(new Paragraph(formatCurrency(report.getSummary().getTotalDebit())).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
            table.addCell(new Cell().add(new Paragraph(formatCurrency(report.getSummary().getTotalCredit())).setBold().setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
            
            document.add(table);
            document.add(new Paragraph(""));
            document.add(new Paragraph("Balanced: " + (report.getSummary().getIsBalanced() ? "YES" : "NO")).setBold());
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export GL Balance Sheet to Excel
     */
    public byte[] exportBalanceSheetToExcel(com.minet.sacco.dto.BalanceSheetDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance Sheet");
            
            int rowNum = 0;
            
            // Header
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("GL BALANCE SHEET");
            
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("As at: " + report.getAsOfDate());
            
            rowNum++; // Blank row
            
            // Assets
            Row assetsHeader = sheet.createRow(rowNum++);
            assetsHeader.createCell(0).setCellValue("ASSETS");
            
            for (com.minet.sacco.dto.BalanceSheetLineDTO asset : report.getAssets()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(asset.getCode() + " - " + asset.getName());
                row.createCell(1).setCellValue(asset.getAmount().doubleValue());
            }
            
            Row totalAssetsRow = sheet.createRow(rowNum++);
            totalAssetsRow.createCell(0).setCellValue("Total Assets");
            totalAssetsRow.createCell(1).setCellValue(report.getTotalAssets().doubleValue());
            
            rowNum++; // Blank row
            
            // Liabilities
            Row liabilitiesHeader = sheet.createRow(rowNum++);
            liabilitiesHeader.createCell(0).setCellValue("LIABILITIES");
            
            for (com.minet.sacco.dto.BalanceSheetLineDTO liability : report.getLiabilities()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(liability.getCode() + " - " + liability.getName());
                row.createCell(1).setCellValue(liability.getAmount().doubleValue());
            }
            
            Row totalLiabilitiesRow = sheet.createRow(rowNum++);
            totalLiabilitiesRow.createCell(0).setCellValue("Total Liabilities");
            totalLiabilitiesRow.createCell(1).setCellValue(report.getTotalLiabilities().doubleValue());
            
            rowNum++; // Blank row
            
            // Equity
            Row equityHeader = sheet.createRow(rowNum++);
            equityHeader.createCell(0).setCellValue("EQUITY");
            
            for (com.minet.sacco.dto.BalanceSheetLineDTO eq : report.getEquity()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(eq.getCode() + " - " + eq.getName());
                row.createCell(1).setCellValue(eq.getAmount().doubleValue());
            }
            
            Row totalEquityRow = sheet.createRow(rowNum++);
            totalEquityRow.createCell(0).setCellValue("Total Equity");
            totalEquityRow.createCell(1).setCellValue(report.getTotalEquity().doubleValue());
            
            rowNum++; // Blank row
            
            Row balanceRow = sheet.createRow(rowNum++);
            balanceRow.createCell(0).setCellValue("Balanced: " + (report.getIsBalanced() ? "YES" : "NO"));
            
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export GL Balance Sheet to PDF
     */
    public byte[] exportBalanceSheetToPdf(com.minet.sacco.dto.BalanceSheetDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "GL BALANCE SHEET");
            document.add(new Paragraph("As at: " + report.getAsOfDate().format(DATE_FORMATTER))
                .setFontSize(11));
            document.add(new Paragraph(""));
            
            // Assets
            document.add(new Paragraph("ASSETS").setBold().setFontSize(12));
            Table assetsTable = new Table(2);
            assetsTable.setWidth(400);
            
            for (com.minet.sacco.dto.BalanceSheetLineDTO asset : report.getAssets()) {
                addSummaryRow(assetsTable, asset.getCode() + " - " + asset.getName(), 
                    "KES " + formatCurrency(asset.getAmount()));
            }
            addSummaryRow(assetsTable, "Total Assets:", "KES " + formatCurrency(report.getTotalAssets()));
            document.add(assetsTable);
            document.add(new Paragraph(""));
            
            // Liabilities
            document.add(new Paragraph("LIABILITIES").setBold().setFontSize(12));
            Table liabilitiesTable = new Table(2);
            liabilitiesTable.setWidth(400);
            
            for (com.minet.sacco.dto.BalanceSheetLineDTO liability : report.getLiabilities()) {
                addSummaryRow(liabilitiesTable, liability.getCode() + " - " + liability.getName(), 
                    "KES " + formatCurrency(liability.getAmount()));
            }
            addSummaryRow(liabilitiesTable, "Total Liabilities:", "KES " + formatCurrency(report.getTotalLiabilities()));
            document.add(liabilitiesTable);
            document.add(new Paragraph(""));
            
            // Equity
            document.add(new Paragraph("EQUITY").setBold().setFontSize(12));
            Table equityTable = new Table(2);
            equityTable.setWidth(400);
            
            for (com.minet.sacco.dto.BalanceSheetLineDTO eq : report.getEquity()) {
                addSummaryRow(equityTable, eq.getCode() + " - " + eq.getName(), 
                    "KES " + formatCurrency(eq.getAmount()));
            }
            addSummaryRow(equityTable, "Total Equity:", "KES " + formatCurrency(report.getTotalEquity()));
            document.add(equityTable);
            document.add(new Paragraph(""));
            
            document.add(new Paragraph("Balanced: " + (report.getIsBalanced() ? "YES" : "NO")).setBold());
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export GL Income Statement to Excel
     */
    public byte[] exportIncomeStatementToExcel(com.minet.sacco.dto.IncomeStatementDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Income Statement");
            
            int rowNum = 0;
            
            // Header
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("GL INCOME STATEMENT");
            
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Period: " + report.getFromDate() + " to " + report.getToDate());
            
            rowNum++; // Blank row
            
            // Revenues
            Row revenueHeader = sheet.createRow(rowNum++);
            revenueHeader.createCell(0).setCellValue("REVENUES");
            
            for (com.minet.sacco.dto.IncomeStatementLineDTO revenue : report.getRevenues()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(revenue.getCode() + " - " + revenue.getName());
                row.createCell(1).setCellValue(revenue.getAmount().doubleValue());
            }
            
            Row totalRevenueRow = sheet.createRow(rowNum++);
            totalRevenueRow.createCell(0).setCellValue("Total Revenues");
            totalRevenueRow.createCell(1).setCellValue(report.getTotalRevenues().doubleValue());
            
            rowNum++; // Blank row
            
            // Expenses
            Row expenseHeader = sheet.createRow(rowNum++);
            expenseHeader.createCell(0).setCellValue("EXPENSES");
            
            for (com.minet.sacco.dto.IncomeStatementLineDTO expense : report.getExpenses()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(expense.getCode() + " - " + expense.getName());
                row.createCell(1).setCellValue(expense.getAmount().doubleValue());
            }
            
            Row totalExpenseRow = sheet.createRow(rowNum++);
            totalExpenseRow.createCell(0).setCellValue("Total Expenses");
            totalExpenseRow.createCell(1).setCellValue(report.getTotalExpenses().doubleValue());
            
            rowNum++; // Blank row
            
            // Net Income
            Row netIncomeRow = sheet.createRow(rowNum++);
            netIncomeRow.createCell(0).setCellValue("NET INCOME");
            netIncomeRow.createCell(1).setCellValue(report.getNetIncome().doubleValue());
            
            Row profitMarginRow = sheet.createRow(rowNum++);
            profitMarginRow.createCell(0).setCellValue("Profit Margin (%)");
            profitMarginRow.createCell(1).setCellValue(report.getProfitMarginPercent().doubleValue());
            
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Export GL Income Statement to PDF
     */
    public byte[] exportIncomeStatementToPdf(com.minet.sacco.dto.IncomeStatementDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "GL INCOME STATEMENT");
            document.add(new Paragraph("Period: " + report.getFromDate().format(DATE_FORMATTER) + 
                " to " + report.getToDate().format(DATE_FORMATTER))
                .setFontSize(11));
            document.add(new Paragraph(""));
            
            // Revenues
            document.add(new Paragraph("REVENUES").setBold().setFontSize(12));
            Table revenuesTable = new Table(2);
            revenuesTable.setWidth(400);
            
            for (com.minet.sacco.dto.IncomeStatementLineDTO revenue : report.getRevenues()) {
                addSummaryRow(revenuesTable, revenue.getCode() + " - " + revenue.getName(), 
                    "KES " + formatCurrency(revenue.getAmount()));
            }
            addSummaryRow(revenuesTable, "Total Revenues:", "KES " + formatCurrency(report.getTotalRevenues()));
            document.add(revenuesTable);
            document.add(new Paragraph(""));
            
            // Expenses
            document.add(new Paragraph("EXPENSES").setBold().setFontSize(12));
            Table expensesTable = new Table(2);
            expensesTable.setWidth(400);
            
            for (com.minet.sacco.dto.IncomeStatementLineDTO expense : report.getExpenses()) {
                addSummaryRow(expensesTable, expense.getCode() + " - " + expense.getName(), 
                    "KES " + formatCurrency(expense.getAmount()));
            }
            addSummaryRow(expensesTable, "Total Expenses:", "KES " + formatCurrency(report.getTotalExpenses()));
            document.add(expensesTable);
            document.add(new Paragraph(""));
            
            // Net Income
            Table summaryTable = new Table(2);
            summaryTable.setWidth(400);
            addSummaryRow(summaryTable, "Net Income:", "KES " + formatCurrency(report.getNetIncome()));
            addSummaryRow(summaryTable, "Profit Margin (%):", report.getProfitMarginPercent().toString() + "%");
            document.add(summaryTable);
            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }

    /**
     * Export Over-Committed Guarantor Report to Excel
     */
    public byte[] exportOverCommittedGuarantorToExcel(com.minet.sacco.dto.OverCommittedGuarantorDTO report) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Over-Committed Guarantors");
        
        // Set column widths
        sheet.setColumnWidth(0, 4000);  // Member ID
        sheet.setColumnWidth(1, 4000);  // Member Number
        sheet.setColumnWidth(2, 5000);  // Member Name
        sheet.setColumnWidth(3, 4000);  // Total Savings
        sheet.setColumnWidth(4, 4000);  // Available Savings
        sheet.setColumnWidth(5, 4000);  // Frozen Pledges
        sheet.setColumnWidth(6, 4000);  // Over-Committed
        sheet.setColumnWidth(7, 3000);  // # Loans
        
        // Title row
        Row titleRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("OVER-COMMITTED GUARANTOR RISK REPORT");
        titleCell.setCellStyle(createHeaderStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        
        // Summary row
        Row summaryRow1 = sheet.createRow(1);
        summaryRow1.createCell(0).setCellValue("Total At Risk:");
        summaryRow1.createCell(1).setCellValue(formatCurrency(report.getTotalAtRisk()));
        
        Row summaryRow2 = sheet.createRow(2);
        summaryRow2.createCell(0).setCellValue("Count Over-Committed:");
        summaryRow2.createCell(1).setCellValue(report.getCountOverCommitted());
        
        // Header row
        Row headerRow = sheet.createRow(4);
        String[] headers = {"Member ID", "Member #", "Name", "Total Savings", "Available Savings", "Frozen Pledges", "Over-Committed By", "# Loans"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        // Data rows
        int rowNum = 5;
        for (com.minet.sacco.dto.OverCommittedGuarantorDTO.OverCommittedGuarantorDetail detail : report.getOverCommittedGuarantors()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(detail.getMemberId());
            row.createCell(1).setCellValue(detail.getMemberNumber());
            row.createCell(2).setCellValue(detail.getMemberName());
            row.createCell(3).setCellValue(toDouble(detail.getTotalSavings()));
            row.createCell(4).setCellValue(toDouble(detail.getAvailableSavings()));
            row.createCell(5).setCellValue(toDouble(detail.getFrozenPledges()));
            row.createCell(6).setCellValue(toDouble(detail.getAmountOverCommitted()));
            row.createCell(7).setCellValue(detail.getNumberOfLoansGuaranteeing());
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    /**
     * Export Over-Committed Guarantor Report to PDF
     */
    public byte[] exportOverCommittedGuarantorToPdf(com.minet.sacco.dto.OverCommittedGuarantorDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "OVER-COMMITTED GUARANTOR RISK REPORT");
            document.add(new Paragraph("Generated: " + java.time.LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(10));
            document.add(new Paragraph(""));
            
            // Summary
            Table summaryTable = new Table(2);
            summaryTable.setWidth(300);
            addSummaryRow(summaryTable, "Total At Risk", formatCurrency(report.getTotalAtRisk()));
            addSummaryRow(summaryTable, "Guarantors Over-Committed", String.valueOf(report.getCountOverCommitted()));
            document.add(summaryTable);
            document.add(new Paragraph(""));
            
            // Details table
            Table detailsTable = new Table(8);
            addHeaderCell(detailsTable, "Member #");
            addHeaderCell(detailsTable, "Name");
            addHeaderCell(detailsTable, "Total Savings");
            addHeaderCell(detailsTable, "Available Savings");
            addHeaderCell(detailsTable, "Frozen Pledges");
            addHeaderCell(detailsTable, "Over-Committed");
            addHeaderCell(detailsTable, "# Loans");
            addHeaderCell(detailsTable, "Status");
            
            for (com.minet.sacco.dto.OverCommittedGuarantorDTO.OverCommittedGuarantorDetail detail : report.getOverCommittedGuarantors()) {
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getMemberNumber() != null ? detail.getMemberNumber() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getMemberName() != null ? detail.getMemberName() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(formatCurrency(detail.getTotalSavings())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                detailsTable.addCell(new Cell().add(new Paragraph(formatCurrency(detail.getAvailableSavings())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                detailsTable.addCell(new Cell().add(new Paragraph(formatCurrency(detail.getFrozenPledges())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                detailsTable.addCell(new Cell().add(new Paragraph(formatCurrency(detail.getAmountOverCommitted())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                detailsTable.addCell(new Cell().add(new Paragraph(String.valueOf(detail.getNumberOfLoansGuaranteeing())).setFontSize(9).setTextAlignment(TextAlignment.CENTER)));
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getMemberStatus() != null ? detail.getMemberStatus() : "").setFontSize(9)));
            }
            
            document.add(detailsTable);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return baos.toByteArray();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Export Exited Members with Outstanding Loans to Excel
     */
    public byte[] exportExitedMemberLoanToExcel(ExitedMemberLoanDTO report) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Exited Members Loans");
        
        // Set column widths
        sheet.setColumnWidth(0, 3000);  // Member ID
        sheet.setColumnWidth(1, 4000);  // Member Number
        sheet.setColumnWidth(2, 5000);  // Member Name
        sheet.setColumnWidth(3, 4000);  // Exit Date
        sheet.setColumnWidth(4, 4000);  // Exit Reason
        sheet.setColumnWidth(5, 4000);  // Loan ID
        sheet.setColumnWidth(6, 4000);  // Loan Number
        sheet.setColumnWidth(7, 4000);  // Outstanding Balance
        sheet.setColumnWidth(8, 4000);  // Original Amount
        sheet.setColumnWidth(9, 4000);  // Disbursement Date
        
        // Title row
        Row titleRow = sheet.createRow(0);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EXITED MEMBERS WITH OUTSTANDING LOANS");
        titleCell.setCellStyle(createHeaderStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
        
        // Header row
        Row headerRow = sheet.createRow(2);
        String[] headers = {"Member ID", "Member #", "Name", "Exit Date", "Exit Reason", "Loan ID", "Loan #", 
                           "Outstanding Balance", "Original Amount", "Disbursement Date"};
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }
        
        // Data rows
        int rowNum = 3;
        for (ExitedMemberLoanDTO.ExitedMemberLoanDetail detail : report.getExitedMembersWithLoans()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(detail.getMemberId());
            row.createCell(1).setCellValue(detail.getMemberNumber());
            row.createCell(2).setCellValue(detail.getMemberName());
            row.createCell(3).setCellValue(detail.getExitDate() != null ? detail.getExitDate().toString() : "");
            row.createCell(4).setCellValue(detail.getExitReason() != null ? detail.getExitReason() : "");
            row.createCell(5).setCellValue(detail.getLoanId());
            row.createCell(6).setCellValue(detail.getLoanNumber());
            row.createCell(7).setCellValue(toDouble(detail.getOutstandingBalance()));
            row.createCell(8).setCellValue(toDouble(detail.getOriginalAmount()));
            row.createCell(9).setCellValue(detail.getDisbursementDate() != null ? detail.getDisbursementDate().toString() : "");
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    /**
     * Export Exited Members with Outstanding Loans to PDF
     */
    public byte[] exportExitedMemberLoanToPdf(ExitedMemberLoanDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Header
            addReportHeader(document, "EXITED MEMBERS WITH OUTSTANDING LOANS");
            document.add(new Paragraph("Generated: " + LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(10));
            document.add(new Paragraph(""));
            
            // Details table
            Table detailsTable = new Table(10);
            addHeaderCell(detailsTable, "Member #");
            addHeaderCell(detailsTable, "Name");
            addHeaderCell(detailsTable, "Exit Date");
            addHeaderCell(detailsTable, "Exit Reason");
            addHeaderCell(detailsTable, "Loan #");
            addHeaderCell(detailsTable, "Outstanding Balance");
            addHeaderCell(detailsTable, "Original Amount");
            addHeaderCell(detailsTable, "Disbursement Date");
            addHeaderCell(detailsTable, "Member ID");
            addHeaderCell(detailsTable, "Loan ID");
            
            for (ExitedMemberLoanDTO.ExitedMemberLoanDetail detail : report.getExitedMembersWithLoans()) {
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getMemberNumber() != null ? detail.getMemberNumber() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getMemberName() != null ? detail.getMemberName() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getExitDate() != null ? detail.getExitDate().toString() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getExitReason() != null ? detail.getExitReason() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getLoanNumber() != null ? detail.getLoanNumber() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(formatCurrency(detail.getOutstandingBalance())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                detailsTable.addCell(new Cell().add(new Paragraph(formatCurrency(detail.getOriginalAmount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                detailsTable.addCell(new Cell().add(new Paragraph(detail.getDisbursementDate() != null ? detail.getDisbursementDate().toString() : "").setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(String.valueOf(detail.getMemberId())).setFontSize(9)));
                detailsTable.addCell(new Cell().add(new Paragraph(String.valueOf(detail.getLoanId())).setFontSize(9)));
            }
            
            document.add(detailsTable);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return baos.toByteArray();
    }

    // =========================================================================
    // MEMBER CONTRIBUTIONS REPORT — EXCEL
    // =========================================================================

    /**
     * Export a member's full contribution / transaction history to Excel.
     */
    public byte[] exportMemberContributionsToExcel(com.minet.sacco.dto.MemberContributionsReportDTO report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {

            // ── Style helpers ────────────────────────────────────────────────
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font whiteFont = workbook.createFont();
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            whiteFont.setBold(true);
            headerStyle.setFont(whiteFont);

            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            CellStyle amountStyle = workbook.createCellStyle();
            DataFormat fmt = workbook.createDataFormat();
            amountStyle.setDataFormat(fmt.getFormat("#,##0.00"));

            // ── Sheet 1: Summary ─────────────────────────────────────────────
            Sheet summary = workbook.createSheet("Summary");

            int r = 0;
            Row titleRow = summary.createRow(r++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(COMPANY_NAME + " — MEMBER CONTRIBUTIONS REPORT");
            titleCell.setCellStyle(boldStyle);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            r++; // blank
            createLabelValueRow(summary, r++, "Member Number", report.getMemberNumber(), boldStyle);
            createLabelValueRow(summary, r++, "Member Name",   report.getMemberName(),   boldStyle);
            createLabelValueRow(summary, r++, "Email",         report.getEmail(),         boldStyle);
            createLabelValueRow(summary, r++, "Phone",         report.getPhone(),         boldStyle);

            if (report.getStartDate() != null || report.getEndDate() != null) {
                String period = (report.getStartDate() != null ? report.getStartDate().format(DATE_FORMATTER) : "—")
                        + " to "
                        + (report.getEndDate() != null ? report.getEndDate().format(DATE_FORMATTER) : "—");
                createLabelValueRow(summary, r++, "Period", period, boldStyle);
            }
            if (report.getAccountTypeFilter() != null) {
                createLabelValueRow(summary, r++, "Account Type Filter", report.getAccountTypeFilter(), boldStyle);
            }
            createLabelValueRow(summary, r++, "Generated At",
                    report.getGeneratedAt() != null ? report.getGeneratedAt().format(DATETIME_FORMATTER) : "", boldStyle);

            r++; // blank
            Row totalsHeader = summary.createRow(r++);
            totalsHeader.createCell(0).setCellValue("TOTALS");
            totalsHeader.getCell(0).setCellStyle(boldStyle);

            createAmountRow(summary, r++, "Total Deposited (KES)",    report.getTotalDeposited(),  amountStyle, boldStyle);
            createAmountRow(summary, r++, "Total Withdrawn (KES)",    report.getTotalWithdrawn(),  amountStyle, boldStyle);
            createAmountRow(summary, r++, "Net Contribution (KES)",   report.getNetContribution(), amountStyle, boldStyle);

            r++; // blank
            if (report.getAccountSummaries() != null && !report.getAccountSummaries().isEmpty()) {
                Row accHeader = summary.createRow(r++);
                String[] accCols = {"Account Type", "Current Balance (KES)", "Total Deposited (KES)", "Total Withdrawn (KES)", "Transactions"};
                for (int c = 0; c < accCols.length; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = accHeader.createCell(c);
                    cell.setCellValue(accCols[c]);
                    cell.setCellStyle(headerStyle);
                }
                for (com.minet.sacco.dto.MemberContributionsReportDTO.AccountSummary acc : report.getAccountSummaries()) {
                    Row row = summary.createRow(r++);
                    row.createCell(0).setCellValue(acc.getAccountType() != null ? acc.getAccountType() : "");
                    org.apache.poi.ss.usermodel.Cell balCell = row.createCell(1);
                    balCell.setCellValue(toDouble(acc.getCurrentBalance()));
                    balCell.setCellStyle(amountStyle);
                    org.apache.poi.ss.usermodel.Cell depCell = row.createCell(2);
                    depCell.setCellValue(toDouble(acc.getTotalDeposited()));
                    depCell.setCellStyle(amountStyle);
                    org.apache.poi.ss.usermodel.Cell wdrCell = row.createCell(3);
                    wdrCell.setCellValue(toDouble(acc.getTotalWithdrawn()));
                    wdrCell.setCellStyle(amountStyle);
                    row.createCell(4).setCellValue(acc.getTransactionCount());
                }
            }

            for (int c = 0; c < 5; c++) summary.autoSizeColumn(c);

            // ── Sheet 2: Transactions ─────────────────────────────────────────
            Sheet txSheet = workbook.createSheet("Transactions");

            Row txHeader = txSheet.createRow(0);
            String[] txCols = {"Date", "Account Type", "Transaction Type", "Amount (KES)", "Description", "Processed By"};
            for (int c = 0; c < txCols.length; c++) {
                org.apache.poi.ss.usermodel.Cell cell = txHeader.createCell(c);
                cell.setCellValue(txCols[c]);
                cell.setCellStyle(headerStyle);
            }

            int txRow = 1;
            if (report.getEntries() != null) {
                for (com.minet.sacco.dto.MemberContributionsReportDTO.ContributionEntry entry : report.getEntries()) {
                    Row row = txSheet.createRow(txRow++);
                    row.createCell(0).setCellValue(
                            entry.getTransactionDate() != null ? entry.getTransactionDate().format(DATETIME_FORMATTER) : "");
                    row.createCell(1).setCellValue(entry.getAccountType() != null ? entry.getAccountType() : "");
                    row.createCell(2).setCellValue(entry.getTransactionType() != null ? entry.getTransactionType() : "");
                    org.apache.poi.ss.usermodel.Cell amtCell = row.createCell(3);
                    amtCell.setCellValue(toDouble(entry.getAmount()));
                    amtCell.setCellStyle(amountStyle);
                    row.createCell(4).setCellValue(entry.getDescription() != null ? entry.getDescription() : "");
                    row.createCell(5).setCellValue(entry.getProcessedBy() != null ? entry.getProcessedBy() : "");
                }
            }

            for (int c = 0; c < txCols.length; c++) txSheet.autoSizeColumn(c);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // =========================================================================
    // MEMBER CONTRIBUTIONS REPORT — PDF
    // =========================================================================

    /**
     * Export a member's full contribution / transaction history to PDF.
     */
    public byte[] exportMemberContributionsToPdf(com.minet.sacco.dto.MemberContributionsReportDTO report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // ── Header ────────────────────────────────────────────────────────
            addReportHeader(document, "MEMBER CONTRIBUTIONS REPORT");

            document.add(new Paragraph("Member:  " + report.getMemberName()
                    + "  (" + report.getMemberNumber() + ")").setFontSize(11));
            if (report.getEmail() != null)
                document.add(new Paragraph("Email:   " + report.getEmail()).setFontSize(10));
            if (report.getPhone() != null)
                document.add(new Paragraph("Phone:   " + report.getPhone()).setFontSize(10));
            if (report.getStartDate() != null || report.getEndDate() != null) {
                String period = (report.getStartDate() != null ? report.getStartDate().format(DATE_FORMATTER) : "—")
                        + " to "
                        + (report.getEndDate() != null ? report.getEndDate().format(DATE_FORMATTER) : "—");
                document.add(new Paragraph("Period:  " + period).setFontSize(10));
            }
            if (report.getAccountTypeFilter() != null)
                document.add(new Paragraph("Account Type: " + report.getAccountTypeFilter()).setFontSize(10));
            document.add(new Paragraph("Generated: "
                    + (report.getGeneratedAt() != null ? report.getGeneratedAt().format(DATETIME_FORMATTER) : ""))
                    .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));

            // ── Overall Totals ────────────────────────────────────────────────
            document.add(new Paragraph("SUMMARY").setBold().setFontSize(12));
            Table totalsTable = new Table(2);
            totalsTable.setWidth(350);
            addSummaryRow(totalsTable, "Total Deposited (KES):",  "KES " + formatCurrency(report.getTotalDeposited()));
            addSummaryRow(totalsTable, "Total Withdrawn (KES):",  "KES " + formatCurrency(report.getTotalWithdrawn()));
            addSummaryRow(totalsTable, "Net Contribution (KES):", "KES " + formatCurrency(report.getNetContribution()));
            document.add(totalsTable);
            document.add(new Paragraph(""));

            // ── Per-account breakdown ─────────────────────────────────────────
            if (report.getAccountSummaries() != null && !report.getAccountSummaries().isEmpty()) {
                document.add(new Paragraph("ACCOUNT BREAKDOWN").setBold().setFontSize(12));
                Table accTable = new Table(5);
                addHeaderCell(accTable, "Account Type");
                addHeaderCell(accTable, "Balance (KES)");
                addHeaderCell(accTable, "Deposited (KES)");
                addHeaderCell(accTable, "Withdrawn (KES)");
                addHeaderCell(accTable, "Txns");
                for (com.minet.sacco.dto.MemberContributionsReportDTO.AccountSummary acc : report.getAccountSummaries()) {
                    accTable.addCell(new Cell().add(new Paragraph(acc.getAccountType() != null ? acc.getAccountType() : "").setFontSize(9)));
                    accTable.addCell(new Cell().add(new Paragraph(formatCurrency(acc.getCurrentBalance())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                    accTable.addCell(new Cell().add(new Paragraph(formatCurrency(acc.getTotalDeposited())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                    accTable.addCell(new Cell().add(new Paragraph(formatCurrency(acc.getTotalWithdrawn())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                    accTable.addCell(new Cell().add(new Paragraph(String.valueOf(acc.getTransactionCount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                }
                document.add(accTable);
                document.add(new Paragraph(""));
            }

            // ── Transaction detail ────────────────────────────────────────────
            document.add(new Paragraph("TRANSACTION HISTORY").setBold().setFontSize(12));
            if (report.getEntries() == null || report.getEntries().isEmpty()) {
                document.add(new Paragraph("No transactions found for the selected criteria.").setFontSize(10));
            } else {
                Table txTable = new Table(6);
                addHeaderCell(txTable, "Date");
                addHeaderCell(txTable, "Account");
                addHeaderCell(txTable, "Type");
                addHeaderCell(txTable, "Amount (KES)");
                addHeaderCell(txTable, "Description");
                addHeaderCell(txTable, "Processed By");

                for (com.minet.sacco.dto.MemberContributionsReportDTO.ContributionEntry entry : report.getEntries()) {
                    txTable.addCell(new Cell().add(new Paragraph(
                            entry.getTransactionDate() != null ? entry.getTransactionDate().format(DATETIME_FORMATTER) : "").setFontSize(8)));
                    txTable.addCell(new Cell().add(new Paragraph(
                            entry.getAccountType() != null ? entry.getAccountType() : "").setFontSize(8)));
                    txTable.addCell(new Cell().add(new Paragraph(
                            entry.getTransactionType() != null ? entry.getTransactionType() : "").setFontSize(8)));
                    txTable.addCell(new Cell().add(new Paragraph(
                            formatCurrency(entry.getAmount())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
                    txTable.addCell(new Cell().add(new Paragraph(
                            entry.getDescription() != null ? entry.getDescription() : "").setFontSize(8)));
                    txTable.addCell(new Cell().add(new Paragraph(
                            entry.getProcessedBy() != null ? entry.getProcessedBy() : "").setFontSize(8)));
                }
                document.add(txTable);
            }

            document.close();
        } catch (Exception e) {
            throw new Exception("Failed to generate member contributions PDF: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    // ── Private helpers shared by the new export methods ─────────────────────

    private void createLabelValueRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell lbl = row.createCell(0);
        lbl.setCellValue(label + ":");
        lbl.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value != null ? value : "");
    }

    private void createAmountRow(Sheet sheet, int rowNum, String label, BigDecimal amount,
                                 CellStyle amountStyle, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell lbl = row.createCell(0);
        lbl.setCellValue(label);
        lbl.setCellStyle(labelStyle);
        org.apache.poi.ss.usermodel.Cell val = row.createCell(1);
        val.setCellValue(toDouble(amount));
        val.setCellStyle(amountStyle);
    }
}
