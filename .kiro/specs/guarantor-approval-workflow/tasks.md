# Guarantor Approval Workflow - Tasks

## Backend Implementation

- [x] 1. Update Guarantor entity with approval status and date fields
- [x] 2. Create GuarantorApprovalService for approval logic
- [x] 3. Add endpoints for guarantor accept/reject
- [x] 4. Update LoanService to handle PENDING_GUARANTOR_APPROVAL status
- [x] 5. Add notification sending when loan applied with guarantors
- [x] 6. Add endpoint to get pending guarantor requests for member
- [x] 7. Update loan status transitions in LoanController

## Frontend Implementation

- [ ] 8. Update MemberLoanApplication to allow guarantor selection by ID
- [ ] 9. Add guarantor request notifications to member portal
- [ ] 10. Create GuarantorApprovalModal component
- [ ] 11. Update loan status display to show guarantor approval state
- [ ] 12. Add guarantor approval section in member dashboard

## Testing

- [ ] 13. Test loan application with guarantor selection
- [ ] 14. Test guarantor notification and approval flow
- [ ] 15. Test loan status transitions
- [ ] 16. Test rejection and reapplication flow
