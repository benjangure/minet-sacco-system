# Password Change Feature - Visual Guide

## User Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    MEMBER PORTAL LOGIN                          │
│                                                                 │
│  Username: member_test                                          │
│  Password: ••••••••••                                           │
│                                                                 │
│  [Login Button]                                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    MEMBER DASHBOARD                             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Sidebar                                                 │   │
│  │ ├─ Dashboard                                            │   │
│  │ ├─ Loans                                                │   │
│  │ ├─ Savings                                              │   │
│  │ ├─ Settings ← CLICK HERE                                │   │
│  │ └─ Logout                                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  OR click Settings tab in top navbar (mobile)                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SETTINGS PAGE                                │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ [Backend Configuration] [Security] ← CLICK HERE         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Backend Configuration Tab                                      │
│  (Configure backend URL)                                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SECURITY TAB                                 │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🔒 Change Password                                      │   │
│  │                                                         │   │
│  │ Current Password                                        │   │
│  │ [••••••••••] 👁️ ← Eye icon to toggle visibility        │   │
│  │                                                         │   │
│  │ New Password                                            │   │
│  │ [••••••••••] 👁️ ← Eye icon to toggle visibility        │   │
│  │                                                         │   │
│  │ Confirm New Password                                    │   │
│  │ [••••••••••] 👁️ ← Eye icon to toggle visibility        │   │
│  │                                                         │   │
│  │ [Change Password Button]                                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Security Tips                                                  │
│  • Use a strong password with at least 8 characters            │
│  • Include uppercase, lowercase, numbers, and special chars    │
│  • Don't share your password with anyone                       │
│  • Change your password regularly                              │
│  • Log out when you're done using the system                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    [Click Change Password]
                              ↓
                    ┌─────────────────┐
                    │ VALIDATION      │
                    └─────────────────┘
                              ↓
                    ┌─────────────────────────────────────┐
                    │ Frontend Checks:                    │
                    │ ✓ Current password not empty        │
                    │ ✓ New password not empty            │
                    │ ✓ Passwords match                   │
                    │ ✓ 8+ characters                     │
                    │ ✓ New ≠ current                     │
                    └─────────────────────────────────────┘
                              ↓
                    ┌─────────────────────────────────────┐
                    │ Send to Backend                     │
                    │ PUT /api/member/change-password     │
                    │ + JWT Token                         │
                    └─────────────────────────────────────┘
                              ↓
                    ┌─────────────────────────────────────┐
                    │ Backend Checks:                     │
                    │ ✓ JWT token valid                   │
                    │ ✓ User authenticated                │
                    │ ✓ Current password correct          │
                    │ ✓ Passwords match                   │
                    │ ✓ New ≠ current                     │
                    └─────────────────────────────────────┘
                              ↓
                    ┌─────────────────────────────────────┐
                    │ Update Password                     │
                    │ • Hash with BCrypt                  │
                    │ • Save to database                  │
                    │ • Send confirmation email           │
                    │ • Log to audit trail                │
                    └─────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SUCCESS MESSAGE                              │
│                                                                 │
│  ✅ Success                                                     │
│  Password changed successfully                                  │
│                                                                 │
│  Form clears automatically                                      │
│  Eye icons reset to hidden state                                │
│  Confirmation email sent to your email address                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    SUBMIT PASSWORD CHANGE                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────────────┐
                    │ FRONTEND CHECK  │
                    └─────────────────┘
                              ↓
                    ┌─────────────────────────────────────┐
                    │ Is current password empty?          │
                    └─────────────────────────────────────┘
                         YES ↓ NO ↓
                            │  │
                    ┌───────┘  └──────────────────┐
                    ↓                             ↓
            ❌ Error:                    ┌─────────────────────┐
            "Please enter your           │ Is new password     │
            current password"            │ empty?              │
                                         └─────────────────────┘
                                              YES ↓ NO ↓
                                                 │  │
                                         ┌───────┘  └──────────┐
                                         ↓                     ↓
                                 ❌ Error:            ┌──────────────────┐
                                 "Please enter a      │ Do passwords     │
                                 new password"        │ match?           │
                                                      └──────────────────┘
                                                           YES ↓ NO ↓
                                                              │  │
                                                      ┌───────┘  └──────┐
                                                      ↓                 ↓
                                              ✓ Continue      ❌ Error:
                                                              "New passwords
                                                              do not match"
                                                      ↓
                                              ┌──────────────────┐
                                              │ Is password      │
                                              │ 8+ characters?   │
                                              └──────────────────┘
                                                   YES ↓ NO ↓
                                                      │  │
                                              ┌───────┘  └──────┐
                                              ↓                 ↓
                                      ✓ Continue      ❌ Error:
                                                      "Password must be
                                                      at least 8 chars"
                                              ↓
                                      ┌──────────────────┐
                                      │ Is new ≠ current?│
                                      └──────────────────┘
                                           YES ↓ NO ↓
                                              │  │
                                      ┌───────┘  └──────┐
                                      ↓                 ↓
                                ✓ Continue      ❌ Error:
                                                "New password must
                                                be different from
                                                current password"
                                      ↓
                        ┌─────────────────────────────┐
                        │ SEND TO BACKEND             │
                        │ PUT /api/member/change-pwd  │
                        │ + JWT Token                 │
                        └─────────────────────────────┘
                                      ↓
                        ┌─────────────────────────────┐
                        │ BACKEND RESPONSE            │
                        └─────────────────────────────┘
                                      ↓
                    ┌─────────────────────────────────────┐
                    │ Response Status?                    │
                    └─────────────────────────────────────┘
                         │         │         │
                    200  │    401  │    400  │    500
                         ↓         ↓         ↓         ↓
                    ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
                    │SUCCESS │ │SESSION │ │ ERROR  │ │SERVER  │
                    │        │ │EXPIRED │ │        │ │ ERROR  │
                    └────────┘ └────────┘ └────────┘ └────────┘
                         ↓         ↓         ↓         ↓
                    ✅ Password  ❌ Session ❌ Invalid ❌ Server
                    changed     expired    password  error
                    
                    Form       Log out    Try       Try
                    clears     & log in   again     again
                    
                    Email      Try again
                    sent       after
                              login
```

---

## Eye Icon Toggle Behavior

```
┌─────────────────────────────────────────────────────────────────┐
│                    PASSWORD FIELD                               │
│                                                                 │
│  Initial State (Hidden):                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ [••••••••••••••••••••••••••••••••••••••••••••] 👁️ OFF   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  After Clicking Eye Icon (Visible):                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ [MyPassword123!••••••••••••••••••••••••••••••••] 👁️ ON  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  After Clicking Eye Icon Again (Hidden):                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ [••••••••••••••••••••••••••••••••••••••••••••] 👁️ OFF   │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘

Icon States:
👁️  = Eye icon (password visible)
👁️‍🗨️ = Eye-off icon (password hidden)

Clicking toggles between:
👁️ ↔️ 👁️‍🗨️
```

---

## Session Expiration Scenario

```
┌─────────────────────────────────────────────────────────────────┐
│                    LOGIN                                        │
│                                                                 │
│  Receive JWT Token                                              │
│  Valid for: 24 hours                                            │
│  Expires at: [Current Time + 24 hours]                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────────────┐
                    │ USE SYSTEM      │
                    │ (0-24 hours)    │
                    └─────────────────┘
                              ↓
                    ┌─────────────────┐
                    │ 24 HOURS PASS   │
                    │ TOKEN EXPIRES   │
                    └─────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    TRY TO CHANGE PASSWORD                       │
│                                                                 │
│  Send Request with Expired Token                                │
│  Authorization: Bearer [EXPIRED_TOKEN]                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    BACKEND RESPONSE                             │
│                                                                 │
│  Status: 401 Unauthorized                                       │
│  Message: "Token expired"                                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    FRONTEND SHOWS                               │
│                                                                 │
│  ❌ Session Expired                                             │
│  Your session has expired. Please log out and log back in       │
│  to continue.                                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    USER CLICKS LOGOUT                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    LOGIN AGAIN                                  │
│                                                                 │
│  Receive NEW JWT Token                                          │
│  Valid for: 24 hours                                            │
│  Expires at: [Current Time + 24 hours]                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    TRY PASSWORD CHANGE AGAIN                    │
│                                                                 │
│  Send Request with FRESH Token                                  │
│  Authorization: Bearer [FRESH_TOKEN]                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SUCCESS!                                     │
│                                                                 │
│  ✅ Password changed successfully                               │
│  📧 Confirmation email sent                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Password Validation Rules

```
┌─────────────────────────────────────────────────────────────────┐
│                    PASSWORD VALIDATION                          │
│                                                                 │
│  Rule 1: Minimum Length                                         │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ❌ Short1      (6 chars)                                │   │
│  │ ❌ Pass123     (7 chars)                                │   │
│  │ ✅ MyPass123   (8 chars) ← MINIMUM                      │   │
│  │ ✅ MyPassword123! (14 chars) ← BETTER                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Rule 2: Must Match Confirmation                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ New Password:        MyPassword123                      │   │
│  │ Confirm Password:    MyPassword123                      │   │
│  │ Result: ✅ MATCH                                        │   │
│  │                                                         │   │
│  │ New Password:        MyPassword123                      │   │
│  │ Confirm Password:    MyPassword124                      │   │
│  │ Result: ❌ DON'T MATCH                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Rule 3: Must Differ from Current                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Current Password:    OldPassword123                     │   │
│  │ New Password:        OldPassword123                     │   │
│  │ Result: ❌ SAME (NOT ALLOWED)                           │   │
│  │                                                         │   │
│  │ Current Password:    OldPassword123                     │   │
│  │ New Password:        NewPassword456                     │   │
│  │ Result: ✅ DIFFERENT (ALLOWED)                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Rule 4: Recommended Complexity                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ❌ password123     (no uppercase, no special chars)     │   │
│  │ ❌ PASSWORD123     (no lowercase, no special chars)     │   │
│  │ ❌ Password        (no numbers, no special chars)       │   │
│  │ ✅ Password123     (uppercase, lowercase, numbers)     │   │
│  │ ✅ Password123!    (uppercase, lowercase, numbers,     │   │
│  │                     special chars) ← BEST              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Security Features Visualization

```
┌─────────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                              │
│                                                                 │
│  Layer 1: Frontend Validation                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ✓ Empty field checks                                    │   │
│  │ ✓ Password match validation                             │   │
│  │ ✓ Minimum length check                                  │   │
│  │ ✓ Uniqueness check (new ≠ current)                      │   │
│  │ ✓ Real-time feedback                                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                  │
│  Layer 2: JWT Authentication                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ✓ Token validation                                      │   │
│  │ ✓ Token expiration check (24 hours)                     │   │
│  │ ✓ User authentication                                   │   │
│  │ ✓ Role-based access control                             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                  │
│  Layer 3: Backend Validation                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ✓ Current password verification (BCrypt)                │   │
│  │ ✓ Password confirmation matching                        │   │
│  │ ✓ Minimum length validation                             │   │
│  │ ✓ Uniqueness validation (new ≠ current)                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                  │
│  Layer 4: Password Hashing                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ✓ BCrypt hashing (strength 10)                          │   │
│  │ ✓ Unique salt per password                              │   │
│  │ ✓ Never stored in plain text                            │   │
│  │ ✓ Irreversible encryption                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                  │
│  Layer 5: Audit & Notification                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ✓ Audit log recording                                   │   │
│  │ ✓ Email confirmation sent                               │   │
│  │ ✓ Timestamp recorded                                    │   │
│  │ ✓ User awareness of change                              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Mobile vs Desktop Layout

```
DESKTOP VIEW                          MOBILE VIEW
┌──────────────────────────┐         ┌──────────────┐
│ Settings                 │         │ Settings     │
│ ┌────────────────────┐   │         │ ┌──────────┐ │
│ │ Backend Config │   │   │         │ │ Backend  │ │
│ │ Security       │   │   │         │ │ Security │ │
│ └────────────────────┘   │         │ └──────────┘ │
│                          │         │              │
│ ┌────────────────────┐   │         │ ┌──────────┐ │
│ │ Change Password    │   │         │ │ Change   │ │
│ │                    │   │         │ │ Password │ │
│ │ Current Password   │   │         │ │          │ │
│ │ [••••••••] 👁️      │   │         │ │ Current  │ │
│ │                    │   │         │ │ [••••] 👁️│ │
│ │ New Password       │   │         │ │          │ │
│ │ [••••••••] 👁️      │   │         │ │ New      │ │
│ │                    │   │         │ │ [••••] 👁️│ │
│ │ Confirm Password   │   │         │ │          │ │
│ │ [••••••••] 👁️      │   │         │ │ Confirm  │ │
│ │                    │   │         │ │ [••••] 👁️│ │
│ │ [Change Password]  │   │         │ │          │ │
│ └────────────────────┘   │         │ │ [Change] │ │
│                          │         │ └──────────┘ │
└──────────────────────────┘         └──────────────┘

Desktop: Wide layout, side-by-side tabs
Mobile: Stacked layout, full-width fields
```

---

## Success Flow Summary

```
START
  ↓
LOGIN
  ↓
NAVIGATE TO SETTINGS
  ↓
CLICK SECURITY TAB
  ↓
FILL PASSWORD FORM
  ├─ Current Password: [••••••••]
  ├─ New Password: [••••••••]
  └─ Confirm Password: [••••••••]
  ↓
CLICK CHANGE PASSWORD
  ↓
FRONTEND VALIDATION ✓
  ├─ Not empty ✓
  ├─ Match ✓
  ├─ 8+ chars ✓
  └─ Different ✓
  ↓
SEND TO BACKEND
  ↓
BACKEND VALIDATION ✓
  ├─ Token valid ✓
  ├─ User authenticated ✓
  ├─ Current password correct ✓
  ├─ Passwords match ✓
  └─ Different ✓
  ↓
UPDATE PASSWORD
  ├─ Hash with BCrypt ✓
  ├─ Save to database ✓
  ├─ Send email ✓
  └─ Log to audit ✓
  ↓
SUCCESS MESSAGE ✅
  ├─ Form clears ✓
  ├─ Eye icons reset ✓
  └─ Email sent ✓
  ↓
END
```

---

This visual guide helps understand the password change feature at a glance!
