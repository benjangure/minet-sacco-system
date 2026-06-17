# Member Credentials Workflow

## Overview
Members need to be able to access their login credentials after registration. This document outlines the complete workflow for credential delivery and retrieval.

## Credential Delivery Workflows

### 1. Individual Member Registration (Teller/Customer Support)
**Flow:** Teller registers member → Modal shows credentials → Teller shares credentials with member

**Steps:**
1. Teller navigates to `/members` page
2. Clicks "Register Member" button
3. Fills in member details and submits
4. Upon success, a modal displays:
   - Member name
   - Username (member number)
   - Temporary password or "Use National ID"
   - Copy-to-clipboard buttons for both
   - Instructions for member to change password on first login

**What happens behind the scenes:**
- Member created in database
- User account created with temporary password or National ID
- MemberCredential entry created and stored with:
  - Username
  - Member name
  - Email (if provided)
  - Temporary password (encrypted in database)
  - `passwordChanged = false`
  - `emailSent = false`

**Teller's action:** Shares username and password/National ID with member through secure channel (phone, email, in-person)

---

### 2. Bulk Member Upload (Treasurer/Admin)
**Flow:** Upload Excel → Validation → Members created with credentials → Dashboard shows batch results

**Steps:**
1. Treasurer navigates to `/bulk-processing`
2. Uploads Excel file with member data
3. System validates and creates members
4. After processing, credentials are:
   - Stored in `member_credentials` table
   - Accessible from credentials dashboard
   - Listed with status "Pending Delivery"

**What happens behind the scenes:**
- Same as individual registration (per member)
- Multiple MemberCredential entries created
- All marked as `emailSent = false` initially

**Treasurer's action:** 
- Views bulk results
- Can download credentials list (when export feature is enabled)
- Shares credentials with members using preferred method
- Can mark as delivered in credentials dashboard

---

### 3. Credential Retrieval (Anytime Access)
**Flow:** Staff member visits credentials dashboard → Searches for member → Views username & password

**Access:** `/admin/member-credentials` 
- Available to: ADMIN, TREASURER, CUSTOMER_SUPPORT roles

**Steps:**
1. Navigate to Admin → Member Credentials (sidebar)
2. See table of all members with credentials
3. Search by member name, username, or email
4. Click the eye icon to view credentials modal
5. Modal displays:
   - Member name and email
   - Username (with copy button)
   - Temporary password (with show/hide and copy buttons)
   - Status (Password Changed: Yes/No, Email Sent: Yes/No)

**Security:**
- Passwords only visible if member hasn't changed them yet
- If member changed password on first login, shows message "Password has been changed by member"
- Passwords stored encrypted in database
- API requires authentication
- Role-based access control

---

## Database Schema

### member_credentials Table
```sql
CREATE TABLE member_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    password VARCHAR(255),  -- Temporary password
    has_national_id BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at TIMESTAMP NULL,
    password_changed BOOLEAN NOT NULL DEFAULT FALSE,
    password_changed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;
```

### Key Fields:
- **password:** Stores temporary password or National ID used for initial login
- **has_national_id:** Indicates if member uses National ID as password
- **password_changed:** Track if member set their own password on first login
- **email_sent:** Track if credentials were delivered via email
- **created_by:** Which staff member created the credential

---

## Backend API Endpoints

### Get All Member Credentials
```
GET /api/member-credentials
Authorization: Bearer {token}
Roles: ADMIN, TREASURER, CUSTOMER_SUPPORT

Response:
{
  "success": true,
  "message": "Member credentials retrieved successfully",
  "data": [
    {
      "id": 1,
      "memberId": 123,
      "username": "EMP001",
      "memberName": "John Doe",
      "email": "john@example.com",
      "hasNationalId": true,
      "emailSent": false,
      "passwordChanged": false,
      "createdAt": "2026-06-17T10:30:00"
    }
  ]
}
```

### Get Password for Credential
```
GET /api/member-credentials/{id}/password
Authorization: Bearer {token}
Roles: ADMIN, TREASURER, CUSTOMER_SUPPORT

Response (if not changed):
{
  "success": true,
  "message": "Password retrieved",
  "data": {
    "password": "temporary_password_here"
  }
}

Response (if changed):
{
  "success": true,
  "message": "Password has been changed by member",
  "data": {
    "password": null
  }
}
```

### Search Credentials
```
GET /api/member-credentials/search?query=john
Authorization: Bearer {token}
Roles: ADMIN, TREASURER, CUSTOMER_SUPPORT
```

### Get Credentials by Member ID
```
GET /api/member-credentials/member/{memberId}
Authorization: Bearer {token}
Roles: ADMIN, TREASURER, CUSTOMER_SUPPORT
```

### Get Pending Email Credentials
```
GET /api/member-credentials/pending-email
Authorization: Bearer {token}
Roles: ADMIN
```

### Get Unchanged Password Credentials
```
GET /api/member-credentials/password-not-changed
Authorization: Bearer {token}
Roles: ADMIN
```

---

## User Experience Flow

### For Teller (Individual Registration)
```
1. Open Members page
2. Click "Register Member"
3. Fill form → Submit
4. SUCCESS: Modal pops up with credentials
5. Copy username & password
6. Click "Done"
7. Share credentials with member via secure channel
```

### For Treasurer (Bulk Registration)
```
1. Open Bulk Processing page
2. Upload Excel file
3. Wait for processing
4. View results - shows how many members created
5. Navigate to Member Credentials dashboard
6. Find members from recent upload (shows recent first)
7. Click eye icon for each member
8. Copy credentials
9. Share with members (email, WhatsApp, SMS, in-person)
```

### For Admin (Credential Lookup)
```
1. Open Member Credentials dashboard
2. Search for member by name/username
3. Click eye icon
4. View/copy credentials
5. Note: If password changed, can't see it anymore
```

---

## Password Reset/Recovery

Members who forget their password:
1. Use password reset on member app login page
2. System sends reset link to their email (when email is configured)
3. Or admin can help reset via admin interface (to be implemented)

---

## Email Integration (Future)

When SMTP is configured:
- Automatic email with credentials sent to member
- Email marked as "Sent" in dashboard
- Link to set up new password in email
- Reduces manual credential delivery burden

**To enable email:**
1. Configure SMTP in `application.properties`
2. System automatically sends credentials after member creation
3. Staff still has dashboard for manual delivery if needed

---

## Summary

✅ **Members can access credentials:**
- Immediately after registration (modal display)
- Anytime via dashboard for retrieval
- Both username and password visible

✅ **Credentials are secure:**
- Encrypted in database
- Only visible to authorized staff
- Staff must be logged in
- Role-based access control

✅ **Tracking:**
- Know who created each credential
- Track if password was changed
- Track if email was sent
- History of credential creation

✅ **No backend logs needed:**
- Passwords stored in database
- Dashboard provides UI access
- Copy-to-clipboard for easy sharing
- Email integration ready when SMTP configured
