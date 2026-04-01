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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
            headerRow.createCell(1).setCellValue("Period: " + report.getStartDate() + " to " + report.getEndDate());
            
            // Summary
            Row summaryRow = sheet.createRow(2);
            summaryRow.createCell(0).setCellValue("Total Deposits:");
            summaryRow.createCell(1).setCellValue(report.getTotalDeposits().doubleValue());
            
            Row summaryRow2 = sheet.createRow(3);
            summaryRow2.createCell(0).setCellValue("Total Withdrawals:");
            summaryRow2.createCell(1).setCellValue(report.getTotalWithdrawals().doubleValue());
            
            Row summaryRow3 = sheet.createRow(4);
            summaryRow3.createCell(0).setCellValue("Total Repayments:");
            summaryRow3.createCell(1).setCellValue(report.getTotalRepayments().doubleValue());
            
            Row summaryRow4 = sheet.createRow(5);
            summaryRow4.createCell(0).setCellValue("Net Cash:");
            summaryRow4.createCell(1).setCellValue(report.getNetCash().doubleValue());
            
            // Column headers
            Row colHeaderRow = sheet.createRow(7);
            colHeaderRow.createCell(0).setCellValue("Date");
            colHeaderRow.createCell(1).setCellValue("Type");
            colHeaderRow.createCell(2).setCellValue("Member");
            colHeaderRow.createCell(3).setCellValue("Account");
            colHeaderRow.createCell(4).setCellValue("Amount");
            colHeaderRow.createCell(5).setCellValue("Description");
            
            // Data rows
            int rowNum = 8;
            for (ReportsService.CashbookEntry entry : report.getEntries()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getDate().toString());
                row.createCell(1).setCellValue(entry.getTransactionType());
                row.createCell(2).setCellValue(entry.getMemberNumber());
                row.createCell(3).setCellValue(entry.getAccountType());
                row.createCell(4).setCellValue(entry.getAmount().doubleValue());
                row.createCell(5).setCellValue(entry.getDescription());
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
            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }
            document.add(new Paragraph("Period: " + startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER))
                .setFontSize(11));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFontSize(10).setFontColor(ColorConstants.GRAY));
            document.add(new Paragraph(""));
            
            // Summary section
            document.add(new Paragraph("SUMMARY").setBold().setFontSize(12));
            Table summaryTable = new Table(2);
            summaryTable.setWidth(300);
            addSummaryRow(summaryTable, "Total Deposits:", "KES " + formatCurrency(report.getTotalDeposits()));
            addSummaryRow(summaryTable, "Total Withdrawals:", "KES " + formatCurrency(report.getTotalWithdrawals()));
            addSummaryRow(summaryTable, "Total Repayments:", "KES " + formatCurrency(report.getTotalRepayments()));
            addSummaryRow(summaryTable, "Net Cash:", "KES " + formatCurrency(report.getNetCash()));
            document.add(summaryTable);
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
            
            for (ReportsService.CashbookEntry entry : report.getEntries()) {
                table.addCell(new Cell().add(new Paragraph(entry.getDate().toString()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(entry.getTransactionType()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(entry.getMemberNumber()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(entry.getAccountType()).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getAmount())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
                table.addCell(new Cell().add(new Paragraph(entry.getDescription()).setFontSize(9)));
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
                row.createCell(6).setCellValue(entry.getMonthlyRepayment().doubleValue());
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
                table.addCell(new Cell().add(new Paragraph(formatCurrency(entry.getMonthlyRepayment())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)));
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
        return String.format("%,.2f", amount);
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
}
