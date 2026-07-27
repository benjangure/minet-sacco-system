# Quick Member Login Test

## Setup
- Backend running: `http://localhost:8080` ✅
- Frontend running: `http://localhost:3000` ✅  
- Browser console open (F12)

## Test: Login After Password Setup

### Step 1: Go to Member Login
```
URL: http://localhost:3000/member/login
```

### Step 2: First Login (Get New Password)
```
Phone/ID: 12141
Password: (initial password from DB)
Click: Login
```
**Expected**: Should redirect to password setup page

### Step 3: Set New Password
```
New Password: Test123!@#
Confirm: Test123!@#
Click: Setup Password
```
**Expected**: Should redirect back to login with success message

### Step 4: Login With New Password (THE FIX TEST) 
```
Phone/ID: 12141
Password: Test123!@#
Click: Login
```

### Step 5: Check Console
Look for these messages in order:
```
✅ "DEBUG: Login successful, redirecting to member dashboard"
✅ "DEBUG: Session in localStorage before navigate: YES"
✅ "DEBUG: Session has token: YES"
✅ "Navigate timeout fired, calling navigate now"
```

### Step 6: Check URL
```
Current URL should be: http://localhost:3000/member/dashboard
NOT: http://localhost:3000/member/login
```

### Step 7: Check Dashboard Loads
```
Should see:
- Eligibility amount
- Active loans list
- Account balances
- Member data
```

### Step 8: Check Network Tab
```
Open: DevTools → Network tab
Should see ALL these requests return status 200:
✅ /api/member/eligibility
✅ /api/member/loans  
✅ /api/member/notifications
❌ NO 401 Unauthorized errors
```

---

## Quick Debug If Issues

### Issue 1: Still on Login Page
**In console, run:**
```javascript
localStorage.getItem('session')
// Should show: {"token":"eyJhbGc...","user":{...}}
// NOT: null or undefined
```

### Issue 2: API Returns 401
**In Network tab:**
1. Click on `/api/member/loans` request
2. Go to Headers tab
3. Look for: `Authorization: Bearer eyJhbGc...`
4. Should be there with "Bearer " prefix

### Issue 3: 500+ Error
**In Backend console:**
- Check for SQL errors
- Check if member 12141 exists in DB
- Verify token generation logic

---

## Success Indicator
✅ You see the dashboard with member data = **FIXED**

