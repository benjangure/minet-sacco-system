# Minet SACCO - Staff Roles & Hierarchy

## Overview
The Minet SACCO system uses a role-based access control (RBAC) model with 5 distinct staff roles. Each role has specific permissions and responsibilities in the loan approval workflow.

---

## Role Hierarchy

```
                          ADMIN
                            |
                    (System Administrator)
                            |
        ________________________|________________________
        |                       |                       |
      LOAN_OFFICER          CREDIT_COMMITTEE        TREASURER
      (Loan Review)         (Loan Approval)         (Disbursement)
        |                       |                       |
        └───────────────────────┴───────────────────────┘
                                |
                            TELLER
                        (Member Service)
```

---

## Role Definitions

### 1. ADMIN (System Administrator)

**Responsibilities:**
- System configuration and maintenance
- User account management (create, edit, delete staff)
- Loan product setup and management
- Fund configuration (benevolent, development, etc.)
- Eligibility rules configuration
- System monitoring and troubleshooting
- Audit trail review
- Backup and recovery

**Permissions:**
- ✅ Full system access
- ✅ Create/edit/delete users
- ✅ Configure loan products
- ✅ Configure eligibility rules
- ✅ View all reports
- ✅ View audit trail
- ✅ Manage system settings

**Cannot:**
- ❌ Process member transactions (use TELLER role)
- ❌ Approve loans (use CREDIT_COMMITTEE role)
- ❌ Disburse loans (use TREASURER role)

**Access Points:**
- Staff Portal: Full access
- User Management page
- System Configuration pages
- Reports & Analytics

---

### 2. LOAN_OFFICER (Loan Review & Assessment)

**Responsibilities:**
- Review loan applications submitted by members
- Verify member eligibility and documentation
- Assess loan purpose and repayment capacity
- Recommend approval or rejection
- Provide feedback to members
- Track application status

**Permissions:**
- ✅ View member profiles and KYC documents
- ✅ View loan applications (PENDING status)
- ✅ Review member eligibility
- ✅ Add comments/notes to applications
- ✅ Recommend approval/rejection
- ✅ View member transaction history
- ✅ Generate member reports

**Cannot:**
- ❌ Approve loans (CREDIT_COMMITTEE only)
- ❌ Disburse loans (TREASURER only)
- ❌ Process member transactions
- ❌ Create/delete users
- ❌ Configure system settings

**Workflow Position:**
```
Member Applies → LOAN_OFFICER Reviews → CREDIT_COMMITTEE Approves → TREASURER Disburses
```

**Access Points:**
- Staff Portal: Loans section
- Member Management
- Reports (member-specific)

---

### 3. CREDIT_COMMITTEE (Loan Approval Authority)

**Responsibilities:**
- Review loan officer recommendations
- Make final approval/rejection decisions
- Set loan terms and conditions
- Ensure compliance with SACCO policies
- Manage credit risk
- Escalate complex cases

**Permissions:**
- ✅ View loan applications (PENDING_LOAN_OFFICER_REVIEW status)
- ✅ View loan officer recommendations
- ✅ Approve or reject loans
- ✅ Set loan conditions
- ✅ View member eligibility assessment
- ✅ Generate approval reports
- ✅ View audit trail for decisions

**Cannot:**
- ❌ Disburse loans (TREASURER only)
- ❌ Process member transactions
- ❌ Create/delete users
- ❌ Configure system settings
- ❌ Modify loan officer recommendations

**Workflow Position:**
```
LOAN_OFFICER Reviews → CREDIT_COMMITTEE Approves → TREASURER Disburses
```

**Access Points:**
- Staff Portal: Loans section (Approval Queue)
- Reports (approval metrics)

---

### 4. TREASURER (Loan Disbursement & Finance)

**Responsibilities:**
- Disburse approved loans to member accounts
- Manage cash flow and liquidity
- Process member deposits and withdrawals
- Reconcile accounts
- Generate financial reports
- Manage fund allocations

**Permissions:**
- ✅ View approved loans (PENDING_TREASURER status)
- ✅ Disburse loans to member accounts
- ✅ Process member deposits
- ✅ Process member withdrawals
- ✅ View account balances
- ✅ Generate financial reports
- ✅ Reconcile transactions
- ✅ View cash flow reports

**Cannot:**
- ❌ Approve loans (CREDIT_COMMITTEE only)
- ❌ Review loan applications
- ❌ Create/delete users
- ❌ Configure system settings
- ❌ Modify approved loan terms

**Workflow Position:**
```
CREDIT_COMMITTEE Approves → TREASURER Disburses → Member Receives Funds
```

**Access Points:**
- Staff Portal: Loans section (Disbursement Queue)
- Savings & Deposits section
- Financial Reports
- Cash Flow Dashboard

---

### 5. TELLER (Member Service & Transactions)

**Responsibilities:**
- Register new members
- Process member deposits and withdrawals
- Handle member inquiries
- Manage member accounts
- Process bulk member operations
- Provide customer service

**Permissions:**
- ✅ Register new members
- ✅ View member profiles
- ✅ Process deposits
- ✅ Process withdrawals
- ✅ View account balances
- ✅ Process bulk member registration
- ✅ Generate member statements
- ✅ Handle member requests

**Cannot:**
- ❌ Approve loans
- ❌ Disburse loans
- ❌ Create/delete users
- ❌ Configure system settings
- ❌ View other staff transactions
- ❌ Modify member eligibility rules

**Access Points:**
- Staff Portal: Members section
- Savings & Deposits section
- Bulk Processing
- Member Reports

---

## Loan Approval Workflow

### Complete Flow with Role Transitions

```
1. MEMBER APPLIES FOR LOAN
   ↓
2. LOAN_OFFICER REVIEWS
   - Verifies eligibility
   - Checks documentation
   - Assesses repayment capacity
   - Recommends approval/rejection
   ↓
3. CREDIT_COMMITTEE DECIDES
   - Reviews officer recommendation
   - Makes final approval/rejection
   - Sets loan terms
   ↓
4. TREASURER DISBURSES
   - Transfers funds to member account
   - Records disbursement
   - Updates loan status
   ↓
5. MEMBER RECEIVES FUNDS
   - Funds appear in savings account
   - Can now repay or apply for another loan
```

### Status Progression

| Status | Assigned To | Action |
|--------|-------------|--------|
| PENDING | LOAN_OFFICER | Review & recommend |
| PENDING_LOAN_OFFICER_REVIEW | CREDIT_COMMITTEE | Approve/reject |
| PENDING_CREDIT_COMMITTEE | CREDIT_COMMITTEE | Final decision |
| PENDING_TREASURER | TREASURER | Disburse funds |
| DISBURSED | MEMBER | Repay loan |
| REPAID | SYSTEM | Closed |

---

## Permission Matrix

| Action | ADMIN | LOAN_OFFICER | CREDIT_COMMITTEE | TREASURER | TELLER |
|--------|-------|--------------|------------------|-----------|--------|
| Create User | ✅ | ❌ | ❌ | ❌ | ❌ |
| Register Member | ✅ | ❌ | ❌ | ❌ | ✅ |
| Review Loan App | ✅ | ✅ | ❌ | ❌ | ❌ |
| Approve Loan | ✅ | ❌ | ✅ | ❌ | ❌ |
| Disburse Loan | ✅ | ❌ | ❌ | ✅ | ❌ |
| Process Deposit | ✅ | ❌ | ❌ | ✅ | ✅ |
| Process Withdrawal | ✅ | ❌ | ❌ | ✅ | ✅ |
| View Audit Trail | ✅ | ❌ | ❌ | ❌ | ❌ |
| Configure System | ✅ | ❌ | ❌ | ❌ | ❌ |
| Generate Reports | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## Typical Team Structure

### Small SACCO (50-200 members)
```
1 ADMIN
1 LOAN_OFFICER (also handles credit committee)
1 TREASURER
1 TELLER
```

### Medium SACCO (200-1000 members)
```
1 ADMIN
2 LOAN_OFFICERS
3 CREDIT_COMMITTEE members
1 TREASURER
2 TELLERS
```

### Large SACCO (1000+ members)
```
1 ADMIN
4 LOAN_OFFICERS
5 CREDIT_COMMITTEE members
2 TREASURERS
5 TELLERS
```

---

## Access Control Implementation

### Backend Enforcement
```java
// Example: Only TREASURER can disburse
@PreAuthorize("hasRole('TREASURER')")
@PostMapping("/disburse-loan")
public ResponseEntity<?> disburseLoan(...) { ... }
```

### Frontend Enforcement
```typescript
// Example: Show disbursement button only for TREASURER
{userRole === 'TREASURER' && (
  <Button onClick={handleDisburse}>Disburse Loan</Button>
)}
```

### Database Enforcement
```sql
-- Audit trail tracks who performed each action
SELECT user_id, action, timestamp FROM audit_log 
WHERE action = 'LOAN_DISBURSEMENT';
```

---

## Separation of Duties

The system enforces **Separation of Duties (SoD)** principle:

1. **No single person can approve and disburse** - CREDIT_COMMITTEE approves, TREASURER disburses
2. **No single person can review and approve** - LOAN_OFFICER reviews, CREDIT_COMMITTEE approves
3. **No single person can manage users and transactions** - ADMIN manages users, TELLER processes transactions
4. **All actions are audited** - Every action logged with user ID and timestamp

---

## Best Practices

### For ADMIN
- ✅ Create separate user accounts for each staff member
- ✅ Use strong passwords
- ✅ Review audit trail regularly
- ✅ Backup system regularly
- ❌ Don't share admin credentials
- ❌ Don't process transactions as admin

### For LOAN_OFFICER
- ✅ Thoroughly review member documentation
- ✅ Document all recommendations
- ✅ Follow eligibility rules strictly
- ❌ Don't approve loans (that's CREDIT_COMMITTEE's job)
- ❌ Don't disburse funds

### For CREDIT_COMMITTEE
- ✅ Review loan officer recommendations carefully
- ✅ Make decisions based on policy
- ✅ Document approval reasons
- ❌ Don't disburse funds
- ❌ Don't override eligibility rules without documentation

### For TREASURER
- ✅ Verify loan is approved before disbursing
- ✅ Reconcile accounts daily
- ✅ Keep cash flow records
- ❌ Don't approve loans
- ❌ Don't process member transactions (use TELLER)

### For TELLER
- ✅ Verify member identity before transactions
- ✅ Keep member records updated
- ✅ Process transactions accurately
- ❌ Don't approve loans
- ❌ Don't disburse funds

---

## Audit & Compliance

All staff actions are logged in the audit trail:
- User ID
- Action performed
- Timestamp
- Status (SUCCESS/FAILURE)
- Details (loan ID, amount, etc.)

**SASRA Compliance:** System maintains complete audit trail for regulatory review.

---

**Last Updated:** April 2, 2026  
**Version:** 1.0.0
