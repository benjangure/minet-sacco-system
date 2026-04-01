# Audit Trail - Complete Explanation

## What is an Audit Trail?

An audit trail is a **complete record of all actions and changes** made in a system. It serves as:
- **Compliance & Accountability**: Track who did what, when, and why
- **Security**: Detect unauthorized or suspicious activities
- **Troubleshooting**: Understand what happened and when
- **Compliance**: Meet regulatory requirements (banking, financial institutions)

For a SACCO system, audit trails track:
- Loan approvals/rejections
- Deposit requests
- Member registrations
- Bulk uploads
- Fund configuration changes
- Guarantor approvals
- Loan disbursements
- Any financial transaction or approval

---

## Current Implementation in Minet SACCO

### 1. **Data Storage**
**Database Table**: `audit_logs`

**Fields Captured**:
```
- id: Unique identifier
- user_id: Who performed the action (FK to users table)
- action: Type of action (APPROVE, REJECT, DISBURSE, CREATE, UPDATE, DELETE, BULK_UPLOAD, etc.)
- entity_type: What was acted upon (LOAN, DEPOSIT_REQUEST, GUARANTOR, MEMBER, BulkBatch, FundConfiguration)
- entity_id: ID of the specific entity
- entity_details: JSON/text description of the entity
- comments: Reason or notes for the action
- timestamp: When the action occurred
- status: SUCCESS or FAILURE
- error_message: If status is FAILURE, what went wrong
- ip_address: IP address of the requester
- user_agent: Browser/client information
```

**Indexes** (for fast queries):
- user_id
- action
- entity_type
- timestamp
- entity_id

---

### 2. **Where Audit Logs Are Created**

Audit logs are automatically created in these services:

#### **LoanService** (Loan approvals/rejections)
```
- APPROVE action when loan is approved
- REJECT action when loan is rejected
- Captures: Loan number, member name, amount
```

#### **LoanDisbursementService** (Loan disbursements)
```
- DISBURSE action when loan is disbursed
- Captures: Loan number, member name, amount
```

#### **DepositRequestService** (Deposit approvals/rejections)
```
- APPROVE action when deposit is approved
- REJECT action when deposit is rejected
- Captures: Deposit amount, member name, approval notes
```

#### **GuarantorApprovalService** (Guarantor approvals/rejections)
```
- APPROVE action when guarantor approves
- REJECT action when guarantor rejects
- Captures: Guarantor details, loan number, member name
```

#### **BulkProcessingService** (Bulk uploads and approvals)
```
- BULK_UPLOAD action for member registrations, loan applications, contributions, disbursements
- BULK_APPROVE action when bulk batch is approved
- BULK_REJECT action when bulk batch is rejected
- Captures: Batch number, record count
```

#### **FundConfigurationService** (Fund configuration changes)
```
- UPDATE_FUND_CONFIG action when fund settings change
- TOGGLE_FUND action when fund is enabled/disabled
- Captures: Fund type, old/new values
```

---

### 3. **Backend API Endpoints**

**Controller**: `AuditController.java`

**Endpoints**:
```
GET /api/audit
  - Get all audit logs with pagination
  - Parameters: page, size, sortBy, direction
  - Returns: List of audit logs with total count

GET /api/audit/user/{userId}
  - Get logs for specific user

GET /api/audit/action/{action}
  - Get logs for specific action (APPROVE, REJECT, etc.)

GET /api/audit/entity-type/{entityType}
  - Get logs for specific entity type (LOAN, DEPOSIT_REQUEST, etc.)

GET /api/audit/date-range
  - Get logs within date range
  - Parameters: startDate, endDate

GET /api/audit/filter
  - Get logs with multiple filters
  - Parameters: userId, action, entityType, status, startDate, endDate, page, size
  - Returns: Filtered audit logs

GET /api/audit/failed
  - Get only failed operations (status = FAILURE)
```

---

### 4. **Frontend Implementation**

**Component**: `minetsacco-main/src/pages/AuditTrail.tsx`

**Features**:
- **Filters**:
  - Action (APPROVE, REJECT, DISBURSE, CREATE, UPDATE, DELETE)
  - Entity Type (LOAN, DEPOSIT_REQUEST, MEMBER, GUARANTOR)
  - Status (SUCCESS, FAILURE)
  - Date Range (Start Date, End Date)

- **Display**:
  - Table showing: Timestamp, User, Action, Entity Type, Details, Comments, Status, IP Address
  - Pagination (20 items per page)
  - Color-coded badges for actions and status

- **Export**:
  - Export filtered logs to CSV

---

### 5. **Data Flow**

```
User Action (e.g., Approve Loan)
    ↓
LoanService.approveLoan()
    ↓
auditService.logAction(user, "APPROVE", "LOAN", loanId, details, comments, "SUCCESS")
    ↓
AuditLog entity created and saved to audit_logs table
    ↓
Frontend calls GET /api/audit?filters
    ↓
AuditController retrieves from database
    ↓
AuditTrail.tsx displays in table
```

---

### 6. **Current Issues & Fixes Applied**

**Issue**: Audit trail showing zero logs
**Root Cause**: The `/api/audit/filter` endpoint required mandatory date parameters
**Fix**: Changed to use `/api/audit` endpoint which accepts optional filters

**Issue**: Date validation missing
**Fix**: Added validation to prevent start date > end date

**Issue**: Apply Filters button not disabled for invalid dates
**Fix**: Added disabled state when date range is invalid

---

### 7. **What Gets Audited**

✅ **Currently Audited**:
- Loan approvals/rejections
- Loan disbursements
- Deposit request approvals/rejections
- Guarantor approvals/rejections
- Bulk uploads (members, loans, contributions, disbursements)
- Bulk batch approvals/rejections
- Fund configuration changes

❌ **NOT Currently Audited** (could be added):
- User logins
- User creation/deletion
- Member registrations (individual)
- Account transactions
- Report generation
- System configuration changes

---

### 8. **Example Audit Log Entry**

```
Timestamp: 2024-03-31 14:30:45
User: John Doe (Loan Officer)
Action: APPROVE
Entity Type: LOAN
Entity ID: 123
Details: Loan #LN-2024-001 - Member: Jane Smith - Amount: KES 50,000
Comments: Loan approved - meets all criteria
Status: SUCCESS
IP Address: 192.168.0.195
User Agent: Mozilla/5.0 (Android)
```

---

## Summary

The audit trail in Minet SACCO is a **comprehensive logging system** that:
1. **Automatically captures** all important business actions
2. **Stores detailed information** about who, what, when, and why
3. **Provides filtering and search** capabilities for compliance and investigation
4. **Supports export** for reporting and archival
5. **Tracks both successes and failures** for troubleshooting

It's essential for:
- Regulatory compliance (SASRA requirements)
- Fraud detection and prevention
- Accountability and transparency
- System troubleshooting and debugging
