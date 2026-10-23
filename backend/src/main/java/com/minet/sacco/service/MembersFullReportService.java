package com.minet.sacco.service;

import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Guarantor;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.Member;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.GuarantorRepository;
import com.minet.sacco.repository.LoanRepaymentRepository;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.MemberRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * One workbook with everything a treasurer needs for all members:
 * Summary, Members &amp; Savings, Loans, Guarantors. Amounts are numeric cells in KES.
 */
@Service
public class MembersFullReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired private MemberRepository memberRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private LoanRepaymentRepository loanRepaymentRepository;
    @Autowired private GuarantorRepository guarantorRepository;

    @Transactional(readOnly = true)
    public byte[] buildWorkbook() throws Exception {
        List<Member> members = memberRepository.findAll().stream()
                .sorted(Comparator.comparing(m -> m.getMemberNumber() == null ? "" : m.getMemberNumber(), this::naturalCompare))
                .collect(Collectors.toList());
        List<Account> accounts = accountRepository.findAllWithDetails();
        List<Loan> loans = loanRepository.findAll().stream()
                .sorted(Comparator.comparing(l -> l.getLoanNumber() == null ? "" : l.getLoanNumber()))
                .collect(Collectors.toList());

        // member id -> balances
        Map<Long, BigDecimal> savings = new HashMap<>();
        Map<Long, BigDecimal> shares = new HashMap<>();
        Map<Long, BigDecimal> frozen = new HashMap<>();
        for (Account a : accounts) {
            if (a.getMember() == null) continue;
            Long mid = a.getMember().getId();
            BigDecimal bal = nz(a.getBalance());
            if (a.getAccountType() == Account.AccountType.SAVINGS) {
                savings.merge(mid, bal, BigDecimal::add);
                frozen.merge(mid, nz(a.getFrozenSavings()), BigDecimal::add);
            } else if (a.getAccountType() == Account.AccountType.SHARES) {
                shares.merge(mid, bal, BigDecimal::add);
            }
        }

        // member id -> outstanding on live loans, and loan count
        Map<Long, BigDecimal> outstanding = new HashMap<>();
        Map<Long, Integer> liveLoanCount = new HashMap<>();
        for (Loan l : loans) {
            if (l.getMember() == null || l.getStatus() != Loan.Status.DISBURSED) continue;
            outstanding.merge(l.getMember().getId(), nz(l.getOutstandingBalance()), BigDecimal::add);
            liveLoanCount.merge(l.getMember().getId(), 1, Integer::sum);
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            // ---------------- Members & Savings
            Sheet ms = wb.createSheet("Members & Savings");
            String[] msHead = {"Member No", "Name", "Status", "Phone", "Savings (KES)", "Shares (KES)",
                    "Frozen savings (KES)", "Total savings + shares (KES)", "Live loans", "Loan outstanding (KES)"};
            header(ms, st, msHead);
            int r = 1;
            BigDecimal tSav = BigDecimal.ZERO, tSh = BigDecimal.ZERO, tFr = BigDecimal.ZERO, tOut = BigDecimal.ZERO;
            for (Member m : members) {
                Row row = ms.createRow(r++);
                BigDecimal sv = savings.getOrDefault(m.getId(), BigDecimal.ZERO);
                BigDecimal sh = shares.getOrDefault(m.getId(), BigDecimal.ZERO);
                BigDecimal fr = frozen.getOrDefault(m.getId(), BigDecimal.ZERO);
                BigDecimal out = outstanding.getOrDefault(m.getId(), BigDecimal.ZERO);
                text(row, 0, m.getMemberNumber(), st.text);
                text(row, 1, m.getFullName(), st.text);
                text(row, 2, m.getStatus() == null ? "" : m.getStatus().toString(), st.text);
                text(row, 3, m.getPhone(), st.text);
                money(row, 4, sv, st);
                money(row, 5, sh, st);
                money(row, 6, fr, st);
                money(row, 7, sv.add(sh), st);
                num(row, 8, liveLoanCount.getOrDefault(m.getId(), 0), st);
                money(row, 9, out, st);
                tSav = tSav.add(sv); tSh = tSh.add(sh); tFr = tFr.add(fr); tOut = tOut.add(out);
            }
            Row tot = ms.createRow(r);
            text(tot, 0, "TOTAL", st.bold);
            text(tot, 1, members.size() + " members", st.bold);
            moneyBold(tot, 4, tSav, st); moneyBold(tot, 5, tSh, st); moneyBold(tot, 6, tFr, st);
            moneyBold(tot, 7, tSav.add(tSh), st); moneyBold(tot, 9, tOut, st);
            finish(ms, msHead.length, members.size());

            // ---------------- Loans
            Sheet ls = wb.createSheet("Loans");
            String[] lHead = {"Loan No", "Member No", "Member", "Product", "Status", "Amount (KES)", "Interest rate %",
                    "Term (months)", "Monthly repayment (KES)", "Applied", "Approved", "Disbursed",
                    "Total repaid (KES)", "Outstanding (KES)"};
            header(ls, st, lHead);
            r = 1;
            BigDecimal tAmt = BigDecimal.ZERO, tRep = BigDecimal.ZERO, tLOut = BigDecimal.ZERO;
            for (Loan l : loans) {
                Row row = ls.createRow(r++);
                BigDecimal repaid = nz(loanRepaymentRepository.getTotalRepaidAmount(l.getId()));
                text(row, 0, l.getLoanNumber(), st.text);
                text(row, 1, l.getMember() == null ? "" : l.getMember().getMemberNumber(), st.text);
                text(row, 2, l.getMember() == null ? "" : l.getMember().getFullName(), st.text);
                text(row, 3, l.getLoanProduct() == null ? "" : l.getLoanProduct().getName(), st.text);
                text(row, 4, l.getStatus() == null ? "" : l.getStatus().toString(), st.text);
                money(row, 5, nz(l.getAmount()), st);
                num(row, 6, l.getInterestRate() == null ? 0 : l.getInterestRate().doubleValue(), st);
                num(row, 7, l.getTermMonths() == null ? 0 : l.getTermMonths(), st);
                money(row, 8, nz(l.getMonthlyRepayment()), st);
                text(row, 9, date(l.getApplicationDate()), st.text);
                text(row, 10, date(l.getApprovalDate()), st.text);
                text(row, 11, date(l.getDisbursementDate()), st.text);
                money(row, 12, repaid, st);
                money(row, 13, nz(l.getOutstandingBalance()), st);
                tAmt = tAmt.add(nz(l.getAmount()));
                tRep = tRep.add(repaid);
                if (l.getStatus() == Loan.Status.DISBURSED) tLOut = tLOut.add(nz(l.getOutstandingBalance()));
            }
            Row lt = ls.createRow(r);
            text(lt, 0, "TOTAL", st.bold);
            text(lt, 1, loans.size() + " loans", st.bold);
            moneyBold(lt, 5, tAmt, st); moneyBold(lt, 12, tRep, st); moneyBold(lt, 13, tLOut, st);
            text(lt, 4, "Outstanding = disbursed only", st.bold);
            finish(ls, lHead.length, loans.size());

            // ---------------- Guarantors
            Sheet gs = wb.createSheet("Guarantors");
            String[] gHead = {"Loan No", "Borrower No", "Borrower", "Loan status", "Guarantor No", "Guarantor",
                    "Guarantee (KES)", "Frozen pledge (KES)", "Guarantor status", "Self guarantee"};
            header(gs, st, gHead);
            r = 1;
            List<Long> loanIds = loans.stream().map(Loan::getId).collect(Collectors.toList());
            if (!loanIds.isEmpty()) {
                List<Guarantor> guarantors = guarantorRepository.findByLoanIdIn(loanIds);
                guarantors.sort(Comparator.comparing((Guarantor g) -> g.getLoan().getLoanNumber() == null ? "" : g.getLoan().getLoanNumber()));
                for (Guarantor g : guarantors) {
                    Row row = gs.createRow(r++);
                    Loan l = g.getLoan();
                    text(row, 0, l.getLoanNumber(), st.text);
                    text(row, 1, l.getMember() == null ? "" : l.getMember().getMemberNumber(), st.text);
                    text(row, 2, l.getMember() == null ? "" : l.getMember().getFullName(), st.text);
                    text(row, 3, l.getStatus() == null ? "" : l.getStatus().toString(), st.text);
                    text(row, 4, g.getMember() == null ? "" : g.getMember().getMemberNumber(), st.text);
                    text(row, 5, g.getMember() == null ? "" : g.getMember().getFullName(), st.text);
                    money(row, 6, nz(g.getGuaranteeAmount()), st);
                    money(row, 7, nz(g.getPledgeAmount()), st);
                    text(row, 8, g.getStatus() == null ? "" : g.getStatus().toString(), st.text);
                    text(row, 9, g.isSelfGuarantee() ? "Yes" : "No", st.text);
                }
            }
            finish(gs, gHead.length, r - 1);

            // ---------------- Summary (first sheet)
            Sheet sum = wb.createSheet("Summary");
            wb.setSheetOrder("Summary", 0);
            wb.setActiveSheet(0);
            Row t = sum.createRow(0);
            text(t, 0, "MINET SACCO - MEMBERS, SAVINGS AND LOANS", st.title);
            text(sum.createRow(1), 0, "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), st.text);
            text(sum.createRow(2), 0, "Currency: KES", st.text);
            String[][] kv = {
                    {"Members", String.valueOf(members.size())},
                    {"Loans on record (all statuses)", String.valueOf(loans.size())},
            };
            int sr = 4;
            for (String[] p : kv) {
                Row row = sum.createRow(sr++);
                text(row, 0, p[0], st.bold);
                text(row, 1, p[1], st.text);
            }
            summaryMoney(sum, sr++, "Total member savings", tSav, st);
            summaryMoney(sum, sr++, "Total member shares", tSh, st);
            summaryMoney(sum, sr++, "Total frozen savings (pledged)", tFr, st);
            summaryMoney(sum, sr++, "Total savings + shares", tSav.add(tSh), st);
            summaryMoney(sum, sr++, "Total loans issued (all statuses)", tAmt, st);
            summaryMoney(sum, sr++, "Total repaid on all loans", tRep, st);
            summaryMoney(sum, sr++, "Total outstanding (disbursed loans)", tLOut, st);
            sum.setColumnWidth(0, 44 * 256);
            sum.setColumnWidth(1, 22 * 256);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ------------------------------------------------------------------ helpers

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String date(LocalDateTime d) {
        return d == null ? "" : d.format(DATE);
    }

    /** Member numbers are mostly numeric: sort 99 before 100, text after numbers. */
    private int naturalCompare(String a, String b) {
        boolean an = a.matches("\\d+"), bn = b.matches("\\d+");
        if (an && bn) {
            int c = Long.compare(Long.parseLong(a), Long.parseLong(b));
            return c != 0 ? c : a.compareTo(b);
        }
        if (an) return -1;
        if (bn) return 1;
        return a.compareToIgnoreCase(b);
    }

    private static class Styles {
        final CellStyle header, text, bold, money, moneyBold, title, numeric;
        Styles(Workbook wb) {
            Font bf = wb.createFont();
            bf.setBold(true);
            Font tf = wb.createFont();
            tf.setBold(true);
            tf.setFontHeightInPoints((short) 14);
            DataFormat df = wb.createDataFormat();

            header = wb.createCellStyle();
            header.setFont(bf);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setWrapText(true);
            header.setBorderBottom(BorderStyle.THIN);

            text = wb.createCellStyle();

            bold = wb.createCellStyle();
            bold.setFont(bf);

            title = wb.createCellStyle();
            title.setFont(tf);

            money = wb.createCellStyle();
            money.setDataFormat(df.getFormat("#,##0.00"));

            moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(df.getFormat("#,##0.00"));
            moneyBold.setFont(bf);
            moneyBold.setBorderTop(BorderStyle.THIN);

            numeric = wb.createCellStyle();
            numeric.setDataFormat(df.getFormat("#,##0.##"));
        }
    }

    private void header(Sheet sheet, Styles st, String[] cols) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(st.header);
        }
    }

    private void finish(Sheet sheet, int cols, int lastDataRow) {
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            if (w > 60 * 256) sheet.setColumnWidth(i, 60 * 256);
        }
        if (lastDataRow >= 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, lastDataRow, 0, cols - 1));
        }
    }

    private void text(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    private void money(Row row, int col, BigDecimal v, Styles st) {
        Cell c = row.createCell(col);
        c.setCellValue(v.doubleValue());
        c.setCellStyle(st.money);
    }

    private void moneyBold(Row row, int col, BigDecimal v, Styles st) {
        Cell c = row.createCell(col);
        c.setCellValue(v.doubleValue());
        c.setCellStyle(st.moneyBold);
    }

    private void num(Row row, int col, double v, Styles st) {
        Cell c = row.createCell(col);
        c.setCellValue(v);
        c.setCellStyle(st.numeric);
    }

    private void summaryMoney(Sheet sheet, int rowNum, String label, BigDecimal v, Styles st) {
        Row row = sheet.createRow(rowNum);
        text(row, 0, label, st.bold);
        money(row, 1, v, st);
    }
}
