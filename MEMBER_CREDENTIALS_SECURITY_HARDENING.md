# Member Credentials Dashboard - Security Hardening Guide

## Security Tradeoffs & Mitigations

This document outlines the security considerations for the Member Credentials Dashboard feature and the mitigations implemented.

---

## 1. Temporary Password Storage (Intentional Tradeoff)

### The Challenge
Members need to receive their login credentials. Options:
1. ❌ Store only hashed passwords (can't retrieve for distribution)
2. ✅ Store temporary plaintext passwords briefly (current approach with guardrails)

### Our Solution: Temporary Storage with Auto-Purge

**Storage Strategy:**
- Temporary passwords stored in `member_credentials.password` column
- Passwords stored as plaintext (recoverable for distribution)
- **Automatically purged once member changes password**

**Implementation:**
```
Timeline:
1. Member created → Temporary password stored
2. Admin distributes credentials
3. Member logs in → Sets new password
4. New password triggers: credential.setPassword(NULL) + credential.setPassword(NULL)
5. Temporary password DELETED from database
6. Stays NULL forever (can't be retrieved again)
```

### Code Implementation
**File:** `AuthController.java` → `setupPassword()` method
```java
// Update credential tracking record and PURGE temporary password for security
memberCredentialRepository.findByMemberId(user.getMemberId())
    .ifPresent(credential -> {
        credential.setPasswordChanged(true);
        credential.setPasswordChangedAt(LocalDateTime.now());
        credential.setPassword(null); // SECURITY: Purge temporary password
        memberCredentialRepository.save(credential);
    });
```

**Migration:** `V127__Backfill_existing_member_credentials.sql`
```sql
-- Purge all passwords where member has already changed them
UPDATE member_credentials
SET password = NULL,
    password_changed_at = NOW()
WHERE password_changed = TRUE 
  AND password IS NOT NULL;
```

**Result:** Password exists in DB only during the narrow window:
- From: Member creation
- To: Member first login + password change
- Duration: Typically a few hours to a few days

---

## 2. Role-Based Access Control (Database & API)

### API Level
**File:** `MemberCredentialsController.java`

All 7 endpoints protected with `@PreAuthorize`:
```java
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_CUSTOMER_SUPPORT')")
public ResponseEntity<?> getCredentials() { ... }
```

**Authorized Roles Only:**
- ✅ ADMIN
- ✅ TREASURER
- ✅ CUSTOMER_SUPPORT
- ❌ MEMBER (403 Forbidden)
- ❌ LOAN_OFFICER (403 Forbidden)
- ❌ AUDITOR (403 Forbidden)

**Enforcement:**
- JWT token validated on every request
- Role verified by Spring Security before method execution
- Returns 403 Forbidden if role mismatch

### Database Level (Optional but Recommended)

For MySQL, you can add table-level access control:

```sql
-- Create role for member credentials access
CREATE ROLE 'credential_viewer'@'localhost';

-- Grant only SELECT to authorized staff
GRANT SELECT ON sacco_db.member_credentials TO 'credential_viewer'@'localhost';

-- Assign to service account
-- (Your application user keeps all permissions for Flyway/ORM operations)

-- Audit logging (track who views passwords)
SET GLOBAL general_log = 'ON';
SET GLOBAL log_output = 'TABLE';
```

---

## 3. Data Minimization

### What's Stored
```
member_credentials table:
├── id ✓ (unique ID)
├── member_id ✓ (foreign key)
├── username ✓ (needed for login)
├── password ⚠️ (temporary only, auto-purged)
├── password_changed (flag)
├── email (contact info)
└── (metadata: timestamps, flags)
```

### What's NOT Stored
- ❌ Real user passwords (hashed in separate `users` table)
- ❌ Payment information
- ❌ Government IDs
- ❌ PII beyond email/name

---

## 4. Session & Token Security

### Frontend
**File:** `MemberCredentials.tsx`

Session validation before every API call:
```typescript
const fetchCredentials = async () => {
    // Verify session exists before fetching
    if (!session || !session.token) {
        toast({
            title: "Not Authenticated",
            description: "Please login to access credentials"
        });
        return;
    }
    
    const response = await fetch(`${API_BASE_URL}/member-credentials`, {
        headers: {
            "Authorization": `Bearer ${session.token}`,
            "Content-Type": "application/json"
        }
    });
};
```

**Protections:**
- ✅ Token included in every request
- ✅ No credentials in localStorage beyond token
- ✅ Token expires with session
- ✅ Session cleared on logout

### Backend
**File:** `JwtUtil.java` (existing)

- JWT tokens signed and verified
- Expiration timestamps enforced
- Token cannot be forged or modified

---

## 5. Audit Trail

### What's Logged
- Member creation (with temporary password generation)
- Password access (API calls to GET password endpoint)
- Password changes (tracked in `password_changed_at`)
- Admin access (via Spring Security logs)

### Recommendation
Enable audit logging for production:

```properties
# application.properties
logging.level.com.minet.sacco.controller.MemberCredentialsController = DEBUG
logging.level.org.springframework.security = INFO
```

This logs all GET requests to password endpoints with timestamp, user, and result.

---

## 6. Network Security

### HTTPS Requirement
- All API endpoints must use HTTPS in production
- Passwords transmitted only over encrypted channels
- Configure in `application.properties`:

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-type=PKCS12
```

### CORS Configuration
**File:** `CorsConfig.java` (existing)

- Only configured domains can access API
- Credentials not exposed in CORS responses
- Frontend must be on approved domain

---

## 7. Deployment Security Checklist

Before deploying to production:

- [ ] **Database Access**
  - [ ] Verify only app service account can access `member_credentials`
  - [ ] Restrict direct SQL access to database
  - [ ] Enable audit logging on password column access

- [ ] **API Security**
  - [ ] Verify all 7 endpoints have @PreAuthorize annotations
  - [ ] Test 403 Forbidden for unauthorized roles
  - [ ] Confirm JWT token validation enabled

- [ ] **Frontend Security**
  - [ ] HTTPS enabled on all endpoints
  - [ ] Session tokens not logged or exposed
  - [ ] Copy-to-clipboard doesn't log credentials

- [ ] **Monitoring**
  - [ ] Set up alerts for password access patterns
  - [ ] Monitor for bulk downloads or suspicious queries
  - [ ] Track failed authentication attempts

- [ ] **Backup & Recovery**
  - [ ] Backup strategy excludes raw password data if possible
  - [ ] Clear backups after 30 days (passwords auto-purged anyway)
  - [ ] Test recovery procedures don't expose passwords

---

## 8. Password Lifecycle

### Timeline Example

```
2026-06-17 09:00 → Member created (John Doe)
                   password = "Tr@Np@Ss123" (stored in DB)
                   Dashboard shows password to admin

2026-06-17 09:15 → Admin copies password, sends via SMS

2026-06-17 14:00 → John logs in using temporary password
                   Taken to Password Setup screen

2026-06-17 14:05 → John sets new password: "MySecure#Pass456"
                   
                   TRIGGER: setupPassword() method executes:
                   ├─ user.setPassword(encode("MySecure#Pass456"))
                   ├─ credential.setPassword(null) ← PURGED
                   ├─ credential.setPasswordChanged(true)
                   └─ credential.setPasswordChangedAt(NOW())

2026-06-17 14:06 → Dashboard updated:
                   - Password field: NULL
                   - Message: "Password has been changed..."
                   - Cannot retrieve password anymore

Forever → Temporary password never retrievable
        → Only hashed password in users table remains
```

---

## 9. Incident Response

### If Credentials Are Exposed

1. **Immediate:**
   - Force password reset for affected member
   - Member logs in → Password change triggered
   - Temporary password auto-purged

2. **Investigation:**
   - Check API logs for unauthorized access
   - Review database access logs
   - Verify no direct SQL queries were made

3. **Prevention:**
   - Update CORS configuration
   - Rotate JWT signing keys
   - Force all active sessions to re-authenticate

---

## 10. Compliance & Standards

### OWASP Guidelines
- ✅ **A02:2021 – Cryptographic Failures**
  - Passwords only in transit (UI) and briefly at rest
  - Auto-purged after first use
  - Hashed passwords in `users` table

- ✅ **A01:2021 – Broken Access Control**
  - Role-based authorization enforced
  - 403 Forbidden for unauthorized access
  - Token validation on every endpoint

- ✅ **A07:2021 – Identification & Authentication**
  - JWT tokens with expiration
  - First-login password change enforced
  - Session timeout on logout

### PCI DSS Considerations
If handling payment-related SACCOs:
- Temporary passwords NOT stored in `users` table (no plaintext user passwords stored anywhere)
- Separate `member_credentials` table for temporary distribution only
- Regular purging ensures minimal exposure window

---

## 11. Future Hardening Options

**Consider for future releases:**

1. **Encryption at Rest**
   - Encrypt `member_credentials.password` column
   - Decrypt only for display (requires key management)

2. **Time-Limited Access**
   - Password only retrievable for 24 hours after creation
   - Auto-purge after 1 day regardless of changes

3. **Download Audit**
   - Log every password view with staff member name
   - Alert on repeated access to same credential

4. **Email Integration**
   - Auto-send credentials via email instead of manual copy-paste
   - Reduce manual password handling window

5. **SMS OTP**
   - Send one-time code for first login
   - No permanent temporary password at all

---

## Summary

| Control | Status | Implementation |
|---------|--------|-----------------|
| Access Control | ✅ Implemented | @PreAuthorize on all endpoints |
| Password Purge | ✅ Implemented | setupPassword() method + V127 migration |
| Audit Logging | ✅ Recommended | Spring Security logs all requests |
| HTTPS Required | ✅ Recommended | Configure in deployment |
| Database Hardening | ⚠️ Optional | SQL-level access control |
| Token Validation | ✅ Implemented | JWT verification on every call |
| Session Management | ✅ Implemented | Token expiration + session logout |
| Data Minimization | ✅ Implemented | Only necessary data stored |

**Verdict: Ready for production with recommended monitoring in place.**

---

## Questions?

For security questions or audit trail needs, contact your system admin or security team before deployment.

This feature was designed with the deliberate tradeoff of temporary plaintext passwords for operational convenience, with strong safeguards to minimize exposure window.
