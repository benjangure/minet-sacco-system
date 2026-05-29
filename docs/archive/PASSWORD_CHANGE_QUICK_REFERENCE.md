# Password Change Feature - Quick Reference Guide

## 🎯 Feature Overview

Both staff and member users can securely change their passwords through dedicated settings pages with comprehensive validation and security checks.

## 📍 Access Points

### Staff Portal
- **Route**: `/settings`
- **Navigation**: AppSidebar → Administration → Settings
- **Tab**: Security → Change Password

### Member Portal
- **Route**: `/member/settings`
- **Navigation**: 
  - MemberSidebar → Settings
  - Mobile Top Navbar → Settings Tab

## 🔐 Security Features

| Feature | Implementation |
|---------|-----------------|
| **Authentication** | JWT Token (Bearer) |
| **Password Hashing** | BCrypt (Strength 10) |
| **Current Password Verification** | Required |
| **Password Confirmation** | Must match new password |
| **Minimum Length** | 8 characters |
| **Unique Password** | Must differ from current |
| **Email Confirmation** | Sent to user email |
| **Activity Logging** | Logged for audit trail |
| **Input Validation** | Client + Server-side |

## 📋 Form Fields

All three fields have eye icon toggles for visibility:

1. **Current Password** - Your existing password
2. **New Password** - Your desired new password (min 8 chars)
3. **Confirm New Password** - Repeat new password for verification

## ✅ Validation Rules

### Client-Side (Frontend)
```
✓ Current password is not empty
✓ New password is not empty
✓ New password ≥ 8 characters
✓ New password = Confirm password
✓ New password ≠ Current password
```

### Server-Side (Backend)
```
✓ Current password matches stored password
✓ New password ≠ Current password
✓ New password = Confirm password
✓ Password hashed with BCrypt
✓ User record updated
✓ Confirmation email sent
✓ Activity logged
```

## 🔗 API Endpoints

### Staff Endpoint
```
PUT /api/users/change-password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "string",
  "newPassword": "string",
  "confirmPassword": "string"
}
```

### Member Endpoint
```
PUT /api/member/change-password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "string",
  "newPassword": "string",
  "confirmPassword": "string"
}
```

## 📊 Response Format

### Success (200 OK)
```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null
}
```

### Error (400 Bad Request)
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

## ⚠️ Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| "Please enter your current password" | Empty field | Enter current password |
| "Please enter a new password" | Empty field | Enter new password |
| "New passwords do not match" | Mismatch | Ensure both match exactly |
| "Password must be at least 8 characters" | Too short | Use ≥ 8 characters |
| "New password must be different from current password" | Same as current | Choose different password |
| "Current password is incorrect" | Wrong password | Verify current password |
| 401 Unauthorized | Token expired | Log out and log back in |
| 400 Bad Request | Server error | Check connection and retry |

## 🧪 Testing Checklist

- [ ] Navigate to Settings page
- [ ] Click Security tab
- [ ] Enter current password
- [ ] Enter new password (8+ chars)
- [ ] Confirm new password
- [ ] Click "Change Password"
- [ ] Verify success message
- [ ] Check form cleared
- [ ] Log out
- [ ] Log back in with new password
- [ ] Verify login successful
- [ ] Check confirmation email received

## 📁 File Locations

### Frontend
- **Staff**: `minetsacco-main/src/pages/Settings.tsx`
- **Member**: `minetsacco-main/src/pages/MemberSettings.tsx`
- **Navigation**: 
  - `minetsacco-main/src/components/AppSidebar.tsx`
  - `minetsacco-main/src/components/MemberSidebar.tsx`
  - `minetsacco-main/src/components/MemberLayout.tsx`

### Backend
- **Staff Endpoint**: `backend/src/main/java/com/minet/sacco/controller/UserController.java`
- **Member Endpoint**: `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java`
- **DTO**: `backend/src/main/java/com/minet/sacco/dto/PasswordChangeRequestDTO.java`
- **Service**: `backend/src/main/java/com/minet/sacco/service/UserService.java`

## 🔄 Implementation Pattern

```
User Input
    ↓
Client-Side Validation
    ↓
API Request (PUT)
    ↓
Server-Side Validation
    ↓
Password Hashing (BCrypt)
    ↓
Database Update
    ↓
Email Confirmation
    ↓
Activity Logging
    ↓
Success Response
    ↓
Form Reset & Toast Notification
```

## 🎨 UI Components Used

- **Card**: Container for form sections
- **Input**: Password input fields
- **Button**: Submit button
- **Label**: Field labels
- **Tabs**: Tab navigation (Profile/Security)
- **Icons**: Eye/EyeOff for visibility toggle
- **Toast**: Success/error notifications
- **Alert**: Information messages

## 🔑 Key Implementation Details

### Eye Icon Toggle
```typescript
// Shows/hides password based on state
type={showPassword ? "text" : "password"}

// Toggle button
<button onClick={() => setShowPassword(!showPassword)}>
  {showPassword ? <EyeOff /> : <Eye />}
</button>
```

### Form Submission
```typescript
const handleChangePassword = async (e) => {
  e.preventDefault();
  
  // Validate
  // Send request
  // Handle response
  // Show notification
  // Reset form
}
```

### Error Handling
```typescript
if (response.ok) {
  // Success
  toast({ title: "Success", description: "..." });
} else {
  // Error
  const error = await response.json();
  toast({ title: "Error", description: error.message });
}
```

## 📱 Responsive Design

- **Desktop**: Full-width form with tabs
- **Tablet**: Responsive layout with proper spacing
- **Mobile**: Stacked form with mobile navbar tab

## 🔒 Security Best Practices

1. ✅ Always verify current password
2. ✅ Use strong hashing (BCrypt)
3. ✅ Enforce minimum length (8 chars)
4. ✅ Require confirmation
5. ✅ Prevent password reuse
6. ✅ Send confirmation email
7. ✅ Log all changes
8. ✅ Use HTTPS in production
9. ✅ Validate on both sides
10. ✅ Handle errors gracefully

## 🚀 Performance Considerations

- **Client-Side Validation**: Instant feedback, reduces server load
- **Async Request**: Non-blocking password change
- **Loading State**: Prevents double submission
- **Error Caching**: Prevents repeated failed requests
- **Form Reset**: Clears sensitive data from memory

## 📞 Support & Troubleshooting

### Issue: Can't access Settings
- **Check**: Are you logged in?
- **Check**: Is your role authorized?
- **Check**: Is the route configured?

### Issue: Password change fails
- **Check**: Is backend running?
- **Check**: Is token valid/not expired?
- **Check**: Is network connection stable?

### Issue: Email not received
- **Check**: Is email service configured?
- **Check**: Is email address correct?
- **Check**: Check spam folder

### Issue: Can't log in with new password
- **Check**: Did you wait for confirmation?
- **Check**: Is caps lock off?
- **Check**: Did you copy password correctly?

## 📚 Related Documentation

- `MEMBER_PASSWORD_CHANGE_IMPLEMENTATION.md` - Detailed member implementation
- `STAFF_VS_MEMBER_PASSWORD_CHANGE_COMPARISON.md` - Side-by-side comparison
- `JWT_TOKEN_EXPIRATION_FIX.md` - Token expiration troubleshooting
- `MEMBER_PASSWORD_CHANGE_400_ERROR_FIX.md` - Member linking issues

## ✨ Feature Status

| Component | Status | Notes |
|-----------|--------|-------|
| Staff Password Change | ✅ Complete | Fully functional |
| Member Password Change | ✅ Complete | Enhanced validation |
| Eye Icon Toggle | ✅ Complete | All 3 fields |
| Email Confirmation | ✅ Complete | Sent on success |
| Activity Logging | ✅ Complete | Audit trail |
| Error Handling | ✅ Complete | Comprehensive |
| Navigation | ✅ Complete | Sidebar + navbar |
| Responsive Design | ✅ Complete | Mobile friendly |

## 🎓 Learning Resources

### For Developers
- Review `UserController.java` for staff implementation
- Review `MemberPortalController.java` for member implementation
- Check `PasswordChangeRequestDTO.java` for request structure
- Study `UserService.java` for business logic

### For Users
- See "Testing Checklist" above
- Follow "Access Points" to find Settings
- Refer to "Common Errors & Solutions" for help

---

**Last Updated**: May 14, 2026
**Version**: 1.0
**Status**: Production Ready ✅
