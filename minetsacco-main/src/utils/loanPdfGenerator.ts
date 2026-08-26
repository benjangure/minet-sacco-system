/**
 * Minet SACCO – Loan Offer & Disbursement Letter PDF Generator
 *
 * Theme   : Minet Red (#C0002A)
 * Exports : generateLoanPdf()  – downloads PDF
 *           printLoanPdf()     – opens in new tab and triggers browser print
 */

import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";

// ---------------------------------------------------------------------------
// Public types
// ---------------------------------------------------------------------------
export interface PdfLoan {
  id: number;
  loanNumber?: string;
  member: { id: number; memberNumber: string; firstName: string; lastName: string; fullName?: string };
  loanProduct: { id: number; name: string; interestRate: number };
  amount: number;
  interestRate: number;
  termMonths: number;
  monthlyRepayment?: number;
  totalInterest?: number;
  totalRepayable?: number;
  outstandingBalance?: number;
  interestCollected?: number;
  principalRepaid?: number;
  status: string;
  purpose?: string;
  applicationDate?: string;
  approvalDate?: string;
  disbursementDate?: string;
  rejectionReason?: string;
  createdBy?: any;   // backend may send full User object
  approvedBy?: any;
  disbursedBy?: any;
  guarantors?: PdfGuarantor[];
  totalTopupAmount?: number;
  topupCount?: number;
}

export interface PdfGuarantor {
  guarantorId?: number;
  id?: number;
  memberNumber?: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  status: string;
  guaranteeAmount?: number;
  frozenPledge?: number;
  pledgeAmount?: number;
  selfGuarantee?: boolean;
  createdAt?: string;
  approvedAt?: string;
  member?: { id: number; memberNumber: string; firstName: string; lastName: string; fullName?: string };
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Safely turn any value (including a backend User object) into a plain string. */
const s = (v: any, fb = "—"): string => {
  if (v == null) return fb;
  if (typeof v === "string") return v.trim() || fb;
  if (typeof v === "number" || typeof v === "boolean") return String(v);
  if (typeof v === "object")
    return String((v as any).fullName || (v as any).firstName || (v as any).username || fb).trim() || fb;
  return fb;
};

const KES = (v?: number) =>
  v != null
    ? `KES ${Number(v).toLocaleString("en-KE", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    : "KES 0.00";

const dt = (d?: string | null) => {
  if (!d) return "—";
  try { return new Date(d).toLocaleDateString("en-KE", { day: "2-digit", month: "long", year: "numeric" }); }
  catch { return String(d); }
};

const titleCase = (str: string) =>
  str.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());

const gName = (g: PdfGuarantor) =>
  g.member ? `${g.member.firstName} ${g.member.lastName}`.trim()
  : g.firstName ? `${g.firstName} ${g.lastName ?? ""}`.trim()
  : g.fullName ?? "—";

const gNo = (g: PdfGuarantor) => g.member?.memberNumber ?? g.memberNumber ?? "—";

// ---------------------------------------------------------------------------
// Colours (red theme)
// ---------------------------------------------------------------------------
type RGB = [number, number, number];
const RED:       RGB = [192,  0,  42];   // Minet red
const RED_DARK:  RGB = [140,  0,  30];
const RED_LIGHT: RGB = [255,245,247];
const GOLD:      RGB = [210,150,110];    // warm accent line
const LGRAY:     RGB = [245,246,248];
const MGRAY:     RGB = [120,120,120];
const DARK:      RGB = [30, 30, 30];
const WHITE:     RGB = [255,255,255];
const BORDER:    RGB = [220,195,200];
const GREEN:     RGB = [22, 120, 55];
const ORANGE:    RGB = [180, 80,  0];
const RED_MID:   RGB = [140, 60, 70];   // label text

// ---------------------------------------------------------------------------
// Logo loader
// ---------------------------------------------------------------------------
const loadLogo = (): Promise<string | null> =>
  new Promise(resolve => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = "/Minet-Logo1.png";
    img.onload = () => {
      const c = document.createElement("canvas");
      c.width = img.naturalWidth; c.height = img.naturalHeight;
      const ctx = c.getContext("2d");
      if (!ctx) return resolve(null);
      ctx.drawImage(img, 0, 0);
      resolve(c.toDataURL("image/png"));
    };
    img.onerror = () => resolve(null);
  });

// ---------------------------------------------------------------------------
// Core builder
// ---------------------------------------------------------------------------
const buildDoc = async (loan: PdfLoan, guarantors: PdfGuarantor[], byName?: string): Promise<jsPDF> => {
  const doc   = new jsPDF({ orientation: "portrait", unit: "mm", format: "a4" });
  const PW    = doc.internal.pageSize.getWidth();
  const PH    = doc.internal.pageSize.getHeight();
  const ML    = 18;
  const MR    = PW - 18;
  const CW    = MR - ML;   // 174 mm

  const all: PdfGuarantor[] = guarantors.length ? guarantors : loan.guarantors ?? [];
  const logo = await loadLogo();

  // ── page utilities ────────────────────────────────────────────────────────

  const newPage = (): number => {
    doc.addPage();
    doc.setFillColor(...RED);
    doc.rect(0, 0, PW, 6, "F");
    doc.setFillColor(...GOLD);
    doc.rect(0, 6, PW, 1, "F");
    if (logo) doc.addImage(logo, "PNG", ML, 9, 20, 9);
    doc.setFontSize(7); doc.setTextColor(...MGRAY);
    doc.text("MINET SACCO SOCIETY LIMITED — CONFIDENTIAL", MR, 14, { align: "right" });
    doc.setDrawColor(...BORDER);
    doc.line(ML, 20, MR, 20);
    return 27;
  };

  const need = (space: number, y: number): number =>
    y + space > PH - 22 ? newPage() : y;

  const heading = (title: string, uw: number, y: number): number => {
    y = need(14, y);
    doc.setFont("helvetica", "bold").setFontSize(9).setTextColor(...RED);
    doc.text(title, ML, y);
    y += 4;
    doc.setDrawColor(...RED).setLineWidth(0.5);
    doc.line(ML, y, ML + uw, y);
    doc.setLineWidth(0.2);
    y += 3;
    return y;
  };

  const footers = () => {
    const n: number = (doc.internal as any).getNumberOfPages();
    for (let i = 1; i <= n; i++) {
      doc.setPage(i);
      doc.setFillColor(...RED);
      doc.rect(0, PH - 10, PW, 10, "F");
      doc.setFontSize(7).setTextColor(...WHITE);
      doc.text(
        "Minet SACCO Society Limited  |  P.O. Box 00100, Nairobi, Kenya  |  Tel: +254 700 000 000  |  sacco@minet.co.ke",
        PW / 2, PH - 5.5, { align: "center" }
      );
      doc.text(`Page ${i} of ${n}`, MR, PH - 5.5, { align: "right" });
      doc.setFontSize(6).setTextColor(200, 170, 175);
      doc.text("This document is confidential and intended solely for the named recipient.", PW / 2, PH - 12, { align: "center" });
    }
  };

  const sigBox = (label: string, name: string, role: string, memNo: string | undefined, x: number, y: number, w: number) => {
    // outer box
    doc.setFillColor(...LGRAY).setDrawColor(...BORDER);
    doc.roundedRect(x, y, w, 40, 2, 2, "FD");
    // header
    doc.setFillColor(...RED);
    doc.roundedRect(x, y, w, 7, 2, 2, "F");
    doc.rect(x, y + 4, w, 3, "F");
    doc.setFont("helvetica", "bold").setFontSize(7).setTextColor(...WHITE);
    doc.text(label.toUpperCase(), x + w / 2, y + 5, { align: "center" });
    // name
    doc.setFontSize(7.5).setTextColor(...DARK);
    doc.text(s(name, "—"), x + w / 2, y + 12.5, { align: "center" });
    // role
    doc.setFont("helvetica", "normal").setFontSize(6.5).setTextColor(...MGRAY);
    doc.text(s(role, ""), x + w / 2, y + 16.5, { align: "center" });
    if (memNo) doc.text(`(${s(memNo)})`, x + w / 2, y + 20, { align: "center" });
    // sig line
    doc.setDrawColor(150, 100, 110).setLineWidth(0.3);
    doc.line(x + 5, y + 30, x + w - 5, y + 30);
    doc.setFontSize(6).setTextColor(...MGRAY);
    doc.text("Signature & Date", x + w / 2, y + 34, { align: "center" });
    // stamp
    doc.setDrawColor(...BORDER).setLineWidth(0.2);
    doc.rect(x + w - 18, y + 8, 14, 14);
    doc.setFontSize(5.5).setTextColor(...BORDER);
    doc.text("STAMP", x + w - 11, y + 17, { align: "center" });
  };

  // ==========================================================================
  // PAGE 1 – LETTERHEAD
  // ==========================================================================
  doc.setFillColor(...RED); doc.rect(0, 0, PW, 6, "F");
  doc.setFillColor(...GOLD); doc.rect(0, 6, PW, 1.2, "F");

  let y = 10;

  if (logo) { doc.addImage(logo, "PNG", ML, y, 38, 17); }
  else {
    doc.setFont("helvetica", "bold").setFontSize(16).setTextColor(...RED);
    doc.text("MINET SACCO", ML, y + 10);
  }

  doc.setFont("helvetica", "bold").setFontSize(15).setTextColor(...RED);
  doc.text("MINET SACCO SOCIETY LIMITED", MR, y + 6, { align: "right" });
  doc.setFont("helvetica", "normal").setFontSize(8).setTextColor(...MGRAY);
  doc.text("Registered under the Co-operative Societies Act (Cap. 490)", MR, y + 11, { align: "right" });
  doc.text("P.O. Box 00100, Nairobi, Kenya  |  Tel: +254 700 000 000",   MR, y + 15, { align: "right" });
  doc.text("Email: sacco@minet.co.ke  |  www.minetsacco.co.ke",           MR, y + 19, { align: "right" });

  y += 24;
  doc.setDrawColor(...RED).setLineWidth(0.7); doc.line(ML, y, MR, y);
  doc.setDrawColor(...GOLD).setLineWidth(0.5); doc.line(ML, y + 1.5, MR, y + 1.5);
  doc.setLineWidth(0.2);
  y += 6;

  // Title banner
  const docTitle =
    loan.status === "DISBURSED" ? "LOAN DISBURSEMENT CONFIRMATION" :
    loan.status === "APPROVED"  ? "LOAN APPROVAL LETTER"           :
                                  "LOAN OFFER DOCUMENT";
  doc.setFillColor(...RED); doc.roundedRect(ML, y, CW, 10, 2, 2, "F");
  doc.setFillColor(215, 40, 70); doc.roundedRect(ML + 1, y + 1, CW - 2, 8, 1.5, 1.5, "F");
  doc.setFont("helvetica", "bold").setFontSize(12).setTextColor(...WHITE);
  doc.text(docTitle, PW / 2, y + 6.8, { align: "center" });
  y += 15;

  // Reference + Status boxes
  const rw = CW * 0.52;
  const sx2 = ML + rw + 5;
  const sw  = CW - rw - 5;

  doc.setFillColor(...RED_LIGHT).setDrawColor(...BORDER);
  doc.roundedRect(ML, y, rw, 22, 2, 2, "FD");
  doc.setFont("helvetica", "bold").setFontSize(7.5).setTextColor(...RED);
  doc.text("LOAN REFERENCE", ML + 4, y + 5);
  const refs: [string, string][] = [
    ["Loan Number:", s(loan.loanNumber, `LOAN-${loan.id}`)],
    ["Application Date:", dt(loan.applicationDate)],
    ["Approval Date:", dt(loan.approvalDate)],
    ["Disbursement Date:", loan.status === "DISBURSED" ? dt(loan.disbursementDate) : "Pending"],
  ];
  let ry = y + 9;
  refs.forEach(([l, v]) => {
    doc.setFont("helvetica", "bold").setFontSize(7).setTextColor(...RED_MID); doc.text(l, ML + 4, ry);
    doc.setFont("helvetica", "normal").setTextColor(...DARK);                 doc.text(v, ML + 37, ry);
    ry += 4;
  });

  doc.setFillColor(...RED_LIGHT).setDrawColor(...BORDER);
  doc.roundedRect(sx2, y, sw, 22, 2, 2, "FD");
  doc.setFont("helvetica", "bold").setFontSize(7.5).setTextColor(...RED);
  doc.text("STATUS", sx2 + 4, y + 5);
  const sColors: Record<string, RGB> = {
    PENDING_TREASURER: RED, APPROVED: GREEN, DISBURSED: RED_DARK, REJECTED: [130, 0, 20],
  };
  const pill: RGB = sColors[loan.status] ?? MGRAY;
  doc.setFillColor(...pill); doc.roundedRect(sx2 + 4, y + 7, sw - 8, 7, 1.5, 1.5, "F");
  doc.setFont("helvetica", "bold").setFontSize(8.5).setTextColor(...WHITE);
  doc.text(titleCase(loan.status), sx2 + 4 + (sw - 8) / 2, y + 12, { align: "center" });
  doc.setFont("helvetica", "normal").setFontSize(7).setTextColor(...MGRAY);
  doc.text(`Generated: ${dt(new Date().toISOString())}`, sx2 + 4, y + 18);
  const gb = s(byName, ""); if (gb) doc.text(`By: ${gb}`, sx2 + 4, y + 21.5);

  y += 27;

  // ==========================================================================
  // 1. BORROWER DETAILS
  // ==========================================================================
  const mname = s(loan.member.fullName || `${loan.member.firstName} ${loan.member.lastName}`);
  y = heading("1. BORROWER DETAILS", 55, y);
  autoTable(doc, {
    startY: y, head: [],
    body: [
      ["Full Name", mname],
      ["Member Number", s(loan.member.memberNumber)],
      ["Loan Product", s(loan.loanProduct.name)],
      ["Purpose of Loan", s(loan.purpose, "Not specified")],
    ],
    theme: "plain",
    margin: { left: ML, right: 18 },
    styles: { fontSize: 8, cellPadding: { top: 2.5, bottom: 2.5, left: 3, right: 3 } },
    columnStyles: {
      0: { fontStyle: "bold", textColor: RED_MID, cellWidth: 45, fillColor: RED_LIGHT },
      1: { textColor: DARK, cellWidth: CW - 45 },
    },
  });
  y = (doc as any).lastAutoTable.finalY + 5;

  // ==========================================================================
  // 2. FINANCIAL DETAILS
  // ==========================================================================
  y = heading("2. LOAN FINANCIAL DETAILS", 65, y);
  const lw = 38, vw = CW / 2 - 38;
  autoTable(doc, {
    startY: y,
    head: [[
      { content: "Parameter", styles: { fontStyle: "bold" } },
      { content: "Value",     styles: { fontStyle: "bold" } },
      { content: "Parameter", styles: { fontStyle: "bold" } },
      { content: "Value",     styles: { fontStyle: "bold" } },
    ]],
    body: [
      ["Principal Amount",  KES(loan.amount),            "Total Interest",       loan.totalInterest ? KES(loan.totalInterest) : "Reducing balance"],
      ["Interest Rate",     `${s(loan.interestRate ?? loan.loanProduct.interestRate)}% p.a.`, "Total Repayable", loan.totalRepayable ? KES(loan.totalRepayable) : "Per schedule"],
      ["Loan Term",         `${loan.termMonths} months`, "Outstanding Balance",  KES(loan.outstandingBalance ?? loan.amount)],
      ["Monthly Repayment", loan.monthlyRepayment ? KES(loan.monthlyRepayment) : "Reducing balance", "Repayment Method", "Reducing Balance"],
    ],
    theme: "grid",
    margin: { left: ML, right: 18 },
    headStyles: { fillColor: RED, textColor: WHITE, fontSize: 7.5, halign: "left", cellPadding: 2.5 },
    styles: { fontSize: 7.5, cellPadding: 2.5, lineColor: BORDER, lineWidth: 0.15 },
    columnStyles: {
      0: { fontStyle: "bold", textColor: RED_MID, fillColor: RED_LIGHT, cellWidth: lw },
      1: { textColor: DARK, cellWidth: vw },
      2: { fontStyle: "bold", textColor: RED_MID, fillColor: RED_LIGHT, cellWidth: lw },
      3: { textColor: DARK },
    },
    alternateRowStyles: { fillColor: [255, 250, 251] as RGB },
  });
  y = (doc as any).lastAutoTable.finalY + 5;

  // ==========================================================================
  // 3. GUARANTORS
  // ==========================================================================
  if (all.length > 0) {
    y = heading("3. GUARANTORS", 38, y);

    const gColors: Record<string, RGB> = {
      ACTIVE: GREEN, ACCEPTED: GREEN, PENDING: ORANGE, REJECTED: [180, 30, 30], RELEASED: MGRAY,
    };

    autoTable(doc, {
      startY: y,
      head: [["#", "Guarantor Name", "Member No.", "Type", "Guarantee Amt.", "Frozen Pledge", "Cover%", "Status"]],
      body: all.map((g, i) => {
        const ga = g.guaranteeAmount ?? loan.amount;
        const fp = g.frozenPledge ?? g.pledgeAmount ?? ga;
        const pc = loan.amount > 0 ? `${((ga / loan.amount) * 100).toFixed(1)}%` : "—";
        return [String(i + 1), gName(g), gNo(g), g.selfGuarantee ? "Self" : "External", KES(ga), KES(fp), pc, titleCase(g.status)];
      }),
      theme: "striped",
      margin: { left: ML, right: 18 },
      headStyles: { fillColor: RED, textColor: WHITE, fontSize: 7, fontStyle: "bold", cellPadding: 2 },
      styles: { fontSize: 7, cellPadding: 2, lineColor: BORDER, lineWidth: 0.1, overflow: "linebreak" },
      // widths: 7+38+22+16+30+28+14+19 = 174 mm = CW
      columnStyles: {
        0: { cellWidth: 7,  halign: "center" },
        1: { cellWidth: 38 },
        2: { cellWidth: 22 },
        3: { cellWidth: 16, halign: "center" },
        4: { cellWidth: 30, halign: "right"  },
        5: { cellWidth: 28, halign: "right"  },
        6: { cellWidth: 14, halign: "center" },
        7: { cellWidth: 19, halign: "center" },
      },
      alternateRowStyles: { fillColor: [255, 249, 250] as RGB },
      didParseCell: data => {
        if (data.section === "body" && data.column.index === 7) {
          const g = all[data.row.index];
          if (g) { data.cell.styles.textColor = gColors[g.status.toUpperCase()] ?? MGRAY; data.cell.styles.fontStyle = "bold"; }
        }
      },
    });

    // Coverage summary – 2 rows, no overflow
    const tg = all.reduce((a, g) => a + (g.guaranteeAmount ?? loan.amount), 0);
    const tf = all.reduce((a, g) => a + (g.frozenPledge ?? g.pledgeAmount ?? 0), 0);
    const cp = loan.amount > 0 ? ((tg / loan.amount) * 100).toFixed(1) : "0.0";
    const ok = tg >= loan.amount;

    y = (doc as any).lastAutoTable.finalY + 3;
    y = need(20, y);

    doc.setFillColor(255, 244, 246).setDrawColor(...RED).setLineWidth(0.4);
    doc.roundedRect(ML, y, CW, 18, 2, 2, "FD");

    // Row 1 – two values side by side
    doc.setFont("helvetica", "bold").setFontSize(7.5).setTextColor(...DARK);
    doc.text(`Total Guaranteed: ${KES(tg)}`,      ML + 5, y + 6);
    doc.text(`Total Frozen Pledges: ${KES(tf)}`,  ML + CW / 2 + 3, y + 6);

    // Row 2 – coverage % left, badge right
    doc.setFont("helvetica", "normal").setFontSize(7).setTextColor(...MGRAY);
    doc.text(`Coverage: ${cp}% of Loan Principal`, ML + 5, y + 13);

    const bc: RGB = ok ? GREEN : ORANGE;
    doc.setFillColor(...bc);
    doc.roundedRect(MR - 52, y + 8.5, 50, 7, 1.5, 1.5, "F");
    doc.setFont("helvetica", "bold").setFontSize(7.5).setTextColor(...WHITE);
    doc.text(ok ? "FULLY COVERED" : "PARTIALLY COVERED", MR - 27, y + 13.5, { align: "center" });

    y += 22;
  } else {
    y += 5;
  }

  // ==========================================================================
  // 4. APPROVAL WORKFLOW TIMELINE
  // ==========================================================================
  y = need(55, y);
  y = heading("4. APPROVAL WORKFLOW", 55, y);

  const steps = [
    { label: "Application\nSubmitted",  key: "" },
    { label: "Guarantor\nApproval",     key: "PENDING_GUARANTOR_APPROVAL" },
    { label: "Loan Officer\nReview",    key: "PENDING_LOAN_OFFICER_REVIEW" },
    { label: "Credit\nCommittee",       key: "PENDING_CREDIT_COMMITTEE" },
    { label: "Treasury\nApproval",      key: "PENDING_TREASURER" },
    { label: "Loan\nApproved",          key: "APPROVED" },
    { label: "Loan\nDisbursed",         key: "DISBURSED" },
  ];
  const order = [
    "PENDING", "PENDING_GUARANTOR_APPROVAL", "PENDING_GUARANTOR_REPLACEMENT",
    "PENDING_GUARANTOR_REASSIGNMENT", "PENDING_LOAN_OFFICER_REVIEW",
    "PENDING_CREDIT_COMMITTEE", "PENDING_TREASURER", "APPROVED", "DISBURSED", "REPAID",
  ];
  const ci = order.indexOf(loan.status);
  const state = (i: number): "done" | "cur" | "todo" => {
    if (i === 0) return "done";
    const ki = order.indexOf(steps[i].key);
    if (!steps[i].key) return "done";
    return ci > ki ? "done" : ci === ki ? "cur" : "todo";
  };

  const sw2  = CW / steps.length;
  const tlY  = y + 10;
  const CR   = 4.5;

  // connector lines
  for (let i = 0; i < steps.length - 1; i++) {
    const x1 = ML + i * sw2 + sw2 / 2 + CR;
    const x2 = ML + (i + 1) * sw2 + sw2 / 2 - CR;
    const col: RGB = state(i) === "done" ? RED : [210, 205, 210];
    doc.setDrawColor(...col).setLineWidth(0.8);
    doc.line(x1, tlY, x2, tlY);
  }

  // circles and labels
  steps.forEach((step, i) => {
    const cx = ML + i * sw2 + sw2 / 2;
    const st = state(i);

    const fill:   RGB = st === "done" ? RED : st === "cur" ? RED_DARK : [225, 218, 220];
    const stroke: RGB = st === "done" ? RED_DARK : st === "cur" ? RED : [190, 183, 186];
    doc.setFillColor(...fill).setDrawColor(...stroke).setLineWidth(0.4);
    doc.circle(cx, tlY, CR, "FD");

    // Inner ring for current step
    if (st === "cur") {
      doc.setDrawColor(...WHITE).setLineWidth(0.35);
      doc.circle(cx, tlY, CR - 1.3, "S");
    }

    // Draw the step indicator inside the circle.
    // jsPDF built-in fonts (helvetica) cannot render Unicode symbols like U+2713 (✓)
    // — they silently render as apostrophes.  Use plain ASCII only.
    // For "done" steps: draw a small solid white dot to signal completion.
    // For all steps: print the step number in white so it's always legible.
    if (st === "done") {
      // Small solid white dot at circle centre
      doc.setFillColor(...WHITE);
      doc.circle(cx, tlY, 1.4, "F");
    }
    // Always print the step number — for "done" it sits over the dot, for others it's the main label
    doc.setFont("helvetica", "bold")
       .setFontSize(7)
       .setTextColor(...WHITE);
    doc.text(String(i + 1), cx, tlY + 2.5, { align: "center" });

    // Label below
    const lc: RGB = st === "done" ? RED_DARK : st === "cur" ? RED : MGRAY;
    doc.setFont("helvetica", st === "cur" ? "bold" : "normal")
       .setFontSize(5.6)
       .setTextColor(...lc);
    const ll = doc.splitTextToSize(step.label, sw2 - 1) as string[];
    doc.text(ll, cx, tlY + CR + 4, { align: "center" });
  });

  y = tlY + CR + 18;

  // ==========================================================================
  // 5. TERMS & CONDITIONS
  // ==========================================================================
  y = need(30, y);
  y = heading("5. TERMS AND CONDITIONS", 60, y);

  doc.setFont("helvetica", "normal").setFontSize(7.5).setTextColor(...DARK);
  [
    "1. This loan offer is subject to acceptance within 14 days from the date of issue.",
    "2. Disbursement will be made to the member's registered bank account on record with Minet SACCO.",
    `3. The loan carries an interest rate of ${s(loan.interestRate ?? loan.loanProduct.interestRate)}% per annum, calculated on a reducing balance basis.`,
    "4. Monthly repayments are due on or before the 5th of each calendar month.",
    "5. Late payments attract a penalty of 2% of the overdue amount per month.",
    "6. Guarantors' savings pledges remain frozen until the loan is fully repaid or released.",
    "7. Early repayment is permitted without penalty; outstanding interest accrued to date remains payable.",
    "8. Any default after three (3) consecutive missed payments shall trigger recovery proceedings against the borrower and guarantors.",
    "9. The SACCO reserves the right to set off the loan balance against any savings/shares balance of the borrower.",
    "10. This document is legally binding upon acceptance by signature of the borrower and guarantors.",
  ].forEach(t => {
    const lines = doc.splitTextToSize(t, CW) as string[];
    y = need(lines.length * 5 + 1, y);
    doc.text(lines, ML, y);
    y += lines.length * 5;
  });
  y += 5;

  // ==========================================================================
  // 6. SIGNATURES
  // ==========================================================================
  y = need(120, y);
  y = heading("6. DECLARATION AND SIGNATURES", 72, y);

  doc.setFont("helvetica", "italic").setFontSize(7.5).setTextColor(80, 50, 55);
  const decl = doc.splitTextToSize(
    "I/We, the undersigned, hereby acknowledge the terms and conditions of the above loan " +
    "and agree to abide by the rules and regulations of Minet SACCO Society Limited. " +
    "I/We confirm that all information provided in the loan application is true and accurate.",
    CW
  ) as string[];
  doc.text(decl, ML, y);
  y += decl.length * 4.5 + 6;

  // Borrower – full width
  y = need(46, y);
  sigBox("Borrower", mname, "Loan Applicant / Borrower", s(loan.member.memberNumber), ML, y, CW);
  y += 46;

  // Guarantors (up to 3 per row)
  if (all.length > 0) {
    for (let r = 0; r < all.length; r += 3) {
      const row = all.slice(r, r + 3);
      const cols2 = Math.min(row.length, 3);
      const bw = (CW - (cols2 - 1) * 5) / cols2;
      y = need(46, y);
      row.forEach((g, i) =>
        sigBox(
          `Guarantor ${r + i + 1}${g.selfGuarantee ? " (Self)" : ""}`,
          gName(g), g.selfGuarantee ? "Self-Guarantee" : "External Guarantor",
          gNo(g), ML + i * (bw + 5), y, bw
        )
      );
      y += 46;
    }
  }

  // Officials
  y = need(46, y);
  const ow = (CW - 10) / 2;
  sigBox("Loan Officer",
    s(loan.createdBy, "Authorized Loan Officer"), "Loan Processing Officer", undefined, ML, y, ow);
  sigBox("Treasurer",
    s(loan.approvedBy, "Authorized Treasurer"), "Minet SACCO Treasurer", undefined, ML + ow + 10, y, ow);
  y += 46;

  // Official seal block
  y = need(28, y);
  doc.setFillColor(255, 247, 249).setDrawColor(...RED).setLineWidth(0.35);
  doc.roundedRect(ML, y, CW, 22, 2, 2, "FD");
  doc.setFont("helvetica", "bold").setFontSize(8).setTextColor(...RED);
  doc.text("FOR OFFICIAL USE ONLY — MINET SACCO SOCIETY LIMITED", PW / 2, y + 6, { align: "center" });
  doc.setFont("helvetica", "normal").setFontSize(7).setTextColor(80, 50, 55);
  doc.text(
    "This document has been duly processed and approved in accordance with the Minet SACCO lending policy.",
    PW / 2, y + 12, { align: "center" }
  );
  doc.text(
    `Reference: ${s(loan.loanNumber, `LOAN-${loan.id}`)}  |  Generated: ${new Date().toLocaleString("en-KE")}`,
    PW / 2, y + 17, { align: "center" }
  );
  // circular stamp
  const scx = MR - 18, scy = y + 11;
  doc.setDrawColor(...RED).setLineWidth(0.6); doc.circle(scx, scy, 9, "S");
  doc.setDrawColor(...GOLD).setLineWidth(0.3); doc.circle(scx, scy, 7.5, "S");
  doc.setFontSize(4.5).setTextColor(...RED);
  doc.text("MINET SACCO",  scx, scy - 1.5, { align: "center" });
  doc.text("OFFICIAL SEAL", scx, scy + 2,   { align: "center" });

  footers();
  return doc;
};

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/** Download the PDF to disk. */
export const generateLoanPdf = async (
  loan: PdfLoan, guarantors: PdfGuarantor[] = [], byName?: string
): Promise<void> => {
  const doc = await buildDoc(loan, guarantors, byName);
  const ref = s(loan.loanNumber, `LOAN-${loan.id}`).replace(/[^a-zA-Z0-9_-]/g, "_");
  const d   = new Date().toISOString().slice(0, 10);
  doc.save(`Minet_SACCO_Loan_${ref}_${d}.pdf`);
};

/** Open in a new tab and trigger the browser print dialog. */
export const printLoanPdf = async (
  loan: PdfLoan, guarantors: PdfGuarantor[] = [], byName?: string
): Promise<void> => {
  const doc = await buildDoc(loan, guarantors, byName);
  const url = doc.output("bloburl") as unknown as string;
  const win = window.open(url, "_blank");
  if (win) {
    win.addEventListener("load", () => {
      try { win.focus(); win.print(); } catch { /* user can print manually */ }
    });
  }
};
