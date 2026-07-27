# GL Admin Approval Workflow - Implementation Complete

## Overview
The GL Configuration Period Entry tab now has fully functional admin approval workflows. Admins can approve, reject, or lock journal entries submitted by treasurers.

## Workflow Flow

### Treasurer Actions
1. **Create/Edit Draft Entries** → Save as DRAFT (POST `/api/gl/period-entry`)
2. **Submit for Approval** → Changes DRAFT → POSTED (PUT `/api/gl/period-entry/{id}/submit`)

### Admin Actions (read-only by treasurer after submission)
1. **Approve Entry** (POSTED status)
   - Button: "Approve" 
   - Handler: `handleApproveEntry(entryId)`
   - Endpoint: `PUT /api/gl/period-entry/{entryId}/approve`
   - State transition: POSTED → APPROVED
   - Result: Entry approved and will be included in GL calculations

2. **Reject Entry** (POSTED status)
   - Button: "Reject"
   - Handler: `handleRejectEntry(entryId)` 
   - Endpoint: `PUT /api/gl/period-entry/{entryId}/reject`
   - State transition: POSTED → DRAFT (back for treasurer to revise)
   - Behavior: Prompts admin for rejection reason, stores in entry description
   - Result: Entry returned to DRAFT for treasurer to fix and resubmit

3. **Lock Entry** (APPROVED status)
   - Button: "Lock"
   - Handler: `handleLockEntry(entryId)`
   - Endpoint: `PUT /api/gl/period-entry/{entryId}/lock`
   - State transition: APPROVED → LOCKED
   - Result: Entry locked, no further edits allowed

## Status Badges (Frontend)
- **DRAFT** → Grey badge (Treasurer can edit and save)
- **POSTED** → Blue badge (Submitted for approval, awaiting admin review)
- **APPROVED** → Green badge (Admin approved, ready to lock)
- **LOCKED** → Red badge (Finalized, no further changes)

## Entry Types

### AUTO (AGGREGATION Accounts)
- Amount: Read-only (calculated from transactional data)
- Created by system
- Not subject to approval workflow

### MANUAL (MANUAL_ENTRY Accounts)
- Amount: Editable by treasurer
- Status tracked: DRAFT → POSTED → APPROVED → LOCKED
- Subject to full approval workflow

## Files Modified

### Frontend
- **Path**: `minetsacco-main/src/pages/GLConfiguration.tsx`
- **Changes**:
  - Added `handleApproveEntry(entryId)` handler
  - Added `handleRejectEntry(entryId)` handler
  - Added `handleLockEntry(entryId)` handler
  - Wired buttons with onClick handlers:
    - Approve button → `handleApproveEntry(entry.entryId!)`
    - Reject button → `handleRejectEntry(entry.entryId!)`
    - Lock button → `handleLockEntry(entry.entryId!)`
  - Added visual state management with loading and toast notifications

### Backend (No changes needed - already implemented)
- **GLPeriodEntryController**: `/api/gl/period-entry`
  - `@GetMapping` - List period entries
  - `@PostMapping` - Treasurer creates/updates drafts
  - `@PutMapping("/{id}/submit")` - Treasurer submits for approval
  - `@PutMapping("/{id}/approve")` - Admin approves (POSTED → APPROVED)
  - `@PutMapping("/{id}/reject")` - Admin rejects (POSTED → DRAFT)
  - `@PutMapping("/{id}/lock")` - Admin locks (APPROVED → LOCKED)

## Authorization

- **Treasurer**: ROLE_TREASURER
  - Can create and save drafts
  - Can submit for approval
  - Can view all entries

- **Admin**: ROLE_ADMIN
  - Can view all entries
  - Can approve POSTED entries
  - Can reject POSTED entries
  - Can lock APPROVED entries

## User Experience

### For Treasurers
1. Navigate to GL Configuration → Period Entry tab
2. Select month/year and click "Load"
3. Edit MANUAL account amounts and save as drafts
4. When ready, click "Submit All for Approval"
5. If admin rejects, entry returns to DRAFT with rejection reason in description
6. Can revise and resubmit

### For Admins
1. Navigate to GL Configuration → Period Entry tab
2. Select month/year and click "Load"
3. Review POSTED entries (awaiting approval)
4. For each POSTED entry, choose:
   - **Approve**: Moves to APPROVED status
   - **Reject**: Returns to DRAFT with optional reason
5. Once APPROVED, can click "Lock" to finalize

## Validation Rules

- Cannot update entry that is not in DRAFT status
- Cannot approve entry that is not in POSTED status
- Cannot reject entry that is not in POSTED status
- Cannot lock entry that is not in APPROVED status
- Treasurer can only submit own entries
- Only MANUAL_ENTRY accounts can have period entries

## Error Handling

All handlers include:
- Try-catch error handling
- Toast notifications for success/error states
- Automatic refresh of entries after state changes
- User-friendly error messages

## Testing Checklist

- [ ] Treasurer can save draft entries
- [ ] Treasurer can submit for approval (DRAFT → POSTED)
- [ ] Admin sees POSTED entries in actions column
- [ ] Admin can approve entry (POSTED → APPROVED)
- [ ] Admin can reject entry with reason (POSTED → DRAFT)
- [ ] Rejected entry returns to DRAFT with reason in description
- [ ] Admin can lock approved entry (APPROVED → LOCKED)
- [ ] LOCKED entry shows in red badge
- [ ] LOCKED entry amount is read-only
- [ ] Toast notifications appear on success/error
- [ ] Page refreshes after each action
- [ ] Authorization checks work (ADMIN vs TREASURER roles)

## Status

✅ **COMPLETE** - All admin approval handlers are implemented and wired.
