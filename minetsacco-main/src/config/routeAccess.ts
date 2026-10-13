/**
 * Staff route -> allowed roles. Mirrors the sidebar visibility so that a user who types a URL
 * directly cannot open a screen their role is not meant to use (the API enforces the same rules).
 * Routes not listed here are open to any authenticated staff user.
 */
export const ROUTE_ROLES: Record<string, string[]> = {
  "/members": ["ADMIN", "TREASURER", "LOAN_OFFICER", "TELLER", "CUSTOMER_SUPPORT", "CREDIT_COMMITTEE"],
  "/loans": ["ADMIN", "LOAN_OFFICER", "CREDIT_COMMITTEE", "TREASURER"],
  "/loan-repayment-requests": ["ADMIN", "TELLER", "TREASURER", "LOAN_OFFICER", "CREDIT_COMMITTEE"],
  "/loan-repayment-recording": ["TELLER", "TREASURER", "ADMIN"],
  "/savings": ["ADMIN", "TREASURER", "TELLER"],
  "/member-transaction-history": ["ADMIN", "TREASURER", "LOAN_OFFICER", "CREDIT_COMMITTEE", "AUDITOR"],
  "/teller-member-context": ["TELLER"],
  "/customer-support-portal": ["CUSTOMER_SUPPORT", "ADMIN"],
  "/bulk-processing": ["TREASURER", "CREDIT_COMMITTEE"],
  "/loan-migration": ["TREASURER", "ADMIN"],
  "/reports": ["ADMIN", "TREASURER", "AUDITOR"],
  "/over-committed-guarantors": ["ADMIN", "TREASURER", "AUDITOR", "LOAN_OFFICER"],
  "/gl-configuration": ["ADMIN", "TREASURER"],
  "/gl-manual-entries": ["TREASURER", "ADMIN"],
  "/kyc-upload": ["CUSTOMER_SUPPORT", "ADMIN"],
  "/kyc-document-upload": ["CUSTOMER_SUPPORT", "ADMIN"],
  "/kyc-upload-tracking": ["CUSTOMER_SUPPORT", "ADMIN"],
  "/kyc-approval": ["TELLER", "ADMIN"],
  "/view-documents": ["TELLER", "ADMIN"],
  "/admin/users": ["ADMIN", "TREASURER"],
  "/admin/member-credentials": ["ADMIN", "TREASURER", "CUSTOMER_SUPPORT"],
  "/admin/loan-products": ["ADMIN"],
  "/admin/fund-configuration": ["ADMIN"],
  "/admin/loan-eligibility-rules": ["ADMIN"],
  "/admin/audit-trail": ["ADMIN", "AUDITOR"],
  "/audit-reports": ["ADMIN", "AUDITOR"],
  "/admin/member-suspension": ["ADMIN", "CREDIT_COMMITTEE", "TREASURER"],
  "/admin/member-exit": ["ADMIN", "CREDIT_COMMITTEE", "TREASURER"],
};

export function isRouteAllowed(pathname: string, role?: string | null): boolean {
  const allowed = ROUTE_ROLES[pathname.replace(/\/+$/, "") || "/"];
  if (!allowed) return true;
  if (!role) return false;
  return allowed.includes(role.replace(/^ROLE_/, "").toUpperCase());
}
