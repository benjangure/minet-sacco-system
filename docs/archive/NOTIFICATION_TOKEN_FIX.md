# Notification Service Token Detection Fix

## Problem
Members were getting "No token found in localStorage" errors when trying to load notifications. The notification service was trying to fetch from `/api/notifications` (staff endpoint) instead of `/api/member/notifications` (member endpoint).

## Root Cause
The `getNotificationsPath()` function in `notificationService.ts` was only checking for the role in the `session` object (used by staff users). Members use a different authentication mechanism:
- **Staff users**: Token stored in `localStorage.session.token` with role in `localStorage.session.user.role`
- **Member users**: Token stored in `localStorage.token` (no session object)

The function wasn't detecting member users, so it defaulted to the staff endpoint which requires a different token format.

## Solution
Updated `getNotificationsPath()` to check both authentication methods:

**Before**:
```typescript
const getNotificationsPath = (): string => {
  let userRole = null;
  
  try {
    const session = localStorage.getItem('session');
    if (session) {
      const parsedSession = JSON.parse(session);
      userRole = parsedSession.user?.role;
    }
  } catch (e) {
    console.warn('Failed to parse session from localStorage');
  }
  
  if (userRole && userRole === 'MEMBER') {
    return '/member/notifications';
  }
  return '/notifications';
};
```

**After**:
```typescript
const getNotificationsPath = (): string => {
  let userRole = null;
  
  // First try to get role from session (staff users)
  try {
    const session = localStorage.getItem('session');
    if (session) {
      const parsedSession = JSON.parse(session);
      userRole = parsedSession.user?.role;
    }
  } catch (e) {
    console.warn('Failed to parse session from localStorage');
  }
  
  // If no role found in session, check if this is a member (member token exists)
  if (!userRole) {
    const memberToken = localStorage.getItem('token');
    if (memberToken) {
      // This is a member user
      userRole = 'MEMBER';
    }
  }
  
  // Staff roles use /api/notifications, members use /api/member/notifications
  if (userRole && userRole === 'MEMBER') {
    return '/member/notifications';
  }
  return '/notifications';
};
```

## Changes Made
**File**: `minetsacco-main/src/services/notificationService.ts`

- Added fallback check for member token in localStorage
- If no session role is found, checks for member token
- If member token exists, sets role to 'MEMBER'
- Routes to correct endpoint based on detected role

## How It Works Now

1. **Staff User** (e.g., Loan Officer):
   - Has `localStorage.session` with role
   - `getNotificationsPath()` detects role from session
   - Routes to `/api/notifications`

2. **Member User**:
   - Has `localStorage.token` (no session object)
   - `getNotificationsPath()` doesn't find session role
   - Falls back to checking for member token
   - Detects member token exists → sets role to 'MEMBER'
   - Routes to `/api/member/notifications`

## Testing
- [ ] Member logs in and views notifications
- [ ] No "No token found" errors in console
- [ ] Notifications load correctly from `/api/member/notifications`
- [ ] Staff users still work with `/api/notifications`
- [ ] Unread count displays correctly
- [ ] Notification bell shows correct count

## Files Modified
- `minetsacco-main/src/services/notificationService.ts` - Updated `getNotificationsPath()` function

## Impact
- Fixes member notification loading
- No breaking changes to staff notification system
- Backward compatible with existing authentication
