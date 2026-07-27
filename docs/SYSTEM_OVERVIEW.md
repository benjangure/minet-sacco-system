**System Overview**

- **Purpose**: Minet SACCO Management System is a full-stack application for managing member accounts, loans, repayments, guarantors, notifications, and audit trails for a SACCO. It supports member self-service through a mobile/member portal and administrative operations through web dashboards (Admin, Treasurer, Loan Officer, Teller, Credit Committee, Auditor, Customer Support).

**Scope**
- Core functions: member onboarding, savings and transaction management, loan application and approval workflow, guarantor management, bulk processing, loan history migration, notifications, and audit logging.

**Super Users & Roles**
- **Admin**: System-wide configuration, user and role management, manage loan products and eligibility rules, view high-level reports, and run migrations/backups.
- **Treasurer**: Responsible for disbursements and financial processing. Key tasks: perform bulk onboarding and bulk financial processing (savings postings, loan repayments), disburse loans, set final interest/total interest at treasurer approval stage, export financial reports, and reconcile transactions.
- **Credit Committee**: Review loan applications escalated to committee, approve or request changes, and participate in final approval workflow before treasurer stage when required.
- **Loan Officer**: Initiate or review loan applications, run pre-checks for member and guarantors, move loans through officer review and forward to credit committee as needed. Can also apply for loans on behalf of members.
- **Teller**: Operate at branch-level for member-facing activities: create or update member profiles (individual onboarding), accept loan repayments and savings deposits (when not using treasurer bulk processes), and help members with the member portal.
- **Customer Support**: Access to member contact details, open tickets, and assist with non-sensitive account issues. They do not see confidential financial calculations unless explicitly authorized.
- **Auditor**: Read-only access to transactions, audit trail, reports, and history for compliance checks and forensic analysis.

**Main Processes**

**Member Onboarding**
- **Bulk Onboarding (Treasurer)**:
  - Use the Treasurer bulk processing page to import CSVs for new members.
  - The bulk pipeline validates records (required fields, unique member numbers/employee IDs) and enqueues them for creation.
  - Successful imports create member records and initial savings accounts; failures are reported with line-level errors for correction and re-run.
  - Location in source: [backend/src/main/resources/db/migration/](backend/src/main/resources/db/migration/)

- **Individual Onboarding (Teller)**:
  - The Teller dashboard provides a member creation form capturing personal details, employee ID, and contact information.
  - Teller can attach KYC documents and open an initial savings account.

**Loan Application Flow**
- Two ways to apply:
  1. **Member-initiated (Member Portal/mobile app)**:
     - Members submit a loan application via the member portal.
     - The member portal calls the `/member/apply-loan` endpoint which sets the authenticated member as the applicant.
     - Application includes: `loanProductId`, `amount`, `termMonths`, and `guarantors` (list with guarantee amounts and selfGuarantee flag).
     - Pre-checks and eligibility validation run during application (advisory). Loans are created with status based on guarantor types (e.g., `PENDING_GUARANTOR_APPROVAL` or `PENDING_LOAN_OFFICER_REVIEW`). Interest fields remain null until treasurer stage.
  2. **Loan Officer / Admin-initiated**:
     - Loan officers can apply on behalf of a member via the Loan Officer dashboard or the web `apply-on-behalf` endpoint.
     - The officer can include guarantors and set guarantee amounts. Officer submits the application which follows the same lifecycle.

- **Approval Workflow**:
  - After application, the loan progresses through states: `PENDING` → (guarantor approvals) → `PENDING_LOAN_OFFICER_REVIEW` → `PENDING_CREDIT_COMMITTEE` → `PENDING_TREASURER` → `APPROVED` → `DISBURSED`.
  - **Treasurer** sets the final interest amounts during `PENDING_TREASURER` stage: `totalInterest`, `interestRemaining`, `totalRepayable`, `monthlyRepayment`, and `outstandingBalance`.
  - Notifications are sent to relevant roles and to the member when key state changes occur.

**Savings and Loan Repayments Recording**
- **Treasurer Bulk Processes**:
  - The Treasurer can upload CSV batches to apply savings credits or loan repayments in bulk.
  - Bulk pipeline validates transactions using member identifiers (member number or employee ID), amount, and transaction type.
  - For loan repayments, rows may specify `principal` and `interest` portions; the system will allocate amounts to reduce `interestRemaining`, `outstandingBalance` and record transactions in the `loan_repayments` table.
  - Successful bulk actions create transaction and audit log entries and update account balances.

- **Individual Posting (Teller)**:
  - Tellers can post single savings or repayment transactions through the Teller dashboard; these create account `transactions` and update balances immediately.

**Loan History Migration**
- The system supports data migration for historical loans (from legacy systems) including loans with statuses `REPAID`, `DISBURSED`, or `DEFAULTED`.
- Migration artifacts live under the migration pipeline and can be inserted via dedicated migration SQL scripts or through an import UI.
- Migration steps:
  1. Prepare a migration CSV with canonical fields (member mapping, product mapping, loan number, principal, totalInterest, termMonths, status, application/approval/disbursement dates, repayment history).
  2. Run validation: member exists or map to a new member, product exists or create mapping, totals reconcile (principal + totalInterest = totalRepayable).
  3. Insert loan records into `loans`, associated `loan_repayments` rows, and set `migration_status` to `MIGRATED` to mark imported records.
  4. Reconcile balances and run report to ensure outstanding balances and repayable amounts match source system.
- See migration SQL folder: [backend/src/main/resources/db/migration/](backend/src/main/resources/db/migration/)

**Other Important Features**
- **Notifications**: The system sends in-app and optional external notifications (email/SMS) for events like guarantor requests, approvals, disbursements, repayments, and bulk processing results. Notification logic exists in services under `backend/src/main/java/com/minet/sacco/service/NotificationService.java`.
- **Audit Trail**: All critical actions (create/update/delete, approvals, bulk imports) are logged into the `audit_logs` table with who, when, and a short description for compliance and traceability.
- **Security & Roles**: Role-based access control (RBAC) is enforced via Spring Security annotations (e.g., `@PreAuthorize`). Sensitive endpoints are restricted by role.
- **Validation & Error Handling**: Bulk operations provide row-level error reporting. Loan applications validate guarantors, amounts, and eligibility rules before acceptance.
- **Reporting**: The backend provides endpoints and SQL reports for cashflow, outstanding loans, arrears, and member balances.
- **Backups & Deployment**: Regular database backups and Flyway-managed migrations ensure controlled schema changes. Deploy with `mvn package` and run the Spring Boot JAR, ensuring environment variables and DB access are configured.

**Where to Find Key Files**
- Loan controller: [backend/src/main/java/com/minet/sacco/controller/LoanController.java](backend/src/main/java/com/minet/sacco/controller/LoanController.java)
- Member portal endpoints: [backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java](backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java)
- Bulk migration scripts: [backend/src/main/resources/db/migration/](backend/src/main/resources/db/migration/)
- Frontend pages: `minetsacco-main/src/pages` (Loans, MemberLoanApplication, BulkProcessing, etc.) — examples:
  - [minetsacco-main/src/pages/Loans.tsx](minetsacco-main/src/pages/Loans.tsx)
  - [minetsacco-main/src/pages/MemberLoanApplication.tsx](minetsacco-main/src/pages/MemberLoanApplication.tsx)
  - [minetsacco-main/src/pages/BulkProcessing.tsx](minetsacco-main/src/pages/BulkProcessing.tsx)

**Operational Notes & Recommendations**
- Keep `interest`-related fields nullable until the treasurer finalizes amounts; this avoids failing inserts during application.
- Enforce row-level validation for bulk imports and provide detailed error CSVs so finance staff can correct and re-run.
- Maintain careful role separation: only Treasurer sets final interest and disburses loans; only Credit Committee and Loan Officers advance approvals.
- Implement regular reconciliation tasks to verify `outstanding_balance` vs ledger transactions.

**Next Steps (suggested)**
- Review this document and tell me any sections to expand (diagrams, sequence flows, or sample CSV formats for bulk imports).
- I can convert this to a PDF or present as slides if needed.

---

*Document generated by the dev assist agent. Edit and expand as needed.*
