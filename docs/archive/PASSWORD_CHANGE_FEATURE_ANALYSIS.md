# Password Change Feature Analysis - SACCO System

## Executive Summary
The system has **partial support** for password changes. Password hashing is in place (BCrypt), and there's backend infrastructure, but there are **critical gaps** preventing users from self-service password changes. The feature needs both backend and frontend completion.

---

## Current State of Password Management

### 1. Database & Encryption
- **Location**: `users` table with `password` VARCHAR(255) column
- **Encryption Method**: BCrypt (strength 10)
- **Password Hashing**: Implemented in `UserService.createUser()` and `UserService.changePassword()`
- **Status**: ✅ IMPLEMENTED

### 2. Backend Password Change Infrastructure

#### Existing Service Method
```
UserService.changePassword(Long id, String newPassword, String reason)
```
- ✅ Encodes password with BCrypt
- ✅ Logs password change activity
- ✅ Updates `updated_at` timestamp
- **Location**: [UserService.java](backend/src/main/java/com/minet/sacco/service/UserService.java) (line 108-119)

#### Existing Controller Endpoint
```
POST /api/users/{id}/change-password
```
- ✅ Exists and is functional
- ❌ **PROBLEM 1**: Only accessible to ADMIN and TREASURER roles (not self-service)
- ❌ **PROBLEM 2**: Does NOT verify current/old password before allowing change
- ❌ **PROBLEM 3**: Requires user ID in URL path and new password in query params (insecure for user-initiated changes)
- **Location**: [UserController.java](backend/src/main/java/com/minet/sacco/controller/UserController.java) (line 311-330)

#### Permission Configuration
```
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER')")
```
- This endpoint is designed for **admin/treasurer to reset other users' passwords**, NOT for self-service
- Members and other staff cannot use this

### 3. Frontend Password Change UI

#### Staff Portal (Implemented but Non-Functional)
- **Location**: [Settings.tsx](minetsacco-main/src/pages/Settings.tsx)
- ✅ Has a "Change Password" form with tabs and proper UI
- ✅ Validates password requirements (min 8 chars, matching confirmation)
- ❌ **PROBLEM**: Calls `PUT /api/users/change-password` endpoint that **DOESN'T EXIST**
- ❌ **PROBLEM**: Tries to send `currentPassword` but no such endpoint exists
- Expected body:
```json
{
  "currentPassword": "old123456",
  "newPassword": "new123456"
}
```

#### Member Portal (Not Implemented)
- **Location**: [MemberSettings.tsx](minetsacco-main/src/pages/MemberSettings.tsx)
- ❌ Only has backend URL configuration
- ❌ Has NO password change functionality for members
- Members cannot change passwords through their portal

#### Password Reset Page (Placeholder only)
- **Location**: [ForgotPassword.tsx](minetsacco-main/src/pages/ForgotPassword.tsx)
- ❌ Shows "Feature Not Available" message
- ❌ Not implemented

### 4. User Types in System
```
Staff Users: ADMIN, TREASURER, LOAN_OFFICER, CREDIT_COMMITTEE, AUDITOR, TELLER, CUSTOMER_SUPPORT
Members: MEMBER role (linked to members table via memberId field)
```

---

## Problems Preventing Users from Changing Passwords

### ❌ Staff Users (Non-Admin)
1. **No dedicated endpoint**: The existing `/{id}/change-password` requires admin role
2. **Frontend UI is broken**: Settings.tsx calls non-existent `PUT /api/users/change-password` endpoint
3. **No current password verification**: Would need to verify old password before allowing change

### ❌ Members
1. **No password change endpoint at all**: MemberPortalController has no password-related methods
2. **Member login endpoint exists** but no corresponding password change endpoint
3. **No UI**: MemberSettings.tsx has no password change form

### ❌ Both Staff & Members
1. **No password reset flow**: ForgotPassword page not implemented
2. **Password-less recovery**: No "forgot password" email verification system
3. **No password requirements enforcement**: While UI checks 8-char minimum, backend has no validation

---

## Implementation Requirements

### What Needs to Be Built

#### **1. Backend: Create PasswordChangeRequest DTO** (New File)
```java
// File: dto/PasswordChangeRequestDTO.java
public class PasswordChangeRequestDTO {
    private String currentPassword;      // Required for self-service
    private String newPassword;          // New password
    private String confirmPassword;      // For validation (optional in DTO)
}
```

#### **2. Backend: Add Password Change Endpoint** (New in UserController)
```java
@PutMapping("/change-password")
@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_TREASURER','ROLE_LOAN_OFFICER',
              'ROLE_CREDIT_COMMITTEE','ROLE_AUDITOR','ROLE_TELLER','ROLE_CUSTOMER_SUPPORT')")
public ResponseEntity<ApiResponse<String>> changeOwnPassword(
    @Valid @RequestBody PasswordChangeRequestDTO request,
    Authentication authentication) {
    
    // Get current user
    String username = authentication.getName();
    User currentUser = userService.getUserByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Verify current password matches
    if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Current password is incorrect"));
    }
    
    // Validate new password
    if (request.getNewPassword().length() < 8) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Password must be at least 8 characters"));
    }
    
    // Change password
    userService.changePassword(currentUser.getId(), request.getNewPassword(), 
                              "Self-service password change by user");
    
    return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
}
```

#### **3. Backend: Add Member Password Change Endpoint** (New in MemberPortalController)
```java
@PutMapping("/change-password")
public ResponseEntity<ApiResponse<String>> changeMemberPassword(
    @Valid @RequestBody Map<String, String> body) {
    
    // Get current member
    Member member = getCurrentMember();
    User memberUser = userRepository.findByMemberId(member.getId())
        .orElseThrow(() -> new RuntimeException("Member user account not found"));
    
    String currentPassword = body.get("currentPassword");
    String newPassword = body.get("newPassword");
    
    // Verify current password
    if (!passwordEncoder.matches(currentPassword, memberUser.getPassword())) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Current password is incorrect"));
    }
    
    // Validate new password
    if (newPassword.length() < 8) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Password must be at least 8 characters"));
    }
    
    // Change password
    userService.changePassword(memberUser.getId(), newPassword, 
                              "Self-service password change by member");
    
    return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
}
```

#### **4. Frontend: Fix Settings.tsx Password Change**
```javascript
// Update the API endpoint from PUT to PUT /api/users/change-password
// Update request body to match what backend expects
const response = await fetch(`${API_BASE_URL}/users/change-password`, {
    method: "PUT",
    headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${session?.token}`,
    },
    body: JSON.stringify({
        currentPassword,
        newPassword,
    }),
});
```

#### **5. Frontend: Add Member Password Change UI**
Create a new section in [MemberSettings.tsx](minetsacco-main/src/pages/MemberSettings.tsx):
```javascript
// Add password change card similar to Staff Settings.tsx
const handleMemberPasswordChange = async (e: React.FormEvent) => {
    // Call /api/member/change-password endpoint
    // Similar validation as staff
}
```

#### **6. (Optional) Password Reset Email Flow**
Implement forgot password with:
- Generate temporary token
- Send email with reset link
- Validate token before allowing new password
- Frontend page to accept new password

---

## Summary Table

| Category | Current State | Gap | Priority |
|----------|---------------|-----|----------|
| **Staff Password Change** | UI exists, backend partial | Missing secure endpoint, broken frontend call | 🔴 HIGH |
| **Member Password Change** | Nothing implemented | Need UI + Backend endpoint | 🔴 HIGH |
| **Password Hashing** | ✅ BCrypt implemented | None | ✅ DONE |
| **Current Password Verification** | Not implemented | Must add for security | 🟠 MEDIUM |
| **Forgot Password Flow** | UI placeholder only | Entire feature missing | 🟠 MEDIUM |
| **Activity Logging** | ✅ Already logging password changes | None | ✅ DONE |

---

## Recommended Implementation Order

1. **Phase 1 (Critical)**: Create `/api/users/change-password` endpoint + DTO
2. **Phase 2 (Critical)**: Create `/api/member/change-password` endpoint in MemberPortalController
3. **Phase 3 (Critical)**: Fix Settings.tsx to call correct endpoint
4. **Phase 4 (Important)**: Add password change UI to MemberSettings.tsx
5. **Phase 5 (Future)**: Implement forgot password email flow

---

## Security Considerations

✅ **Good**: BCrypt password hashing with strength 10
✅ **Good**: Activity logging for all password changes
⚠️ **Warning**: New endpoints need current password verification (prevents account takeovers)
⚠️ **Warning**: Password should be sent only over HTTPS
⚠️ **Warning**: Consider rate limiting on password change attempts

---

## Conclusion

The foundation is there (BCrypt, UserService methods, partial UI), but **the self-service password change feature is currently broken and non-functional for both staff and members**. The implementation is straightforward and can be completed with:

- **Backend**: 2 new endpoints (~50 lines each) + 1 new DTO
- **Frontend**: Fix existing Settings component + add Member password section
- **Testing**: Test with both staff and member accounts

Would you like me to now implement these changes? I can start with the backend endpoints and DTOs.
