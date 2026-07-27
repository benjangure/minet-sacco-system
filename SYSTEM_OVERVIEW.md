# Minet SACCO - System Overview

## Current System Status (April 2026)

This document provides an overview of the Minet SACCO system as currently implemented, including all recent features and improvements.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Shared Backend API                         │
│              (Spring Boot REST Endpoints)                   │
│                  PostgreSQL Database                         │
└─────────────────────────────────────────────────────────────┘
         ▲                                    ▲
         │                                    │
    ┌────┴─────────────────────────────┬─────┴────┐
    │                                  │          │
┌───▼────────────┐          ┌──────────▼──┐  ┌───▼──────────┐
│  Member Portal │          │ Android APK │  │ Staff Portal │
│  (React)       │          │ (Capacitor) │  │  (React)     │
│ localhost:3000 │          │ Mobile App  │  │ localhost:3000
│ /member        │          │             │  │ /dashboard
└────────────────┘          └─────────────┘  └──────────────┘
```

---

## Core Features

### 1. Member Management ✅

**Features:**
- Manual member registration (staff portal)
- Bulk member registration via Excel upload
- Member approval workflow
- Member status tracking (PENDING, ACTIVE, SUSPENDED, EXITED)
- Member KYC document upload
- Member profile management

**Access:** ADMIN, TREASURER, LOAN_OFFICER, TELLER

**Files:**
- `backend/src/main/java/com/minet/sacco/service/MemberService.java`
- `minetsacco-main/src/pages/Members.tsx`

---

### 2. Savings Management ✅

**Features:**
- Deposit to savings account (M-Pesa or bank transfer)
- Withdrawal from savings account
- Deposit approval workflow (for bank transfers)
- Account balance tracking
- Transaction history
- Minimum balance enforcement

**Account Types:**
- SAVINGS - Main account for deposits/withdrawals
- SHARES - Dormant account (no deposits allowed)
- CONTRIBUTIONS - Monthly contributions

**Access:** MEMBER (self-service), TELLER (approval), TREASURER (processing)

**Files:**
- `backend/src/main/java/com/minet/sacco/service/AccountService.java`
- `minetsacco-main/src/pages/Savings.tsx`

---

### 3. Loan Management ✅

**Features:**
- Loan product configuration (interest rate, term, amount limits)
- Member loan application
- Loan officer application on behalf of member ✨ NEW
- Live member eligibility checking
- Guarantor management (up to 3 guarantors)
- Guarantor employee ID lookup ✨ NEW
- Live guarantor eligibility checking ✨ NEW
- Total guarantee amount validation ✨ NEW
- Loan approval workflow (LOAN_OFFICER → CREDIT_COMMITTEE → TREASURER)
- Loan disbursement
- Loan repayment (member or bulk)
- Loan status tracking

**Loan Statuses:**
- PENDING - Initial application
- PENDING_GUARANTOR_APPROVAL - Waiting for guarantor approval
- PENDING_LOAN_OFFICER_REVIEW - Loan officer review
- PENDING_CREDIT_COMMITTEE - Credit committee approval
- PENDING_TREASURER - Treasurer disbursement
- APPROVED - Approved, ready for disbursement
- REJECTED - Rejected
- DISBURSED - Funds disbursed to member
- ACTIVE - Loan in repayment
- FULLY_PAID - Loan repaid
- DEFAULTED - Loan in default
- WRITTEN_OFF - Loan written off

**Access:** MEMBER (apply), LOAN_OFFICER (process/apply on behalf), CREDIT_COMMITTEE (approve), TREASURER (disburse)

**Files:**
- `backend/src/main/java/com/minet/sacco/service/LoanService.java`
- `backend/src/main/java/com/minet/sacco/controller/LoanController.java`
- `minetsacco-main/src/pages/Loans.tsx`
- `minetsacco-main/src/pages/MemberLoanApplication.tsx`

---

### 4. Guarantor Management ✅

**Features:**
- Guarantor eligibility validation
- Guarantor approval/rejection workflow
- Guarantor savings freezing (when loan approved)
- Guarantor savings release (when loan repaid)
- Guarantor tracking and history
- Multiple guarantor support (up to 3 per loan)

**Guarantor Statuses:**
- PENDING - Waiting for approval
- ACCEPTED - Approved
- REJECTED - Rejected
- ACTIVE - Loan active, savings frozen
- DECLINED - Declined
- RELEASED - Loan repaid, savings released

**Access:** GUARANTOR (approve/reject), LOAN_OFFICER (manage), CREDIT_COMMITTEE (review)

**Files:**
- `backend/src/main/java/com/minet/sacco/service/GuarantorTrackingService.java`
- `backend/src/main/java/com/minet/sacco/service/GuarantorValidationService.java`
- `minetsacco-main/src/pages/MyGuarantees.tsx`

---

### 5. Bulk Processing ✅

**Features:**
- Bulk member registration via Excel
- Bulk loan application via Excel
- Bulk loan repayment via Excel
- Validation reporting
- Batch processing with error handling
- Transaction rollback on errors

**Supported Operations:**
- Member registration (name, phone, email, DOB, ID, address, occupation)
- Loan applications (member phone, product, amount, guarantor)
- Loan repayments (member phone, loan ID, amount)

**Access:** ADMIN, TREASURER, LOAN_OFFICER

**Files:**
- `backend/src/main/java/com/minet/sacco/service/BulkProcessingService.java`
- `backend/src/main/java/com/minet/sacco/service/ExcelParserService.java`
- `minetsacco-main/src/pages/BulkProcessing.tsx`

---

### 6. Loan Eligibility Rules ✅

**Features:**
- Configurable eligibility rules
- Minimum savings balance requirement
- Minimum contribution period requirement
- Maximum loan multiple (of savings)
- Guarantor requirements
- Dynamic eligibility calculation

**Rules Configured:**
- Minimum savings: 1,000 KES
- Minimum contribution period: 3 months
- Maximum loan multiple: 3x savings
- Guarantor requirement: Yes (up to 3)

**Access:** ADMIN (configure), LOAN_OFFICER (view), CREDIT_COMMITTEE (view)

**Files:**
- `backend/src/main/java/com/minet/sacco/service/EligibilityCalculationService.java`
- `minetsacco-main/src/pages/LoanEligibilityRules.tsx`

---

### 7. Reports & Analytics ✅

**Features:**
- Profit & Loss report
- Member reports
- Loan portfolio report
- Loan performance report
- Transaction history report
- Export to Excel

**Reports Available:**
- P&L by period (monthly/yearly)
- Member list with status
- Member contributions
- Loan portfolio by status
- Loan repayment rates
- Overdue loans
- Transaction history

**Access:** ADMIN, TREASURER, AUDITOR

**Files:**
- `backend/src/main/java/com/minet/sacco/controller/ReportsController.java`
- `minetsacco-main/src/pages/Reports.tsx`
- `minetsacco-main/src/pages/ProfitLossReport.tsx`

---

### 8. Audit Trail ✅

**Features:**
- Complete audit logging of all actions
- User action tracking
- Entity change tracking (old value vs new value)
- Timestamp and IP address logging
- Audit trail filtering and search
- Audit report generation

**Logged Actions:**
- User login/logout
- Member registration/approval
- Account deposits/withdrawals
- Loan applications/approvals/disbursements
- Loan repayments
- User role changes
- Data modifications
- Bulk processing operations

**Access:** AUDITOR (view), ADMIN (view)

**Files:**
- `backend/src/main/java/com/minet/sacco/service/AuditService.java`
- `minetsacco-main/src/pages/AuditTrail.tsx`

---

### 9. Notifications ✅

**Features:**
- Real-time notifications
- Email notifications
- SMS notifications (M-Pesa)
- Notification history
- Notification read/unread tracking
- Personalized notification messages

**Notification Types:**
- Member approval
- Loan application received
- Loan approved/rejected
- Loan disbursed
- Loan repayment received
- Guarantor approval request
- Guarantor approval/rejection
- Deposit approved
- Withdrawal processed

**Access:** All users (receive), ADMIN (send)

**Files:**
- `backend/src/main/java/com/minet/sacco/service/NotificationService.java`
- `minetsacco-main/src/components/NotificationBell.tsx`

---

### 10. Member Portal ✅

**Features:**
- Member dashboard with account summary
- Loan application
- Loan repayment
- Savings deposit/withdrawal
- Transaction history
- Loan balance tracking
- Guarantor management
- Account statement

**Access:** MEMBER role only

**Files:**
- `minetsacco-main/src/pages/MemberDashboard.tsx`
- `minetsacco-main/src/pages/MemberLoanApplication.tsx`
- `minetsacco-main/src/pages/MyGuarantees.tsx`

---

### 11. Staff Portal ✅

**Features:**
- Staff dashboard with quick stats
- Member management
- Loan management
- Savings management
- Bulk processing
- Reports & analytics
- Audit trail
- User management
- System settings

**Access:** ADMIN, TREASURER, LOAN_OFFICER, CREDIT_COMMITTEE, TELLER, AUDITOR

**Files:**
- `minetsacco-main/src/pages/Dashboard.tsx`
- `minetsacco-main/src/components/AppSidebar.tsx`

---

### 12. Android Mobile App ✅

**Features:**
- Member portal on mobile
- Same functionality as web portal
- Offline support (limited)
- Push notifications
- Secure token storage
- Biometric login (optional)

**Technology:** Capacitor (React wrapped in native Android)

**Files:**
- `android/app/src/main/AndroidManifest.xml`
- `android/app/build.gradle`
- `capacitor.config.ts`

---

## Recent Enhancements (Current Sprint)

### Loan Officer Application Feature ✨

**What's New:**
- Loan officers can apply for loans on behalf of members
- Member selection dropdown
- Live member eligibility checking
- Guarantor employee ID search
- Live guarantor eligibility checking
- Total guarantee amount validation
- Prevents guarantor amount from exceeding loan amount

**Benefits:**
- Faster loan processing
- Better guarantor selection
- Real-time eligibility feedback
- Reduced errors

**Files Modified:**
- `minetsacco-main/src/pages/Loans.tsx` - New loan officer interface
- `backend/src/main/java/com/minet/sacco/service/LoanService.java` - Eligibility validation
- `backend/src/main/java/com/minet/sacco/controller/LoanController.java` - New endpoints

---

### Guarantor Rejection Handling ✨

**What's New:**
- When guarantor rejects, borrower gets 3 options:
  1. Replace Guarantor - Find new guarantor
  2. Reduce Loan Amount - Reduce to match remaining guarantees
  3. Withdraw Application - Cancel and reapply later

**Benefits:**
- Loan doesn't get stuck
- Clear path forward for borrower
- Faster resolution
- Better member experience

**Status:** Planned for next sprint

**Documentation:** `GUARANTOR_REJECTION_HANDLING.md`

---

## User Roles & Permissions

### Role Hierarchy

```
ADMIN (Full Access)
├── TREASURER (Accounting & Disbursement)
├── LOAN_OFFICER (Loan Processing)
├── CREDIT_COMMITTEE (Loan Approval)
├── TELLER (Member Transactions)
├── AUDITOR (Audit Trail)
└── MEMBER (Self-Service)
```

### Permission Matrix

| Feature | Admin | Treasurer | Loan Officer | Credit Committee | Teller | Auditor | Member |
|---------|-------|-----------|--------------|------------------|--------|---------|--------|
| View Members | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | Own only |
| Approve Member | ✓ | | | | | | |
| Create Loan Product | ✓ | | | | | | |
| Apply for Loan | | | ✓ | | | | ✓ |
| Process Loan | | | ✓ | | | | |
| Approve Loan | | | | ✓ | | | |
| Disburse Loan | ✓ | ✓ | | | | | |
| Process Deposit | ✓ | ✓ | | | ✓ | | |
| View Audit Trail | ✓ | | | | | ✓ | |
| Manage Users | ✓ | | | | | | |
| View Reports | ✓ | ✓ | ✓ | ✓ | | ✓ | |

---

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.x
- **Language:** Java 17+
- **Database:** PostgreSQL
- **ORM:** JPA/Hibernate
- **Security:** JWT, Spring Security
- **Build:** Maven
- **Migrations:** Flyway

### Frontend
- **Framework:** React 18+
- **Language:** TypeScript
- **Build Tool:** Vite
- **Styling:** Tailwind CSS
- **UI Components:** Shadcn/ui
- **HTTP Client:** Axios
- **State Management:** React Context

### Mobile
- **Framework:** Capacitor
- **Platform:** Android
- **Build Tool:** Gradle

### External Integrations
- **M-Pesa:** Payment processing
- **Email:** Notifications
- **SMS:** Notifications

---

## Database Schema

### Core Tables

**users** - Staff and member users
- id, username, email, password, role, enabled, created_at

**members** - Member profiles
- id, member_number, employee_id, first_name, last_name, phone, email, status, created_at

**accounts** - Savings/shares/contributions accounts
- id, member_id, type, balance, frozen_savings, created_at

**transactions** - Deposits and withdrawals
- id, account_id, type, amount, status, created_at

**loans** - Loan records
- id, member_id, loan_product_id, amount, interest_rate, term_months, status, created_at

**guarantors** - Guarantor relationships
- id, loan_id, member_id, guarantee_amount, status, created_at

**loan_products** - Loan product configuration
- id, name, interest_rate, min_amount, max_amount, min_term_months, max_term_months

**audit_log** - Audit trail
- id, user_id, action, entity_type, entity_id, old_value, new_value, timestamp

**notifications** - System notifications
- id, user_id, message, type, read_at, created_at

---

## API Endpoints

### Authentication
- `POST /api/auth/login` - Staff login
- `POST /api/auth/member/login` - Member login
- `POST /api/auth/logout` - Logout

### Members
- `GET /api/members` - List members
- `POST /api/members` - Create member
- `GET /api/members/{id}` - Get member
- `PUT /api/members/{id}` - Update member
- `POST /api/members/{id}/approve` - Approve member

### Loans
- `GET /api/loans` - List loans
- `POST /api/loans/apply` - Apply for loan
- `POST /api/loans/validate-member-eligibility` - Validate member eligibility
- `POST /api/loans/validate-guarantor-eligibility` - Validate guarantor eligibility
- `POST /api/loans/{id}/approve` - Approve loan
- `POST /api/loans/{id}/disburse` - Disburse loan
- `POST /api/loans/{id}/repay` - Repay loan

### Accounts
- `GET /api/accounts/{memberId}` - Get member accounts
- `POST /api/accounts/deposit` - Deposit to account
- `POST /api/accounts/withdraw` - Withdraw from account

### Reports
- `GET /api/reports/profit-loss` - P&L report
- `GET /api/reports/members` - Member report
- `GET /api/reports/loans` - Loan report

### Audit
- `GET /api/audit-trail` - Get audit logs
- `GET /api/audit-trail/user/{userId}` - Get user audit logs

---

## Deployment

### Development
```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend
cd minetsacco-main
npm run dev

# Access
- Staff: http://localhost:3000
- Member: http://localhost:3000/member
- API: http://localhost:8080
```

### Production
- Backend: Docker container or JAR deployment
- Frontend: Static build deployed to CDN or web server
- Database: Managed PostgreSQL instance
- Mobile: APK distributed via Play Store or direct download

---

## Security Features

- JWT token-based authentication
- Role-based access control (RBAC)
- Password hashing (bcrypt)
- CORS configuration
- SQL injection prevention (parameterized queries)
- XSS protection
- CSRF protection
- Audit logging of all actions
- Secure token storage (mobile)
- HTTPS enforcement (production)

---

## Performance Considerations

- Database indexing on frequently queried fields
- Pagination for large result sets
- Caching of loan products and eligibility rules
- Lazy loading of member data
- Optimized Excel parsing for bulk operations
- Connection pooling for database

---

## Known Limitations & Future Enhancements

### Current Limitations
- No offline mode for member portal
- Limited mobile app features (web-based)
- No advanced analytics/ML predictions
- No integration with external banking systems

### Planned Enhancements
- Guarantor rejection handling (3 options)
- Advanced member financial health scoring
- Loan officer dashboard improvements
- Mobile app native features
- Integration with MPESA API for real-time payments
- Advanced reporting and analytics
- Member self-service KYC verification

---

## Support & Maintenance

### Regular Tasks
- Database backups (daily)
- Audit trail review (monthly)
- User access review (quarterly)
- Security updates (as needed)
- Performance monitoring (ongoing)

### Troubleshooting
- Check backend logs: `backend/logs/`
- Check frontend console: Browser DevTools
- Check database: PostgreSQL client
- Check audit trail: Audit Trail page

---

## Contact & Documentation

- **System Overview:** This file
- **Usage Guide:** `Minet SACCO Usage guide.docx`
- **Project Structure:** `Minet SACCO Project Structure.docx`
- **System Design:** `Minet SACCO System Design.docx`
- **Loan Officer Feature:** `PRESENTATION_SUMMARY.md`
- **Guarantor Rejection:** `GUARANTOR_REJECTION_HANDLING.md`

