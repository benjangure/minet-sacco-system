# Minet Sacco - Comprehensive System Summary

## Executive Overview

**Minet Sacco** is a full-stack digital financial management platform designed for Savings and Credit Cooperative Organizations (SACCOs) in Kenya. It manages member savings, loans, guarantees, repayments, and regulatory compliance while enforcing Kenyan SACCO standards.

**Technology Stack:**
- **Backend**: Spring Boot 3.x, Java 17, MySQL 8.0
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS
- **Reporting**: Apache POI (Excel), iText (PDF), Flyway (migrations)

---

## System Architecture

### High-Level Layers

```
┌─────────────────────────────────────────────────────────┐
│  Presentation (React SPA + Mobile via Capacitor)        │
├─────────────────────────────────────────────────────────┤
│  API (REST Controllers)                                 │
├─────────────────────────────────────────────────────────┤
│  Business Logic (Services)                              │
├─────────────────────────────────────────────────────────┤
│  Data Access (JPA Repositories)                         │
├─────────────────────────────────────────────────────────┤
│  Database (MySQL)                                       │
└─────────────────────────────────────────────────────────┘
```

---

## Core Modules

### 1. Loan Management Module

**Purpose**: Complete loan lifecycle management (application → repayment → closure)

**Key Components**:
- `LoanService` — Create, update, repay loans
- `LoanMigrationService` — Import legacy loans with validation
- `LoanDisbursementService` — Disburse and activate guarantees
- `LoanNumberGenerationService` — Generate unique loan numbers

**Guarantorship Types**:

| Type | Self-Guarantee | External | Use Case |
|------|---|---|---|
| NORMAL | ❌ | ✅ 1-5 | Standard loans with pledges |
| SELF | ✅ 100% | ❌ | Borrower self-guarantees entire amount |
| PARTIAL | ✅ Part | ✅ 1-5 | Borrower covers part + others cover rest |

**Key Features**:
- ✅ Flexible repayment split (total amount only, auto-calculates principal/interest)
- ✅ Reducing-balance outstanding balance method
- ✅ Pledge reduction as loans are repaid
- ✅ Savings freezing for guarantors (pledged amount only)
- ✅ Bulk migration from legacy systems
- ✅ Loan eligibility calculation
- ✅ Interest accrual and GL reconciliation

**Repayment Scenarios** (AUTO-CALCULATED):
1. Total only → 100% principal, 0% interest
2. Total + Principal → Interest = Total - Principal
3. Total + Interest → Principal = Total - Interest
4. Principal + Interest → Total auto-summed
5. All three → Validates sum equals total

---

### 2. Member Management Module

**Purpose**: Manage member profiles, savings, and lifecycle

**Key Components**:
- `MemberService` — CRUD operations
- `MemberExitService` — Handle exits with payouts
- `Member` Entity — Status tracking (ACTIVE, SUSPENDED, EXITED)
- `Account` Entity — Savings account with frozen balance

**Key Features**:
- ✅ Member registration and KYC
- ✅ Member suspension/exit workflow
- ✅ Payout calculation on exit
- ✅ Monthly contribution tracking
- ✅ Savings freezing by guarantor pledges
- ✅ Status transitions with audit

---

### 3. Guarantor & Pledge Management

**Purpose**: Manage guarantor eligibility, pledges, and savings freezing

**Key Components**:
- `GuarantorValidationService` — Pledge validation
- `GuarantorReportService` — Over-committed detection
- `Guarantor` Entity — Pledge tracking and status

**Key Features**:
- ✅ Guarantor pledge validation (sum = principal)
- ✅ Savings freezing (pledged amount only)
- ✅ Over-committed detection (liabilities > savings)
- ✅ Pledge reduction on repayment
- ✅ Reallocation on member exit
- ✅ Self-guarantee vs. external distinction

**Critical Rule**: Frozen Savings = Pledged Amount (not full principal)

---

### 4. Loan Repayment Module

**Purpose**: Record flexible repayments with auto-split

**Key Features**:
- ✅ Total repayment only (mandatory)
- ✅ Auto-calculate principal/interest split
- ✅ 5 scenarios supported (see above)
- ✅ Payment method tracking
- ✅ Partial repayment support
- ✅ GL transaction creation
- ✅ Pledge reduction on repayment
- ✅ Reducing-balance outstanding tracking

**Outstanding Balance Calculation**:
```
Current Method: SubTRAct current repayment's principal from existing balance
Outstanding = Existing Outstanding - Principal Repaid (This Repayment)
This correctly reflects reducing-balance method
```

---

### 5. Bulk Processing Module

**Purpose**: Handle Excel-based loan migrations and monthly uploads

**Key Components**:
- `BulkProcessingService` — Process batch operations
- `BulkValidationService` — Validate each row
- `ExcelParserService` — Parse Excel files

**Key Features**:
- ✅ Row-by-row validation
- ✅ CREATE mode (new loans)
- ✅ UPDATE mode (existing loans)
- ✅ DELETE mode (retire loans)
- ✅ Per-row error reporting
- ✅ Transaction rollback on failures
- ✅ Batch audit logging

**CREATE Mode** - Required Fields:
- Employee ID
- Loan Amount
- Term (months)
- Interest Rate
- Disbursement Date
- Loan Status
- Guarantorship Type
- At least 1 guarantor with pledge

**UPDATE Mode** - Required Fields:
- Loan Number (identifies loan)
- All other fields optional
- Only validates fields that are provided

**Templates**:
- Includes Excel generation with proper validation rules
- PARTIAL type requires: Col 1 = borrower, Cols 2-6 = external guarantors

---

### 6. GL Accounting Module

**Purpose**: Track GL accounts and reconciliation

**Key Components**:
- `GLCalculationService` — Calculate GL entries
- `GLManualEntryService` — Create manual entries
- `BalanceSheetService` — Calculate balance sheet
- `IncomeStatementService` — Calculate income statement

**Key Features**:
- ✅ Automatic GL entries on transactions
- ✅ Manual GL entry support
- ✅ Balance sheet generation
- ✅ Income statement generation
- ✅ Reconciliation reporting
- ✅ Audit trail for all GL changes

---

### 7. Reporting & Export Module

**Purpose**: Generate reports and export data

**Key Components**:
- `ReportsService` — Query and aggregate data
- `ReportExportService` — Format exports
- `GuarantorReportService` — Guarantor-specific reports

**Export Formats**:
- ✅ Excel (Apache POI)
- ✅ PDF (iText)
- ✅ CSV

**Reports Available**:
- Loan portfolio summary
- Member accounts
- Guarantor commitments
- Over-committed guarantors
- GL accounts
- Balance sheet
- Income statement
- Audit trail
- Monthly contribution tracking

---

### 8. Notification & Audit Module

**Purpose**: Communications and audit logging

**Key Components**:
- `NotificationService` — Email notifications
- `AuditService` — Audit logging
- `EmailService` — Email delivery

**Audit Events Tracked**:
- Login/logout
- Loan creation/update/deletion
- Repayment recording
- Member exit
- Guarantor changes
- GL transactions
- Bulk processing
- Configuration changes

---

## Database Schema

### Key Entities

| Table | Purpose | Key Fields |
|-------|---------|-----------|
| `loans` | Loan records | amount, principal, outstanding_balance, status, loan_number |
| `members` | Member profiles | member_number, phone, status |
| `guarantors` | Pledge records | guarantee_amount, pledge_amount, status, is_self_guarantee |
| `accounts` | Savings accounts | balance, frozen_balance |
| `transactions` | Account movements | amount, type (LOAN_DISBURSEMENT, REPAYMENT, etc.) |
| `gl_accounts` | GL ledger entries | debit, credit, reconciled |
| `notifications` | Email log | recipient, subject, status |
| `audit_logs` | Audit trail | action, changed_by, timestamp |

---

## Data Flow Diagrams

### Loan Creation & Disbursement

```
Loan Application
    ↓
Eligibility Check (savings, guarantor capacity)
    ↓
Approval by Loan Officer
    ↓
Guarantor Assignment (NORMAL/SELF/PARTIAL)
    ↓
Savings Freezing (guarantor pledges)
    ↓
Loan Disbursement
    ↓
GL Accounting (debit member account, credit loan liability)
    ↓
Notification (borrower + guarantors)
```

### Loan Repayment

```
Repayment Amount Submitted
    ↓
Validate & Auto-Calculate Split
    ↓
Update Outstanding Balance (reducing-balance)
    ↓
Reduce Guarantor Pledges
    ↓
Unfreeze Proportional Savings
    ↓
GL Accounting (debit member account, credit income)
    ↓
Check if Fully Repaid
    ├─ Yes: Release All Pledges, Update Status
    └─ No: Continue active
    ↓
Notification (borrower + loan officer)
```

### Bulk Loan Migration

```
Upload Excel File
    ↓
Parse Excel → LoanMigrationItem records
    ↓
Row-by-Row Validation
    ├─ CREATE mode: validate fields + guarantors
    ├─ UPDATE mode: validate updates + guarantor changes
    └─ DELETE mode: check dependencies
    ↓
Process Valid Rows
    ├─ Create/Update Loan
    ├─ Setup Guarantors (NORMAL/SELF/PARTIAL)
    ├─ Freeze Savings
    └─ Create GL Entries
    ↓
Generate Report (success/failure per row)
    ↓
Notification (processor with summary)
```

---

## Key Business Rules

### 1. Savings Freezing
- **Rule**: Frozen savings = pledged amount (NOT full principal)
- **Application**: All guarantor types (NORMAL, SELF, PARTIAL)
- **Release**: Proportional as loan is repaid
- **Full Release**: When loan is fully repaid

### 2. Pledge Validation
- **Sum Rule**: All guarantor pledges must equal loan principal exactly
- **No Overpledge**: Pledges cannot exceed principal
- **No Underpledge**: Pledges cannot be less than principal
- **Per Guarantor**: Individual pledge > 0 and ≤ their savings

### 3. Eligibility Calculation
- **Member Savings**: Must have minimum savings (configurable)
- **Guarantor Capacity**: Sum(guarantor savings) ≥ loan amount
- **Status Check**: Member must be ACTIVE
- **Freeze Check**: Unfrozen savings ≥ required guarantor pledge

### 4. Outstanding Balance
- **Calculation**: Subtract principal repaid from previous balance
- **Method**: Reducing-balance (not recalculation from total historical)
- **Cap**: Cannot go negative (set to 0 if needed)

### 5. Repayment Split
- **Total Amount**: Mandatory
- **Principal/Interest**: Flexible (auto-calculated if missing)
- **5 Scenarios**: See Loan Repayment Module above
- **Validation**: No negative amounts, sum validation

### 6. Member Exit
- **Calculation**: Full savings - frozen pledges + proportional loan interest
- **Guarantor Reassignment**: Pledges must be reallocated or loan unsecured
- **Status**: ACTIVE → EXITED
- **Processing**: Irreversible after confirmation

---

## API Endpoints (Key)

### Loan Management
- `POST /api/loans` — Create loan
- `GET /api/loans` — List loans
- `PUT /api/loans/{id}` — Update loan
- `POST /api/loans/{id}/repayment` — Record repayment
- `POST /api/loans/{id}/approve` — Approve loan
- `POST /api/loans/{id}/disburse` — Disburse loan

### Bulk Operations
- `POST /api/bulk-processing/upload` — Upload Excel
- `GET /api/bulk-processing/batch/{batchId}` — Get batch results
- `GET /api/bulk-processing/template` — Download template

### Members
- `POST /api/members` — Register member
- `GET /api/members` — List members
- `PUT /api/members/{id}` — Update member
- `POST /api/members/{id}/exit` — Process exit

### Reports
- `GET /api/reports/loans` — Loan report
- `GET /api/reports/guarantors` — Guarantor report
- `GET /api/reports/over-committed` — Over-committed guarantors
- `GET /api/reports/balance-sheet` — Balance sheet
- `GET /api/reports/income-statement` — Income statement
- `GET /api/reports/export` — Export (Excel/PDF)

### GL Accounting
- `POST /api/gl/manual-entry` — Create manual GL entry
- `GET /api/gl/accounts` — List GL accounts
- `GET /api/gl/reconciliation` — Reconciliation report

---

## Frontend Components (React)

### Main Pages
- **Dashboard** — Overview of active loans, members, alerts
- **Loan Management** — Create, view, repay loans
- **Loan Migration** — Bulk upload and progress tracking
- **Member Management** — Register, suspend, exit members
- **Reports** — View and export reports
- **GL Configuration** — Manage GL accounts and reconciliation
- **Audit Trail** — View system activities
- **Admin** — User management, configuration

### Key Features
- Role-based access control (Admin, Treasurer, Loan Officer, Member)
- Real-time notifications
- PDF/Excel export
- Responsive design (mobile-friendly)
- Authentication via session tokens
- Protected routes with automatic redirect

---

## Recent Fixes & Features

### ✅ COMPLETED

1. **Infinite Redirect Loop Fix**
   - Fixed auth flow with proper `useEffect` dependency arrays
   - Resolved "Maximum update depth exceeded" errors

2. **PARTIAL Guarantorship Type**
   - Implemented in `LoanMigrationService`
   - Allows borrower to self-guarantee part + external guarantors cover rest
   - Sum validation: all pledges = principal exactly
   - Full processing pipeline: CREATE, UPDATE, DELETE

3. **Auto-Split Repayment Feature**
   - Flexible repayment with only total mandatory
   - 5 scenarios auto-calculated:
     1. Total only → 100% principal
     2. Total + principal → calculates interest
     3. Total + interest → calculates principal
     4. Principal + interest → auto-sums total
     5. All three → validates sum
   - Implemented in `LoanService.recordLoanRepayment()`

4. **Self-Guarantee Freeze Amount Fix**
   - Fixed `LoanDisbursementService.updateGuarantorStatusToActive()`
   - Now uses actual pledged amount (supports PARTIAL self-guarantees)
   - Fallback to full principal for legacy data

5. **Reducing-Balance Outstanding Calculation**
   - Changed from historical recalculation to incremental updates
   - Each repayment: Outstanding -= Principal Repaid
   - Correct method for SACCO accounting

---

## Deployment Status

### Ready for Deployment
- ✅ PARTIAL guarantorship type
- ✅ Auto-split repayment
- ✅ Self-guarantee freeze fix
- ✅ Infinite redirect loop fix
- ✅ Outstanding balance reducing-balance method

### Testing Completed
- Row-by-row validation
- Bulk processing
- GL reconciliation
- Repayment split scenarios
- Pledge reduction

### Known Limitations
- None currently blocking deployment

---

## Performance Considerations

### Database Optimization
- Indexes on: member_number, loan_number, loan_status, member_status
- Pagination on list endpoints (default 50 records)
- Connection pooling (HikariCP)

### Caching Strategy
- Member data cached during session
- Loan product config cached (cleared on update)
- GL account hierarchy cached

### Bulk Processing
- Transactional per batch (rollback on failure)
- Configurable batch size (default 100 rows)
- Progress tracking via WebSocket updates

---

## Security Features

### Authentication
- Session token-based (stored in localStorage)
- Automatic timeout (30 minutes)
- Login rate limiting
- Password hashing (bcrypt)

### Authorization
- Role-based access control (RBAC)
- Field-level permissions
- Audit logging of all changes
- Admin-only endpoints

### Data Protection
- HTTPS for all traffic
- SQL injection prevention (parameterized queries)
- CSRF token on state-changing operations
- XSS protection in React (auto-escaping)

---

## Module Dependencies

### Dependency Graph

```
Controllers (API)
    ↓
Services (Business Logic)
    ├─ LoanService
    ├─ LoanMigrationService (depends on LoanService, GuarantorValidationService)
    ├─ LoanDisbursementService
    ├─ MemberService
    ├─ BulkProcessingService (depends on LoanMigrationService, NotificationService)
    ├─ ReportsService
    └─ GLCalculationService
    ↓
Repositories (Data Access)
    ├─ LoanRepository
    ├─ MemberRepository
    ├─ GuarantorRepository
    ├─ AccountRepository
    └─ TransactionRepository
    ↓
Database (MySQL)
```

### Critical Dependencies (No Circular)
- All upward dependencies only (services → repositories → DB)
- No circular service dependencies
- Clear separation of concerns

---

## Troubleshooting Guide

### Issue: Loan Won't Disburse
**Check**: Guarantor savings, freeze validations, GL account config
**Fix**: Verify guarantor savings ≥ pledge, check GL defaults

### Issue: Repayment Won't Calculate
**Check**: Principal + interest match total, no negatives
**Fix**: Validate input amounts, check for data truncation

### Issue: Member Exit Fails
**Check**: Outstanding loans, active guarantorships
**Fix**: Repay all loans first, reallocate guarantor pledges

### Issue: Bulk Upload Hangs
**Check**: Excel file size, row count, network timeout
**Fix**: Split into smaller batches, check MySQL max_allowed_packet

### Issue: GL Entries Unbalanced
**Check**: Debit/credit logic, rounding
**Fix**: Run reconciliation report, check for posting errors

---

## Maintenance Tasks

### Daily
- Monitor batch processing queue
- Check for failed repayments
- Review audit trail for anomalies

### Weekly
- Generate GL reconciliation report
- Verify savings freeze accuracy
- Check for over-committed guarantors

### Monthly
- Full GL reconciliation
- Member contribution tracking
- Backup database

### Quarterly
- Performance optimization review
- Security audit
- Compliance check with SACCO regulations

---

## Future Enhancements

### Planned Features
- SMS notifications (in addition to email)
- Mobile app (iOS/Android via Capacitor)
- API rate limiting dashboard
- Advanced reporting (custom queries)
- Installment-based repayments
- Loan top-ups
- Delinquency management
- Collections workflow

### Infrastructure
- Database replication for HA
- API caching layer (Redis)
- Background job queue (for async processing)
- Monitoring/alerting (Prometheus/Grafana)

---

## Contact & Support

**Project**: Minet Sacco
**Version**: 1.0.0
**Last Updated**: July 6, 2026
**Maintained By**: Development Team
