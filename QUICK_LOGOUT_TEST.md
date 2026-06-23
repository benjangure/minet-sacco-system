# Quick Logout Test

## Test Logout Works

1. **Login**: Go to /member/login, enter credentials, click login
2. **Verify on Dashboard**: Should see dashboard with member data
3. **Click Logout**: Find logout button (usually in sidebar/menu)
4. **Expected**: Should redirect to /member/login page
5. **Verify**: URL should be `/member/login` (NOT still on dashboard)

## Test After Restart

1. **Close entire browser** (not just tab)
2. **Restart browser**
3. **Open**: http://localhost:3000/member/login
4. **Expected**: Should be on login page (NOT redirected to dashboard)
5. **Why**: After restart, token is expired

## Test PC Shutdown

1. **Login** to dashboard
2. **Shutdown PC** (turn it off)
3. **Turn PC back on**
4. **Open browser**
5. **Go to**: http://localhost:3000/member/dashboard
6. **Expected**: Might be on dashboard (if session still valid) OR redirected to login
7. **Both are acceptable**: Either means logout will work

## If Logout Still Redirects to Dashboard

Check DevTools Console for:
- `DEBUG: Valid session exists in localStorage, redirecting to dashboard`
  → This means token is still valid (this is OK)
  
- `DEBUG: Session guard clears it and shows login`
  → This means token was invalid (this is OK)

If you see neither → Token validation isn't running → Clear browser cache and restart

