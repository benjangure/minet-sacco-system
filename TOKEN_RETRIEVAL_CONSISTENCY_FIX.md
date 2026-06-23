# Token Retrieval Consistency Fix - Root Cause Analysis

## The Root Problem

When AuthContext introduced the first-login feature and started saving sessions as `{ token, user }` objects in localStorage under the key `session`, several components in the codebase were still looking for the token in the old location directly under the key `token`.

This created an **inconsistency** where:
- ✅ AuthContext saved token at: `localStorage['session'].token`
- ❌ Other components looked for token at: `localStorage['token']`

Result: **Components couldn't find the token and all API calls failed with 401 Unauthorized**

---

## All Places Token Retrieval Was Fixed

### 1. Axios Request Interceptor (api.ts)
**File**: `minetsacco-main/src/config/api.ts` Lines 50-67
**Function**: Adds Authorization header to all API requests
**Before**: Looked only at `localStorage.getItem('token')`
**After**: Checks session object first, then falls back to direct token key

**Impact**: **CRITICAL** - Without this, no API calls include the token

```typescript
// Before (BROKEN)
if (isMemberRoute) {
  token = localStorage.getItem('token');  // ❌ Wrong location
}

// After (FIXED)
const sessionStr = localStorage.getItem('session');
if (sessionStr) {
  const session = JSON.parse(sessionStr);
  if (session.token) token = session.token;  // ✅ Correct location
}
if (!token) token = localStorage.getItem('token');  // Fallback
```

---

### 2. ProtectedRoute Component (ProtectedRoute.tsx)
**File**: `minetsacco-main/src/components/ProtectedRoute.tsx`
**Function**: Guards member routes by checking if JWT is valid
**Before**: Synchronous check of localStorage (race condition)
**After**: Async check via useEffect + state

**Impact**: Prevented page render while checking token validity

```typescript
// Before (BROKEN - Race Condition)
let token = localStorage.getItem('token');
if (!token) {
  const sessionStr = localStorage.getItem('session');
  const session = JSON.parse(sessionStr);
  token = session.token;  // ❌ Might not be written yet
}
if (!token) return <Navigate to="/member/login" />;

// After (FIXED - Async with State)
const [memberTokenValid, setMemberTokenValid] = useState<boolean | null>(null);
useEffect(() => {
  // Check token...
  setMemberTokenValid(isValid);
}, [requiredRole]);
if (memberTokenValid === null) return <LoadingSpinner />;
```

---

### 3. MemberDashboard - fetchDashboard() (MemberDashboard.tsx)
**File**: `minetsacco-main/src/pages/MemberDashboard.tsx` Lines 268-297
**Function**: Fetches main dashboard data on page load
**Before**: Looked only at `localStorage.getItem('token')`
**After**: Checks session object first

**Impact**: Dashboard couldn't load because API call had no token

```typescript
// Before (BROKEN)
const token = localStorage.getItem('token');  // ❌ Wrong location
if (!token) { navigate('/member'); return; }

// After (FIXED)
let token = localStorage.getItem('token');
if (!token) {
  const sessionStr = localStorage.getItem('session');
  const session = JSON.parse(sessionStr);
  token = session.token;  // ✅ Correct location
}
```

---

### 4. MemberDashboard - handleViewReceipt() (MemberDashboard.tsx)
**File**: `minetsacco-main/src/pages/MemberDashboard.tsx` Lines 115-140
**Function**: Downloads deposit request receipts
**Before**: Looked only at `localStorage.getItem('token')`
**After**: Checks session object first

**Impact**: Receipt downloads failed because fetch had no Authorization header

```typescript
// Before (BROKEN)
const token = localStorage.getItem('token');  // ❌ Wrong location
const response = await fetch(`${API_BASE_URL}/member/deposit-requests/${depositId}/receipt/download`, {
  headers: { Authorization: `Bearer ${token}` }  // ❌ No token
});

// After (FIXED)
let token = localStorage.getItem('token');
if (!token) {
  const sessionStr = localStorage.getItem('session');
  const session = JSON.parse(sessionStr);
  token = session.token;  // ✅ Token found
}
const response = await fetch(`${API_BASE_URL}/member/deposit-requests/${depositId}/receipt/download`, {
  headers: { Authorization: `Bearer ${token}` }  // ✅ Token included
});
```

---

### 5. MemberDashboard - fetchUnreadNotifications() (MemberDashboard.tsx)
**File**: `minetsacco-main/src/pages/MemberDashboard.tsx` Lines 436-455
**Function**: Fetches unread notification count
**Before**: Looked only at `localStorage.getItem('token')`
**After**: Checks session object first

**Impact**: Notification count wasn't loading

```typescript
// Before (BROKEN)
const token = localStorage.getItem('token');  // ❌ Wrong location
if (!token) return;
const response = await api.get('/member/notifications/unread-count');

// After (FIXED)
let token = localStorage.getItem('token');
if (!token) {
  const sessionStr = localStorage.getItem('session');
  const session = JSON.parse(sessionStr);
  token = session.token;  // ✅ Token found
}
if (!token) return;
const response = await api.get('/member/notifications/unread-count');
```

---

### 6. MemberLogin - Session Guard (MemberLogin.tsx)
**File**: `minetsacco-main/src/pages/MemberLogin.tsx` Lines 34-51
**Function**: Redirects already-logged-in users to dashboard
**Before**: No check for existing session
**After**: Checks session object and redirects

**Impact**: Prevented redirect loops when user reloaded page

```typescript
// Before (BROKEN)
useEffect(() => {
  // No session check - could show login page even if logged in
  setSuccessMessage(location.state?.message);
}, [location.state]);

// After (FIXED)
useEffect(() => {
  const sessionStr = localStorage.getItem('session');
  if (sessionStr) {
    const session = JSON.parse(sessionStr);
    if (session.token) {
      navigate('/member/dashboard', { replace: true });  // Redirect if logged in
      return;
    }
  }
  setSuccessMessage(location.state?.message);
}, [location.state, navigate]);
```

---

## Pattern Used for Consistent Token Retrieval

All fixed locations now use this pattern:

```typescript
// Step 1: Try direct token key (fallback for old code)
let token = localStorage.getItem('token');

// Step 2: Try session object (where AuthContext stores it)
if (!token) {
  const sessionStr = localStorage.getItem('session');
  if (sessionStr) {
    try {
      const session = JSON.parse(sessionStr);
      if (session.token && typeof session.token === 'string') {
        token = session.token;  // ✅ Found!
      }
    } catch (e) {
      console.error('Failed to parse session:', e);
    }
  }
}

// Step 3: Use token or handle missing token
if (!token) {
  console.log('No token found');
  // Handle unauthenticated state
} else {
  // Use token for API calls
  config.headers.Authorization = `Bearer ${token}`;
}
```

---

## Why This Happened

1. **Original Design**: Token stored directly at `localStorage['token']`
2. **New Feature**: First-login flow introduced, changed to `localStorage['session']` = `{ token, user }`
3. **Incomplete Migration**: Some components updated, others weren't
4. **Result**: Inconsistent token storage → 401 errors everywhere

---

## Prevention for Future Changes

To prevent this in the future:

1. **Create a helper function** for token retrieval:
   ```typescript
   function getAuthToken(): string | null {
     let token = localStorage.getItem('token');
     if (!token) {
       const sessionStr = localStorage.getItem('session');
       if (sessionStr) {
         const session = JSON.parse(sessionStr);
         token = session.token;
       }
     }
     return token;
   }
   ```

2. **Export from api.ts**:
   ```typescript
   export { getAuthToken };
   ```

3. **Use everywhere**:
   ```typescript
   const token = getAuthToken();
   ```

This ensures all components use the same logic and any future storage format changes only need to be made in one place.

---

## Summary

| Component | Issue | Fix | Impact |
|-----------|-------|-----|--------|
| api.ts | Interceptor looking for wrong token | Check session object | ⭐ CRITICAL - All APIs |
| ProtectedRoute.tsx | Sync check race condition | Async with useEffect | High - Route protection |
| MemberDashboard.tsx (3 functions) | Wrong token location | Check session object | High - Dashboard loading |
| MemberLogin.tsx | No session guard | Added session check on mount | Medium - Redirect loop |

**Total Fixes**: 6 separate locations fixed
**Root Cause**: Token storage format changed but not all references updated
**Solution**: Unified token retrieval pattern across all components

