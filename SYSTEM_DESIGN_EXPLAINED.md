# Minet SACCO - System Design Explained

This document explains the architectural decisions and implementation details that make the system work.

## 1. Mobile App Integration with Member Portal

### How the Android APK Connects to the Web Portal

The mobile app and web portal are **not separate systems** - they share the same backend API and database. Here's how they work together:

**Architecture:**
```
┌─────────────────────────────────────────────────────────┐
│                   Shared Backend API                     │
│              (Spring Boot REST Endpoints)               │
│                  PostgreSQL Database                     │
└─────────────────────────────────────────────────────────┘
         ▲                                    ▲
         │                                    │
    ┌────┴─────────────────────────────┬─────┴────┐
    │                                  │          │
┌───▼────────────┐          ┌──────────▼──┐  ┌───▼──────────┐
│  Web Portal    │          │ Android APK │  │ Staff Portal │
│  (React)       │          │ (Capacitor) │  │  (React)     │
│ localhost:3000 │          │ Mobile App  │  │ localhost:3000
│ /member        │          │             │  │ /dashboard
└────────────────┘          └─────────────┘  └──────────────┘
```

**Key Points:**
- Both web and mobile use the same REST API endpoints
- Both authenticate via JWT tokens
- Both store data in the same PostgreSQL database
- The only difference is the UI layer (React web vs Capacitor mobile)

### Token Storage Differences

**Web Portal (Member):**
- Token stored in `localStorage.getItem('token')`
- Persists across browser sessions
- Accessible via JavaScript

**Mobile App (APK):**
- Token stored in Capacitor's secure storage
- Encrypted on device
- Survives app restarts

**Staff Portal (Web):**
- Token stored in React Context (AuthContext)
- Session-based (lost on page refresh)
- Used for in-memory state management

### API Request Flow

```
Mobile App / Web Portal
    │
    ├─ Add JWT token to Authorization header
    │  (Bearer <token>)
    │
    ├─ Send HTTP request to backend
    │  (e.g., GET /api/member/dashboard)
    │
    ├─ Backend validates JWT token
    │
    ├─ Backend checks user role/permissions
    │
    ├─ Backend executes business logic
    │
    └─ Backend returns JSON response
       (same format for all clients)
```

## 2. Android APK Initialization

### Build Process

The APK is built using **Capacitor**, which wraps the React web app in a native Android container:

```
React Web App (TypeScript/React)
    │
    ├─ npm run build (creates dist/ folder)
    │
    ├─ Capacitor copies dist/ to Android assets
    │
    ├─ Gradle builds Android APK
    │  (includes WebView to render React)
    │
    └─ APK ready for installation
```

### App Initialization Flow

1. **User installs APK** → Android loads the app
2. **WebView starts** → Loads React app from assets
3. **React app initializes** → Checks localStorage for token
4. **If token exists** → Redirects to /member (member portal)
5. **If no token** → Shows member login page
6. **User logs in** → Token saved to localStorage
7. **App navigates to dashboard** → Member portal loads

### Key Configuration Files

- `android/app/src/main/AndroidManifest.xml` - App permissions and entry point
- `android/app/build.gradle` - Build configuration
- `capacitor.config.ts` - Capacitor settings (app name, version, etc.)
- `src/main.tsx` - React app entry point

### Why Capacitor?

- Single codebase for web and mobile
- No need to maintain separate React Native app
- Faster development and deployment
- Easy to add native features (camera, geolocation, etc.)

## 3. How Calculations Work in the System

### Loan Calculations

**Loan Eligibility:**
```
Eligible Amount = Min(
  Member's Savings Balance × Multiplier,
  Loan Product Max Amount
)

Example:
- Member has 50,000 KES in savings
- Loan product multiplier = 3x
- Loan product max = 200,000 KES
- Eligible amount = Min(50,000 × 3, 200,000) = 150,000 KES
```

**Interest Calculation:**
```
Monthly Interest = Loan Amount × (Annual Rate / 12 / 100)

Example:
- Loan: 100,000 KES
- Annual rate: 12%
- Monthly interest = 100,000 × (12 / 12 / 100) = 1,000 KES
```

**Repayment Schedule:**
```
Monthly Payment = (Loan Amount + Total Interest) / Number of Months

Example:
- Loan: 100,000 KES
- Monthly interest: 1,000 KES
- Duration: 12 months
- Total interest: 1,000 × 12 = 12,000 KES
- Monthly payment = (100,000 + 12,000) / 12 = 9,333 KES
```

**Guarantor Capacity:**
```
Guarantor Capacity = Guarantor's Savings × Multiplier - Existing Guarantees

Example:
- Guarantor has 100,000 KES savings
- Multiplier: 2x
- Already guaranteeing: 50,000 KES
- Available capacity = (100,000 × 2) - 50,000 = 150,000 KES
```

### Account Balance Calculations

**Savings Account:**
```
Current Balance = Previous Balance + Deposits - Withdrawals

Tracked in: Account entity, updated on each transaction
```

**Shares Account:**
```
Status: DORMANT (no deposits allowed)
Balance: Always 0 (or initial value)
Purpose: Placeholder for future use
```

**Contributions Account:**
```
Updated via: Bulk processing monthly contributions
Formula: Previous Balance + Monthly Contribution Amount
```

### Profit & Loss Calculation

```
Total Revenue = Loan Interest + Penalties + Fees

Total Expenses = Staff Salaries + Operational Costs + Provisions

Net Profit/Loss = Total Revenue - Total Expenses

Calculated in: ProfitLossReportDTO
```

## 4. Data Logging & Audit Trail

### What Gets Logged

Every significant action is logged to the `audit_log` table:

```
User Action → AuditService.log() → Database Record

Logged Actions:
- User login/logout
- Member registration/approval
- Account deposits/withdrawals
- Loan applications/approvals/disbursements
- Loan repayments
- User role changes
- Data modifications
- Bulk processing operations
```

### Audit Log Structure

```sql
CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  action VARCHAR(255),           -- e.g., "LOAN_APPROVED"
  entity_type VARCHAR(255),      -- e.g., "Loan"
  entity_id BIGINT,              -- ID of affected record
  old_value TEXT,                -- Previous value (JSON)
  new_value TEXT,                -- New value (JSON)
  timestamp TIMESTAMP,           -- When action occurred
  ip_address VARCHAR(45),        -- User's IP
  user_agent VARCHAR(500)        -- Browser/app info
);
```

### How Logging Works

```
1. User performs action (e.g., approves loan)
   │
2. Controller receives request
   │
3. Service executes business logic
   │
4. AuditService.log() called with:
   - User ID
   - Action type
   - Entity type and ID
   - Old and new values
   │
5. Audit record inserted to database
   │
6. Response sent to user
```

### Accessing Audit Trail

**Staff Portal:**
- Navigate to Audit Trail page
- Filter by user, date range, action type
- View complete history of all system changes

**API Endpoint:**
```
GET /api/audit-trail
GET /api/audit-trail/user/{userId}
GET /api/audit-trail?action=LOAN_APPROVED&startDate=2024-01-01
```

## 5. Role Separation & Permission Enforcement

### User Roles

```
┌─────────────────────────────────────────────────────────┐
│                    ADMIN                                 │
│  (Full system access, user management)                  │
└─────────────────────────────────────────────────────────┘

┌──────────────────┬──────────────────┬──────────────────┐
│   TREASURER      │  LOAN_OFFICER    │  CREDIT_COMMITTEE│
│  (Accounting)    │  (Loan Processing)│ (Loan Approval) │
└──────────────────┴──────────────────┴──────────────────┘

┌──────────────────┬──────────────────┬──────────────────┐
│     TELLER       │     AUDITOR      │     MEMBER       │
│ (Transactions)   │ (Audit Trail)    │ (Self-Service)   │
└──────────────────┴──────────────────┴──────────────────┘
```

### Permission Matrix

| Action | Admin | Treasurer | Loan Officer | Credit Committee | Teller | Auditor | Member |
|--------|-------|-----------|--------------|------------------|--------|---------|--------|
| View Members | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | Own only |
| Approve Member | ✓ | | | | | | |
| Create Loan Product | ✓ | | | | | | |
| Apply for Loan | | | | | | | ✓ |
| Process Loan | | | ✓ | | | | |
| Approve Loan | | | | ✓ | | | |
| Disburse Loan | ✓ | ✓ | | | | | |
| Process Deposit | ✓ | ✓ | | | ✓ | | |
| View Audit Trail | ✓ | | | | | ✓ | |
| Manage Users | ✓ | | | | | | |

### How Permissions Are Enforced

**Backend (Spring Security):**
```java
@PreAuthorize("hasRole('LOAN_OFFICER')")
public ResponseEntity<?> processLoan(@RequestBody LoanRequest request) {
    // Only LOAN_OFFICER can execute this
}
```

**Frontend (React):**
```typescript
{user?.role === 'LOAN_OFFICER' && (
  <button onClick={handleProcessLoan}>Process Loan</button>
)}
```

**Database Level:**
- Queries filtered by user role
- Members can only see their own data
- Staff can only see assigned members

## 6. Member vs Staff Login Separation

### How the System Prevents Cross-Login

**Problem:** How do we prevent a member from logging in via staff portal and vice versa?

**Solution:** Role-based routing and authentication checks

### Login Flow - Member

```
1. User visits /member
2. MemberLogin component loads
3. User enters phone number + password
4. POST /api/auth/member/login
5. Backend checks:
   - User exists
   - Password correct
   - User role = MEMBER
6. If valid → Return JWT token
7. Token stored in localStorage
8. Redirect to /member/dashboard
9. ProtectedRoute checks:
   - Token exists
   - Token valid
   - User role = MEMBER
10. If all pass → Show member portal
11. If fail → Redirect to /member/login
```

### Login Flow - Staff

```
1. User visits /login or /
2. Login component loads
3. User enters username + password
4. POST /api/auth/login
5. Backend checks:
   - User exists
   - Password correct
   - User role ≠ MEMBER (must be staff)
6. If valid → Return JWT token
7. Token stored in AuthContext
8. Redirect to /dashboard
9. ProtectedRoute checks:
   - Token exists
   - Token valid
   - User role ≠ MEMBER
10. If all pass → Show staff portal
11. If fail → Redirect to /login
```

### Key Differences

| Aspect | Member | Staff |
|--------|--------|-------|
| Login URL | `/member` | `/login` or `/` |
| Endpoint | `/api/auth/member/login` | `/api/auth/login` |
| Token Storage | localStorage | AuthContext |
| Dashboard | `/member/dashboard` | `/dashboard` |
| Allowed Roles | MEMBER only | ADMIN, TREASURER, etc. |
| Session | Persistent | Session-based |

### Backend Validation

```java
// Member login endpoint
@PostMapping("/member/login")
public ResponseEntity<?> memberLogin(@RequestBody AuthRequest request) {
    User user = userService.findByPhone(request.getPhone());
    
    // Enforce: Only MEMBER role can use this endpoint
    if (!user.getRole().equals(Role.MEMBER)) {
        throw new UnauthorizedException("Invalid credentials");
    }
    
    // Generate token
    String token = jwtProvider.generateToken(user);
    return ResponseEntity.ok(new AuthResponse(token));
}

// Staff login endpoint
@PostMapping("/login")
public ResponseEntity<?> staffLogin(@RequestBody AuthRequest request) {
    User user = userService.findByUsername(request.getUsername());
    
    // Enforce: Only staff roles can use this endpoint
    if (user.getRole().equals(Role.MEMBER)) {
        throw new UnauthorizedException("Invalid credentials");
    }
    
    // Generate token
    String token = jwtProvider.generateToken(user);
    return ResponseEntity.ok(new AuthResponse(token));
}
```

### Frontend Validation

```typescript
// Member Portal - ProtectedRoute
function MemberProtectedRoute({ children }) {
  const token = localStorage.getItem('token');
  const decoded = jwtDecode(token);
  
  if (decoded.role !== 'MEMBER') {
    return <Navigate to="/member/login" />;
  }
  
  return children;
}

// Staff Portal - ProtectedRoute
function StaffProtectedRoute({ children }) {
  const { user } = useAuth();
  
  if (user?.role === 'MEMBER') {
    return <Navigate to="/member" />;
  }
  
  return children;
}
```

### Additional Security Measures

1. **Separate Login Endpoints** - Different endpoints for member vs staff
2. **Role Validation** - Backend validates role on every request
3. **Token Expiration** - Tokens expire after set time
4. **CORS Configuration** - Only allow requests from authorized origins
5. **HTTPS Only** - All communication encrypted
6. **Secure Cookies** - HttpOnly, Secure, SameSite flags set

## 7. Data Integrity & Consistency

### Transaction Management

```java
@Transactional
public void approveLoan(Long loanId) {
    // All operations succeed or all fail
    Loan loan = loanRepository.findById(loanId);
    loan.setStatus(LoanStatus.APPROVED);
    
    // Create notification
    Notification notification = new Notification(...);
    notificationRepository.save(notification);
    
    // Log audit trail
    auditService.log("LOAN_APPROVED", loan);
    
    // If any operation fails, entire transaction rolls back
}
```

### Validation Layers

```
User Input
    ↓
Frontend Validation (React)
    ↓
API Request
    ↓
Controller Validation (@Valid)
    ↓
Service Business Logic Validation
    ↓
Database Constraints
    ↓
Audit Logging
```

### Referential Integrity

- Foreign keys prevent orphaned records
- Cascade delete for related entities
- Unique constraints on critical fields
- Check constraints for valid values



## 8. Recent Fixes & Enhancements (April 2026)

### Loan Eligibility Calculation Fix

**Problem:** Members could apply for multiple loans and the system incorrectly calculated eligibility by treating loan disbursements as new savings, inflating the eligible amount.

**Solution:** Formula-based approach using True Savings calculation:
```
True Savings = Current Savings - Total Disbursed Loans
Remaining Eligibility = (True Savings × 3) - Outstanding Loans
```

**Key Implementation Details:**
- Only counts loans with active statuses (DISBURSED, APPROVED, REPAID)
- Always subtracts original loan amount, never outstanding balance
- Handles partially repaid loans correctly
- Displays breakdown to members showing all components

**Files Modified:**
- `LoanEligibilityValidator.java` - Core calculation logic
- `MemberPortalController.java` - API endpoint
- `MemberLoanApplication.tsx` - Frontend display

### Loan Repayment Process Enhancement

**Problem:** Repayments weren't creating transaction records, not updating savings conditionally, and not releasing guarantor pledges.

**Solution:** Complete repayment workflow with 4 components:

1. **Transaction Recording** - Every repayment creates LOAN_REPAYMENT transaction for audit trail
2. **Conditional Savings Debit** - Only SAVINGS_DEDUCTION payment method debits savings; external payments (M-Pesa, Bank Transfer, Cash) don't touch savings
3. **Guarantor Pledge Release** - When loan fully repaid, all guarantor pledges automatically released
4. **Loan Status Progression** - DISBURSED → PARTIALLY_REPAID → REPAID

**Payment Method Logic:**
```
SAVINGS_DEDUCTION → Debit savings + Create LOAN_REPAYMENT transaction
M-PESA → No savings debit + Create LOAN_REPAYMENT transaction
BANK_TRANSFER → No savings debit + Create LOAN_REPAYMENT transaction
CASH → No savings debit + Create LOAN_REPAYMENT transaction
```

**Files Modified:**
- `MemberPortalController.java` - Repayment logic
- `LoanRepaymentForm.tsx` - Payment method selection

### Shares Account Restriction

**Problem:** Members could submit deposit requests to SHARES account, which should not accept contributions.

**Solution:** Added validation to reject deposits to SHARES account with clear error message.

**Files Modified:**
- `MemberPortalController.java` - Deposit validation

### APK Icon & Splash Screen

**Problem:** APK displayed generic "M" icon instead of Minet SACCO logo.

**Solution:** 
- Replaced vector icon with actual logo PNG in all Android densities
- Created custom splash screen with white background, logo, and "Welcome to Minet SACCO" text
- Splash displays for 4 seconds on app startup

**Files Modified:**
- `ic_launcher_foreground.xml` - Logo reference
- `splash_screen.xml` - Splash screen layout
- `capacitor.config.ts` - Splash configuration

### Member Portal Routing

**Problem:** Staff couldn't access staff login after routing changes.

**Solution:** Intelligent root route that checks user role:
- Staff logged in → `/dashboard`
- Member logged in → `/member/dashboard`
- Not logged in → Staff login page

**Files Modified:**
- `App.tsx` - Root routing logic

---

**Last Updated:** April 2, 2026  
**Version:** 1.0.0
