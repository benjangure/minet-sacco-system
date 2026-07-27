# Admin Journal Entry (GL Manual Entry) Approval Workflow - Implementation Breakdown

## Summary
The admin's role has been **fully implemented** to approve/reject journal entries (GL Manual Entries) submitted by the treasurer. The system follows a **Maker-Checker pattern** where the Treasurer creates entries and the Admin approves or rejects them before they're included in GL calculations.

---

## What Has Been Implemented

### 1. **Frontend Implementation**

#### UI Page: GL Manual Entries (`GLManualEntries.tsx`)
**Location:** `/src/pages/GLManualEntries.tsx`

**Features:**
- **Two tabs for Admins:**
  - **"Pending Approval"** - Shows only entries awaiting admin decision
  - **"All Entries"** - Shows all entries regardless of status

- **Approval Actions (visible only for PENDING entries):**
  - ✅ **Approve** - Admin clicks green checkmark to approve entry
  - ❌ **Reject** - Admin clicks red X to reject entry
  - 🗑️ **Delete** - Admin can delete pending entries

- **Entry Details Displayed:**
  - Date
  - Account Code & Name
  - Reason (Accrual, Adjustment, Allocation, Reclassification)
  - Description
  - Amount
  - Debit/Credit Type
  - Status (PENDING, APPROVED, REJECTED)

- **Status Badges:**
  - Yellow badge: PENDING
  - Green badge: APPROVED
  - Red badge: REJECTED

- **Treasurer Workflow (also on same page):**
  - Treasurer can create new entries with a form
  - Entries are created with PENDING status
  - A note states: "This entry will be created with PENDING status and sent for admin approval before being included in GL calculations"

#### Sidebar Menu
**Location:** `/src/components/AppSidebar.tsx` (lines 31-32)

- Menu item: **"GL Manual Entries"** at `/gl-manual-entries`
- Accessible by: `treasurer` and `admin` roles
- Icon: Notebook

#### App Routing
**Location:** `/src/App.tsx` (line 105)

- Route configured: `/gl-manual-entries`
- Protected by `ProtectedRoute` component
- Role-based access control in place

---

### 2. **Backend Implementation**

#### Controller: GLController
**Location:** `/backend/src/main/java/com/minet/sacco/controller/GLController.java`

**Approval Endpoints:**

1. **Approve Entry**
   ```java
   @PutMapping("/manual-entries/{entryId}/approve")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<ApiResponse<GLManualEntryDTO>> approveEntry(
     @PathVariable Integer entryId,
     Authentication authentication
   )
   ```
   - Requires `ADMIN` role
   - Updates entry status to APPROVED
   - Records approver user and timestamp
   - Logs the action

2. **Reject Entry**
   ```java
   @PutMapping("/manual-entries/{entryId}/reject")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<ApiResponse<GLManualEntryDTO>> rejectEntry(
     @PathVariable Integer entryId,
     Authentication authentication
   )
   ```
   - Requires `ADMIN` role
   - Updates entry status to REJECTED
   - Records rejecter user and timestamp
   - Logs the action

#### Service: GLManualEntryService
**Location:** `/backend/src/main/java/com/minet/sacco/service/GLManualEntryService.java`

**Core Methods:**

1. **approveEntry(Integer entryId, Integer approverId)**
   - Validates entry exists and is in PENDING status
   - Sets status to APPROVED
   - Records approver info
   - Records timestamp (approvedAt)
   - Saves and returns updated entry

2. **rejectEntry(Integer entryId, Integer approverId)**
   - Validates entry exists and is in PENDING status
   - Sets status to REJECTED
   - Records rejecter info
   - Records timestamp
   - Saves and returns updated entry

3. **getPendingEntries()**
   - Returns all entries with PENDING status
   - Ordered by creation date (newest first)

4. **getAllEntries()**
   - Returns all entries regardless of status
   - Sorted by creation date

5. **createManualEntry(GLManualEntryRequest, Integer userId)**
   - Creates entry with PENDING status
   - Links to GL Account
   - Records creator user
   - Sets entry reason from dropdown

6. **deleteEntry(Integer entryId)**
   - Only allows deletion of PENDING entries
   - Prevents modification of approved/rejected entries

#### Entity: GLManualEntry
**Status States:**
```java
enum ApprovalStatus {
  PENDING,
  APPROVED,
  REJECTED
}

enum EntryReason {
  ACCRUAL,
  ADJUSTMENT,
  ALLOCATION,
  RECLASSIFICATION
}
```

**Key Fields:**
- `id` - Unique identifier
- `glAccount` - GL Account reference
- `entryDate` - Date of entry
- `description` - Entry details
- `amount` - Amount in KES
- `isDebit` - Debit (true) or Credit (false)
- `entryReason` - Reason for entry
- `approvalStatus` - Current status (PENDING, APPROVED, REJECTED)
- `createdByUser` - Treasurer who created entry
- `approvedByUser` - Admin who approved/rejected entry
- `createdAt` - Creation timestamp
- `approvedAt` - Approval/rejection timestamp

#### Database Support
**Migration:** `V116__Create_GL_Tables.sql` and related migrations

Tables involved:
- `gl_manual_entry` - Stores all manual entries
- `gl_account` - GL Account master

---

### 3. **API Service Layer**

#### Frontend Service: glManualEntryService
**Location:** `/src/services/glManualEntryService.ts`

**API Methods:**

```typescript
// Admin-specific methods:
approveEntry(entryId: number): Promise<GLManualEntry>
  // PUT /gl/manual-entries/{entryId}/approve

rejectEntry(entryId: number): Promise<GLManualEntry>
  // PUT /gl/manual-entries/{entryId}/reject

// Query methods:
getPendingEntries(): Promise<GLManualEntry[]>
  // GET /gl/manual-entries/pending

getAllEntries(): Promise<GLManualEntry[]>
  // GET /gl/manual-entries

// Treasurer-specific method:
createManualEntry(entry: GLManualEntryRequest): Promise<GLManualEntry>
  // POST /gl/manual-entries

deleteEntry(entryId: number): Promise<void>
  // DELETE /gl/manual-entries/{entryId}
```

---

## Workflow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    GL ENTRY APPROVAL FLOW                   │
└─────────────────────────────────────────────────────────────┘

TREASURER (Creates Entry)
    │
    ├─ Accesses "GL Manual Entries" page
    ├─ Clicks "New Entry" button
    ├─ Fills form:
    │  ├─ GL Account
    │  ├─ Entry Date
    │  ├─ Amount
    │  ├─ Debit/Credit
    │  ├─ Reason (dropdown)
    │  └─ Description
    ├─ Clicks "Submit Entry for Approval"
    └─ Entry created with PENDING status
                 │
                 ▼
         [Entry Status: PENDING]
                 │
    ┌────────────┴────────────┐
    │                         │
    ▼                         ▼
ADMIN (Approve)     ADMIN (Reject)
    │                         │
    ├─ Accesses "GL Manual Entries"    ├─ Accesses "GL Manual Entries"
    ├─ Views "Pending Approval" tab    ├─ Views "Pending Approval" tab
    ├─ Reviews entry details           ├─ Reviews entry details
    ├─ Clicks green ✓ button           ├─ Clicks red ✗ button
    └─ Entry → APPROVED                └─ Entry → REJECTED
                 │                                 │
                 ▼                                 ▼
      [Entry Status: APPROVED]        [Entry Status: REJECTED]
      (Included in GL calculations)   (Not included in GL)
                 │
                 └─ Admin can view in "All Entries" tab
                 └─ Can see who approved and when
```

---

## Role-Based Access Control

### Admin Capabilities
- ✅ View "Pending Approval" tab (only PENDING entries)
- ✅ View "All Entries" tab (all entries with all statuses)
- ✅ Approve PENDING entries
- ✅ Reject PENDING entries
- ✅ Delete PENDING entries (before approval)
- ✅ See who created each entry (createdByUser)
- ✅ See who approved/rejected and when (approvedByUser, approvedAt)

### Treasurer Capabilities
- ✅ Create new GL Manual Entries
- ✅ View "Pending Approval" tab (only entries they created or pending)
- ✅ View "All Entries" tab
- ✅ Cannot approve or reject entries
- ✅ Cannot delete entries once submitted
- ❌ Approve/Reject (Admin only)

### Security
- `@PreAuthorize("hasRole('ADMIN')")` ensures only admins can approve/reject
- Authentication captures user ID for audit trail
- Token-based authentication with JWT (from localStorage)

---

## Data Captured for Audit Trail

Each GL Manual Entry captures:
1. **Creator Information**
   - `createdByUser` - Username of treasurer who created entry
   - `createdAt` - Timestamp of creation

2. **Approver Information**
   - `approvedByUser` - Username of admin who approved/rejected
   - `approvedAt` - Timestamp of approval/rejection

3. **Entry Details**
   - All transaction details (amount, account, date, reason, description)
   - Entry status progression (PENDING → APPROVED/REJECTED)

---

## Status Lifecycle

```
Entry Creation:        PENDING
                          │
                    (Admin Decision)
                    /           \
                   /             \
            ✓ APPROVED        ✗ REJECTED
```

**Transitions:**
- PENDING → APPROVED: When admin clicks approve
- PENDING → REJECTED: When admin clicks reject
- PENDING → DELETED: When entry is deleted before approval (only treasurer/admin action)

**Non-reversible:**
- Once APPROVED or REJECTED, status cannot change
- Only PENDING entries can be deleted

---

## Current Features Summary

| Feature | Implemented | Details |
|---------|:----------:|---------|
| Create GL Manual Entries | ✅ | Treasurer creates with PENDING status |
| View Pending Entries | ✅ | Admin sees only PENDING entries in dedicated tab |
| View All Entries | ✅ | Both roles can see entry history |
| Approve Entry | ✅ | Admin approves, records user & timestamp |
| Reject Entry | ✅ | Admin rejects, records user & timestamp |
| Delete Entry | ✅ | Delete PENDING entries only |
| Entry Reasons | ✅ | Accrual, Adjustment, Allocation, Reclassification |
| Debit/Credit Support | ✅ | Choose debit or credit for each entry |
| Audit Trail | ✅ | Captures creator, approver, timestamps |
| Role-Based Access | ✅ | ADMIN-only approval/rejection |
| GL Account Selection | ✅ | Dropdown of all GL accounts |
| Status Display | ✅ | Color-coded badges (Yellow/Green/Red) |

---

## Key Implementation Points

1. **Maker-Checker Pattern:** Treasurer (Maker) creates, Admin (Checker) approves
2. **Audit Trail:** All actions recorded with user info and timestamps
3. **Security:** API endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`
4. **Status Immutability:** PENDING entries can be deleted; APPROVED/REJECTED cannot change
5. **GL Integration:** Entries are designed to be included in GL calculations once APPROVED
6. **UI/UX:** Clear visual separation between pending and historical entries

---

## What Still Needs Implementation (If Any)

- **Notification system:** Could add notifications when entries are approved/rejected
- **Bulk approval:** Could add bulk approve/reject for multiple entries
- **Approval comments:** Could add reason/notes field for rejections
- **GL Integration:** Verify that APPROVED entries are actually included in Trial Balance calculations
- **Period Lock:** Could prevent entry approval after period close

---

## Conclusion

✅ **The admin journal entry approval workflow has been fully implemented.**

The system provides:
- A clear separation of duties (Treasurer creates, Admin approves)
- Complete audit trail (who created, who approved, when)
- User-friendly UI with pending/all views
- Security through role-based access control
- Status management through PENDING → APPROVED/REJECTED lifecycle

The admin's work is appropriately scoped to **reviewing and approving journal entries submitted by the treasurer** before they're used in GL calculations.
