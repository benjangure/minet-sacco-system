package com.minet.sacco.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.LoanRepayment;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.repository.LoanRepaymentRepository;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.util.NameFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Member-facing PDF statements: account statement (with running balances), loan statement and
 * transaction history. All figures are KES, two decimals, never null-unsafe.
 */
@Service
public class MemberStatementPdfService {

    /** Sentinel "start" used when the member did not choose a date range. */
    public static final LocalDate ALL_TIME_START = LocalDate.of(2000, 1, 1);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

    @Autowired private ReportsService reportsService;
    @Autowired private LoanRepository loanRepository;
    @Autowired private LoanRepaymentRepository loanRepaymentRepository;

    // ------------------------------------------------------------------ account statement

    public byte[] accountStatement(Member member, LocalDate start, LocalDate end) throws Exception {
        ReportsService.MemberStatementReport report = reportsService.generateMemberStatement(member.getId(), start, end);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = new Document(new PdfDocument(new PdfWriter(out)))) {
            header(doc, "ACCOUNT STATEMENT", member, start, end);

            Map<String, List<ReportsService.MemberStatementEntry>> byAccount = new TreeMap<>();
            if (report.getOpeningBalances() != null) {
                for (String type : report.getOpeningBalances().keySet()) {
                    byAccount.put(type, new ArrayList<>());
                }
            }
            for (ReportsService.MemberStatementEntry e : report.getEntries()) {
                byAccount.computeIfAbsent(e.getAccountType(), k -> new ArrayList<>()).add(e);
            }

            if (byAccount.isEmpty()) {
                doc.add(new Paragraph("No accounts were found for this member.").setFontSize(10));
            }

            for (Map.Entry<String, List<ReportsService.MemberStatementEntry>> acc : byAccount.entrySet()) {
                String type = acc.getKey();
                BigDecimal opening = report.getOpeningBalances() != null && report.getOpeningBalances().get(type) != null
                        ? report.getOpeningBalances().get(type) : BigDecimal.ZERO;

                doc.add(new Paragraph(label(type) + " ACCOUNT").setBold().setFontSize(12).setMarginTop(12));
                Table table = new Table(UnitValue.createPercentArray(new float[]{14, 18, 30, 13, 13, 14})).useAllAvailableWidth();
                headerCell(table, "Date");
                headerCell(table, "Type");
                headerCell(table, "Description");
                headerCell(table, "Money in");
                headerCell(table, "Money out");
                headerCell(table, "Balance");

                table.addCell(cell("", false));
                table.addCell(cell("Opening balance", false));
                table.addCell(cell("", false));
                table.addCell(cell("", false));
                table.addCell(cell("", false));
                table.addCell(cell(money(opening), true));

                BigDecimal in = BigDecimal.ZERO;
                BigDecimal outTotal = BigDecimal.ZERO;
                BigDecimal closing = opening;
                for (ReportsService.MemberStatementEntry e : acc.getValue()) {
                    boolean credit = isCredit(e.getTransactionType());
                    boolean debit = isDebit(e.getTransactionType());
                    BigDecimal amt = e.getAmount() == null ? BigDecimal.ZERO : e.getAmount();
                    table.addCell(cell(e.getDate() == null ? "" : e.getDate().format(DATE), false));
                    table.addCell(cell(pretty(e.getTransactionType()), false));
                    table.addCell(cell(e.getDescription() == null ? "" : e.getDescription(), false));
                    table.addCell(cell(credit ? money(amt) : "", true));
                    table.addCell(cell(debit ? money(amt) : "", true));
                    if (e.getBalance() != null) closing = e.getBalance();
                    table.addCell(cell(money(e.getBalance() == null ? closing : e.getBalance()), true));
                    if (credit) in = in.add(amt);
                    if (debit) outTotal = outTotal.add(amt);
                }
                doc.add(table);

                Table sum = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth().setMarginTop(4);
                summaryRow(sum, "Total money in", money(in));
                summaryRow(sum, "Total money out", money(outTotal));
                summaryRow(sum, "Closing balance", money(closing));
                doc.add(sum);

                if (acc.getValue().isEmpty()) {
                    doc.add(new Paragraph("No transactions in this period.").setFontSize(9).setItalic());
                }
            }

            if (report.getCurrentBalances() != null && !report.getCurrentBalances().isEmpty()) {
                doc.add(new Paragraph("CURRENT BALANCES").setBold().setFontSize(12).setMarginTop(14));
                Table cur = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
                for (Map.Entry<String, BigDecimal> b : new TreeMap<>(report.getCurrentBalances()).entrySet()) {
                    summaryRow(cur, label(b.getKey()), money(b.getValue()));
                }
                doc.add(cur);
            }
            footer(doc);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ transaction history

    public byte[] transactionHistory(Member member, LocalDate start, LocalDate end) throws Exception {
        ReportsService.MemberStatementReport report = reportsService.generateMemberStatement(member.getId(), start, end);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = new Document(new PdfDocument(new PdfWriter(out)))) {
            header(doc, "TRANSACTION HISTORY", member, start, end);

            List<ReportsService.MemberStatementEntry> rows = new ArrayList<>(report.getEntries());
            // newest first
            rows.sort((a, b) -> {
                if (a.getDate() == null || b.getDate() == null) return 0;
                return b.getDate().compareTo(a.getDate());
            });

            if (rows.isEmpty()) {
                doc.add(new Paragraph("No transactions were found for this period.").setFontSize(10).setItalic());
            } else {
                Table table = new Table(UnitValue.createPercentArray(new float[]{15, 20, 15, 18, 32})).useAllAvailableWidth();
                headerCell(table, "Date");
                headerCell(table, "Type");
                headerCell(table, "Account");
                headerCell(table, "Amount (KES)");
                headerCell(table, "Description");
                for (ReportsService.MemberStatementEntry e : rows) {
                    table.addCell(cell(e.getDate() == null ? "" : e.getDate().format(DATE), false));
                    table.addCell(cell(pretty(e.getTransactionType()), false));
                    table.addCell(cell(label(e.getAccountType()), false));
                    table.addCell(cell(money(e.getAmount()), true));
                    table.addCell(cell(e.getDescription() == null ? "" : e.getDescription(), false));
                }
                doc.add(table);
            }

            doc.add(new Paragraph("SUMMARY").setBold().setFontSize(12).setMarginTop(14));
            Table sum = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
            BigDecimal in = report.getTotalDeposits() == null ? BigDecimal.ZERO : report.getTotalDeposits();
            BigDecimal outAmt = report.getTotalWithdrawals() == null ? BigDecimal.ZERO : report.getTotalWithdrawals();
            summaryRow(sum, "Transactions", String.valueOf(rows.size()));
            summaryRow(sum, "Total money in", money(in));
            summaryRow(sum, "Total money out", money(outAmt));
            summaryRow(sum, "Net movement", money(in.subtract(outAmt)));
            doc.add(sum);
            footer(doc);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ loan statement

    public byte[] loanStatement(Member member, LocalDate start, LocalDate end) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = new Document(new PdfDocument(new PdfWriter(out)))) {
            header(doc, "LOAN STATEMENT", member, start, end);

            List<Loan> loans = loanRepository.findByMemberId(member.getId());
            if (loans == null || loans.isEmpty()) {
                doc.add(new Paragraph("You have no loans on record.").setFontSize(10).setItalic());
            }

            BigDecimal totalOutstanding = BigDecimal.ZERO;
            BigDecimal totalRepaidInPeriod = BigDecimal.ZERO;

            for (Loan loan : loans == null ? new ArrayList<Loan>() : loans) {
                String product = loan.getLoanProduct() != null ? loan.getLoanProduct().getName() : "Loan";
                doc.add(new Paragraph("Loan " + safe(loan.getLoanNumber()) + " - " + product)
                        .setBold().setFontSize(12).setMarginTop(12));

                Table details = new Table(UnitValue.createPercentArray(new float[]{45, 55})).useAllAvailableWidth();
                summaryRow(details, "Status", pretty(loan.getStatus() == null ? null : loan.getStatus().toString()));
                summaryRow(details, "Amount", money(loan.getAmount()));
                summaryRow(details, "Interest rate", loan.getInterestRate() == null ? "-" : loan.getInterestRate().stripTrailingZeros().toPlainString() + "%");
                summaryRow(details, "Term", loan.getTermMonths() == null ? "-" : loan.getTermMonths() + " months");
                summaryRow(details, "Monthly repayment", money(loan.getMonthlyRepayment()));
                if (loan.getDisbursementDate() != null) {
                    summaryRow(details, "Disbursed", loan.getDisbursementDate().format(DATE));
                }
                summaryRow(details, "Outstanding balance", money(loan.getOutstandingBalance()));
                doc.add(details);

                List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanIdOrderByPaymentDateDesc(loan.getId());
                List<LoanRepayment> inPeriod = new ArrayList<>();
                if (repayments != null) {
                    for (LoanRepayment r : repayments) {
                        if (r.getPaymentDate() == null) continue;
                        LocalDate d = r.getPaymentDate().toLocalDate();
                        if (!d.isBefore(start) && !d.isAfter(end)) inPeriod.add(r);
                    }
                }
                if (inPeriod.isEmpty()) {
                    doc.add(new Paragraph("No repayments in this period.").setFontSize(9).setItalic());
                } else {
                    doc.add(new Paragraph("Repayments").setBold().setFontSize(10).setMarginTop(6));
                    Table t = new Table(UnitValue.createPercentArray(new float[]{30, 35, 35})).useAllAvailableWidth();
                    headerCell(t, "Date");
                    headerCell(t, "Amount (KES)");
                    headerCell(t, "Method");
                    BigDecimal sub = BigDecimal.ZERO;
                    for (LoanRepayment r : inPeriod) {
                        t.addCell(cell(r.getPaymentDate().format(DATE), false));
                        t.addCell(cell(money(r.getAmount()), true));
                        t.addCell(cell(r.getPaymentMethod() == null ? "-" : pretty(r.getPaymentMethod().toString()), false));
                        if (r.getAmount() != null) sub = sub.add(r.getAmount());
                    }
                    doc.add(t);
                    totalRepaidInPeriod = totalRepaidInPeriod.add(sub);
                }
                if (loan.getOutstandingBalance() != null) {
                    totalOutstanding = totalOutstanding.add(loan.getOutstandingBalance());
                }
            }

            doc.add(new Paragraph("SUMMARY").setBold().setFontSize(12).setMarginTop(14));
            Table sum = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
            summaryRow(sum, "Repaid in this period", money(totalRepaidInPeriod));
            summaryRow(sum, "Total outstanding", money(totalOutstanding));
            doc.add(sum);
            footer(doc);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ helpers

    private void header(Document doc, String title, Member member, LocalDate start, LocalDate end) {
        doc.add(new Paragraph("MINET SACCO").setBold().setFontSize(13).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(title).setBold().setFontSize(17).setTextAlignment(TextAlignment.CENTER));
        String name = NameFormatter.toProperCase(member.getFullName() != null ? member.getFullName() : member.getFirstName());
        doc.add(new Paragraph("Member: " + safe(name) + " (" + safe(member.getMemberNumber()) + ")").setFontSize(10).setMarginTop(8));
        String period = start.equals(ALL_TIME_START)
                ? "All transactions up to " + end.format(DATE)
                : start.format(DATE) + " to " + end.format(DATE);
        doc.add(new Paragraph("Period: " + period).setFontSize(10));
        doc.add(new Paragraph("Generated: " + LocalDateTime.now().format(STAMP)).setFontSize(10));
        doc.add(new Paragraph("Currency: KES").setFontSize(10));
    }

    private void footer(Document doc) {
        doc.add(new Paragraph("This statement is computer generated and does not require a signature.")
                .setFontSize(8).setItalic().setMarginTop(16));
    }

    private void headerCell(Table t, String text) {
        t.addHeaderCell(new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
    }

    private Cell cell(String text, boolean right) {
        Paragraph p = new Paragraph(text == null ? "" : text).setFontSize(9);
        if (right) p.setTextAlignment(TextAlignment.RIGHT);
        return new Cell().add(p);
    }

    private void summaryRow(Table t, String label, String value) {
        t.addCell(new Cell().add(new Paragraph(label).setFontSize(9)));
        t.addCell(new Cell().add(new Paragraph(value).setFontSize(9).setBold().setTextAlignment(TextAlignment.RIGHT)));
    }

    static String money(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v;
        return String.format(Locale.US, "%,.2f", x);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isCredit(String type) {
        return "DEPOSIT".equals(type) || "LOAN_DISBURSEMENT".equals(type);
    }

    private static boolean isDebit(String type) {
        return "WITHDRAWAL".equals(type) || "LOAN_REPAYMENT".equals(type);
    }

    private static String label(String accountType) {
        if (accountType == null) return "";
        String s = accountType.toLowerCase(Locale.ROOT).replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String pretty(String enumName) {
        return enumName == null ? "-" : label(enumName);
    }
}
