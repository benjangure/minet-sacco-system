# Password Change Feature - Architecture Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND LAYER                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  STAFF PORTAL              MEMBER PORTAL                        │
│  ┌──────────────┐          ┌──────────────┐                    │
│  │ Settings.tsx │          │MemberSettings│                    │
│  │              │          │    .tsx      │                    │
│  │ - Profile    │          │              │                    │
│  │ - Security   │          │ - Backend    │                    │
│  │   (Password) │          │ - Security   │                    │
│  │              │          │   (Password) │                    │
│  └──────┬───────┘          └──────┬───────┘                    │
│         │                         │                            │
│         │ handleChangePassword()  │ handleChangePassword()     │
│         │ (Basic Validation)      │ (Enhanced Validation)      │
│         │                         │                            │
│         └────────────┬────────────┘                            │
│                      │                                         │
│              Client-Side Validation                            │
│              ✓ Password match                                  │
│              ✓ Min 8 characters                                │
│              ✓ Not empty (member)                              │
│              ✓ New ≠ Current (member)                          │
│                      │                                         │
└──────────────────────┼─────────────────────────────────────────┘
                       │
                       │ HTTP PUT Request
                       │ Authorization: Bearer {JWT}
                       │
┌──────────────────────┼─────────────────────────────────────────┐
│                      ▼                                         │
│              BACKEND LAYER (Spring Boot)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  UserController                MemberPortalController          │
│  ┌──────────────────┐          ┌──────────────────┐            │
│  │ changeOwnPassword│          │changeMemberPassword           │
│  │                  │          │                  │            │
│  │ PUT /api/users/  │          │ PUT /api/member/ │            │
│  │ change-password  │          │ change-password  │            │
│  └────────┬─────────┘          └────────┬─────────┘            │
│           │                             │                     │
│           │ Server-Side Validation      │                     │
│           │ ✓ Verify current password   │                     │
│           │ ✓ New ≠ Current             │                     │
│           │ ✓ Confirmation matches      │                     │
│           │                             │                     │
│           └────────────┬────────────────┘                     │
│                        │                                      │
│                        ▼                                      │
│              ┌──────────────────┐                             │
│              │  UserService     │                             │
│              │  changePassword()│                             │
│              └────────┬─────────┘                             │
│                       │                                       │
│                       ▼                                       │
│              ┌──────────────────┐                             │
│              │ PasswordEncoder  │                             │
│              │ (BCrypt Strength │                             │
│              │      10)         │                             │
│              └────────┬─────────┘                             │
│                       │                                       │
└───────────────────────┼───────────────────────────────────────┘
                        │
┌───────────────────────┼───────────────────────────────────────┐
│                       ▼                                       │
│              DATABASE LAYER (MySQL)                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────┐                      │
│  │ users table                          │                      │
│  ├──────────────────────────────────────┤                      │
│  │ id          │ username │ password    │                      │
│  │ (PK)        │ (String) │ (Hashed)    │                      │
│  ├──────────────────────────────────────┤                      │
│  │ 1           │ EMP001   │ $2a$10$... │ ◄─── Updated         │
│  │ 2           │ EMP002   │ $2a$10$... │                      │
│  │ ...         │ ...      │ ...        │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                        │
                        │ Email Service
                        │
┌───────────────────────┼───────────────────────────────────────┐
│                       ▼                                       │
│              EMAIL SERVICE LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────┐                      │
│  │ sendPasswordChangeConfirmation()      │                      │
│  │                                      │                      │
│  │ To: user@email.com                   │                      │
│  │ Subject: Password Changed            │                      │
│  │ Body: Confirmation message           │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                        │
                        │ Audit Log
                        │
┌───────────────────────┼───────────────────────────────────────┐
│                       ▼                                       │
│              AUDIT LOGGING LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────┐                      │
│  │ Activity Log Entry                   │                      │
│  ├──────────────────────────────────────┤                      │
│  │ User: EMP001                         │                      │
│  │ Action: Password Changed             │                      │
│  │ Timestamp: 2026-05-14 15:30:00       │                      │
│  │ Status: SUCCESS                      │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagram

```
User Input
    │
    ▼
┌─────────────────────────────────────┐
│ Client-Side Validation              │
│ ✓ currentPassword not empty         │
│ ✓ newPassword not empty             │
│ ✓ newPassword === confirmPassword   │
│ ✓ newPassword.length >= 8           │
│ ✓ currentPassword !== newPassword    │
└─────────────────────────────────────┘
    │
    ├─ FAIL ──► Show Error Toast ──► Stop
    │
    ▼
┌─────────────────────────────────────┐
│ Send HTTP PUT Request               │
│ Headers:                            │
│ - Content-Type: application/json    │
│ - Authorization: Bearer {JWT}       │
│ Body:                               │
│ - currentPassword                   │
│ - newPassword                       │
│ - confirmPassword                   │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Server-Side Validation              │
│ ✓ Verify current password           │
│ ✓ Check new ≠ current               │
│ ✓ Check confirmation matches        │
└─────────────────────────────────────┘
    │
    ├─ FAIL ──► Return 400 Error ──► Show Error Toast ──► Stop
    │
    ▼
┌─────────────────────────────────────┐
│ Hash New Password (BCrypt)          │
│ Strength: 10                        │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Update Database                     │
│ UPDATE users                        │
│ SET password = {hashed}             │
│ WHERE id = {userId}                 │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Send Confirmation Email             │
│ To: user@email.com                  │
│ Subject: Password Changed           │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Log Activity                        │
│ Action: Password Changed            │
│ User: {username}                    │
│ Timestamp: {now}                    │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Return Success Response             │
│ Status: 200 OK                      │
│ Message: "Password changed..."      │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Frontend Success Handling           │
│ ✓ Show success toast                │
│ ✓ Clear form fields                 │
│ ✓ Reset eye icon states             │
│ ✓ Redirect (optional)               │
└─────────────────────────────────────┘
```

## Component Interaction Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    MemberSettings.tsx                        │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ State Management                                       │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ • currentPassword                                      │ │
│  │ • newPassword                                          │ │
│  │ • confirmPassword                                      │ │
│  │ • passwordLoading                                      │ │
│  │ • showCurrentPassword                                  │ │
│  │ • showNewPassword                                      │ │
│  │ • showConfirmPassword                                  │ │
│  └────────────────────────────────────────────────────────┘ │
│                          │                                   │
│                          ▼                                   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Form Rendering                                         │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ • Current Password Input + Eye Icon                    │ │
│  │ • New Password Input + Eye Icon                        │ │
│  │ • Confirm Password Input + Eye Icon                    │ │
│  │ • Change Password Button                               │ │
│  │ • Security Tips Card                                   │ │
│  └────────────────────────────────────────────────────────┘ │
│                          │                                   │
│                          ▼                                   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Event Handlers                                         │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ • handleChangePassword()                               │ │
│  │ • setShowCurrentPassword()                             │ │
│  │ • setShowNewPassword()                                 │ │
│  │ • setShowConfirmPassword()                             │ │
│  └────────────────────────────────────────────────────────┘ │
│                          │                                   │
│                          ▼                                   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ External Dependencies                                  │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ • useAuth() - Get JWT token                            │ │
│  │ • useToast() - Show notifications                      │ │
│  │ • fetch() - Make API calls                             │ │
│  │ • MemberLayout - Wrapper component                     │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## Security Layers Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Layer 1: HTTPS/TLS                                        │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Encrypts data in transit                            │  │
│  │ Status: Ready for production                        │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                 │
│  Layer 2: JWT Authentication                              │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Verifies user identity                              │  │
│  │ Token expires after 24 hours                        │  │
│  │ Status: ✅ Implemented                              │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                 │
│  Layer 3: Current Password Verification                   │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Prevents unauthorized password changes              │  │
│  │ Uses BCrypt comparison                              │  │
│  │ Status: ✅ Implemented                              │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                 │
│  Layer 4: Input Validation                                │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Client-side: Format & length checks                 │  │
│  │ Server-side: Comprehensive validation               │  │
│  │ Status: ✅ Implemented                              │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                 │
│  Layer 5: Password Hashing                                │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Algorithm: BCrypt                                   │  │
│  │ Strength: 10                                        │  │
│  │ Status: ✅ Implemented                              │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                 │
│  Layer 6: Audit Logging                                   │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Logs all password changes                           │  │
│  │ Tracks user, timestamp, status                      │  │
│  │ Status: ✅ Implemented                              │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                 │
│  Layer 7: Email Confirmation                              │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ Notifies user of password change                    │  │
│  │ Provides security alert                             │  │
│  │ Status: ✅ Implemented                              │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Navigation Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    STAFF PORTAL                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Dashboard                                                 │
│      │                                                     │
│      ▼                                                     │
│  AppSidebar                                                │
│      │                                                     │
│      ├─ Main Menu                                          │
│      │   ├─ Dashboard                                      │
│      │   ├─ Members                                        │
│      │   └─ ...                                            │
│      │                                                     │
│      └─ Administration                                     │
│          ├─ User Management                                │
│          ├─ Loan Products                                  │
│          ├─ Fund Configuration                             │
│          ├─ Loan Eligibility Rules                         │
│          ├─ Audit Trail                                    │
│          ├─ Audit Reports                                  │
│          ├─ System Settings                                │
│          ├─ Member Suspension                              │
│          ├─ Member Exit                                    │
│          └─ Settings ◄─── CLICK HERE                       │
│                                                             │
│      ▼                                                     │
│  Settings.tsx (/settings)                                  │
│      │                                                     │
│      ├─ Profile Tab                                        │
│      │   ├─ User Information                               │
│      │   └─ Role & Permissions                             │
│      │                                                     │
│      └─ Security Tab                                       │
│          ├─ Change Password Form                           │
│          │   ├─ Current Password (Eye Icon)                │
│          │   ├─ New Password (Eye Icon)                    │
│          │   ├─ Confirm Password (Eye Icon)                │
│          │   └─ Change Password Button                     │
│          └─ Security Tips                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    MEMBER PORTAL                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Dashboard                                                 │
│      │                                                     │
│      ├─ MemberSidebar                                      │
│      │   ├─ Home                                           │
│      │   ├─ Transactions                                   │
│      │   ├─ My Account                                     │
│      │   ├─ Loans                                          │
│      │   ├─ My Guarantees                                  │
│      │   ├─ Reports                                        │
│      │   ├─ Notifications                                  │
│      │   └─ Settings ◄─── CLICK HERE (Sidebar)            │
│      │                                                     │
│      └─ MemberLayout (Mobile Top Navbar)                   │
│          ├─ Home Tab                                       │
│          ├─ Transact Tab                                   │
│          ├─ My Account Tab                                 │
│          ├─ Loans Tab                                      │
│          ├─ Deposits Tab                                   │
│          ├─ Reports Tab                                    │
│          ├─ Notifications Tab                              │
│          └─ Settings Tab ◄─── CLICK HERE (Mobile)          │
│                                                             │
│      ▼                                                     │
│  MemberSettings.tsx (/member/settings)                     │
│      │                                                     │
│      ├─ Backend Configuration Tab                          │
│      │   ├─ Current Backend URL                            │
│      │   ├─ New Backend URL Input                          │
│      │   ├─ Test Connection Button                         │
│      │   ├─ Save Changes Button                            │
│      │   └─ How to Find Backend URL                        │
│      │                                                     │
│      └─ Security Tab                                       │
│          ├─ Change Password Form                           │
│          │   ├─ Current Password (Eye Icon)                │
│          │   ├─ New Password (Eye Icon)                    │
│          │   ├─ Confirm Password (Eye Icon)                │
│          │   └─ Change Password Button                     │
│          └─ Security Tips                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

**Architecture Version**: 1.0
**Last Updated**: May 14, 2026
**Status**: Production Ready ✅
