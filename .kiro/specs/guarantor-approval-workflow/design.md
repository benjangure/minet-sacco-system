# Guarantor Approval Workflow - Design

## Overview
Implement a guarantor approval step in the loan application process. Members select guarantors by ID, guarantors receive notifications and can accept/reject, then loans proceed to loan officer review.

## Architecture

### Database Changes
- Add `guarantor_approval_status` to Guarantor entity (PENDING, ACCEPTED, REJECTED)
- Add `guarantor_approval_date` timestamp
- Update Loan status flow: APPLIED → PENDING_GUARANTOR_APPROVAL → PENDING_LOAN_OFFICER_REVIEW → APPROVED/REJECTED

### Backend Services
1. **LoanService** - Update to handle guarantor approval workflow
2. **GuarantorService** - New service for guarantor approval logic
3. **NotificationService** - Send notifications to guarantors

### Backend Endpoints
- `POST /api/loans/{loanId}/guarantor/{guarantorId}/approve` - Guarantor accepts
- `POST /api/loans/{loanId}/guarantor/{guarantorId}/reject` - Guarantor rejects
- `GET /api/member/guarantor-requests` - Get pending guarantor requests for member
- `GET /api/loans/{loanId}/guarantor-status` - Check guarantor approval status

### Frontend Components
1. **Loan Application** - Add guarantor selection by member number/employee ID
2. **Guarantor Notifications** - Show pending guarantor requests in member portal
3. **Guarantor Approval Modal** - Accept/Reject interface
4. **Loan Status Display** - Show "Awaiting Guarantor Approval" status

### Workflow
1. Member applies for loan with guarantor IDs
2. Loan status = PENDING_GUARANTOR_APPROVAL
3. Guarantors notified
4. Guarantor accepts → status = PENDING_LOAN_OFFICER_REVIEW
5. Guarantor rejects → loan returns to member (needs new guarantor)
6. Loan officer reviews and approves/rejects as before

### Notification Flow
- When loan applied: Send notification to all guarantors
- When guarantor accepts: Update loan status, notify loan officer
- When guarantor rejects: Notify member to select new guarantor
