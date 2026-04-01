# Loan Application 401 Error - Troubleshooting Steps

## QUICK FIX (Try This First)

### Step 1: Complete Logout
1. Click the **Logout** button in the application
2. Open browser DevTools (F12)
3. Go to **Application** → **Local Storage**
4. Find your domain
5. Right-click and select **Clear All**
6. Close the browser completely
7. Reopen the browser

### Step 2: Fresh Login
1. Navigate to the login page
2. Enter your credentials
3. Click Login
4. Wait for the page to load completely

### Step 3: Test Guarantor Search
1. Navigate to **Loan Application**
2. Try searching for a guarantor by employee ID (e.g., EMP009)
3. Check if it works now

**If it works**: The issue was an expired token. You're done.

**If it still shows 401**: Continue to the diagnostic steps below.

---

## DIAGNOSTIC STEPS

### Diagnostic 1: Verify Token is Stored

1. Open DevTools (F12)
2. Go to **Application** tab
3. Click **Local Storage** on the left
4. Click on your domain (e.g., `http://localhost:5173`)
5. Look for a key called `session`
6. Click on it to see its value

**Expected**: You should see a JSON object with:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 0,
    "username": "your_username",
    "email": "your_email",
    "role": "CUSTOMER_SUPPORT"
  }
}
```

**If missing**: 
- The login didn't work properly
- Try logging in again
- Check if there are any error messages during login

**If present**: Continue to Diagnostic 2

---

### Diagnostic 2: Check Token Expiration

1. Copy the entire `token` value (the long string starting with `eyJ...`)
2. Go to https://jwt.io in a new tab
3. Paste the token in the **Encoded** section (left side)
4. Look at the **Payload** section (middle)
5. Find the `exp` field

**Expected**: The `exp` value should be a large number representing a future timestamp

**To check if it's expired**:
1. Open browser console (F12 → Console tab)
2. Paste this code:
```javascript
const token = JSON.parse(localStorage.getItem('session')).token;
const parts = token.split('.');
const payload = JSON.parse(atob(parts[1]));
const expirationTime = payload.exp * 1000;
const currentTime = Date.now();
console.log('Token expires at:', new Date(expirationTime));
console.log('Current time:', new Date(currentTime));
console.log('Is expired?', currentTime > expirationTime);
```

**If expired**: Log out and log back in to get a fresh token

**If not expired**: Continue to Diagnostic 3

---

### Diagnostic 3: Check Network Request Headers

1. Open DevTools (F12)
2. Go to **Network** tab
3. Make sure **Network** recording is ON (red dot should be visible)
4. In the application, try to search for a guarantor
5. Look for a request to `member-by-employee-id` in the Network tab
6. Click on that request
7. Go to the **Headers** tab
8. Scroll down to find **Request Headers**
9. Look for the `Authorization` header

**Expected**: You should see:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**If missing**:
- The frontend is not sending the token
- This is a code issue
- Contact support with this information

**If present**: Continue to Diagnostic 4

---

### Diagnostic 4: Check Backend Response

1. In the same Network request (from Diagnostic 3)
2. Go to the **Response** tab
3. Look at what the backend returned

**If you see**:
```json
{
  "timestamp": "2026-03-31T...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unable to get JWT Token or JWT Token has expired"
}
```

**This means**:
- The token is being sent (good)
- But the backend is rejecting it (bad)
- The token is either expired or invalid

**Solution**: Log out and log back in

---

## WHAT TO DO IF NOTHING WORKS

If you've tried all the steps above and it still doesn't work:

1. **Collect this information**:
   - Screenshot of the 401 error in Network tab
   - Screenshot of the Authorization header (from Diagnostic 3)
   - Screenshot of the token payload (from jwt.io)
   - Backend console output (look for JWT-related errors)

2. **Check backend logs**:
   - Look at the backend console
   - Search for "JWT" or "401" or "Unauthorized"
   - Copy any error messages

3. **Verify backend is running**:
   - Open a new tab
   - Go to `http://192.168.0.195:8080/api/auth/login`
   - You should see an error (not a 404)
   - If you see 404, the backend is not running

4. **Try a different endpoint**:
   - In DevTools Console, run:
   ```javascript
   const token = JSON.parse(localStorage.getItem('session')).token;
   fetch('http://192.168.0.195:8080/api/member/profile', {
     headers: { 'Authorization': `Bearer ${token}` }
   }).then(r => r.json()).then(console.log);
   ```
   - If this works, the token is valid
   - If this fails with 401, the token is invalid

---

## COMMON ISSUES AND SOLUTIONS

### Issue: "Token is expired"
**Solution**: Log out and log back in

### Issue: "Authorization header is missing"
**Solution**: This is a frontend code issue. The token is not being sent with the request.

### Issue: "Backend is not running"
**Solution**: Start the backend server

### Issue: "CORS error"
**Solution**: Check if CORS is properly configured in the backend

### Issue: "Token format is invalid"
**Solution**: The token in localStorage is corrupted. Clear localStorage and log in again.

---

## VERIFICATION CHECKLIST

After trying the quick fix, verify:

- [ ] I logged out completely
- [ ] I cleared browser cache and localStorage
- [ ] I closed and reopened the browser
- [ ] I logged back in
- [ ] I can see the loan application page
- [ ] I can see the eligibility card at the top
- [ ] I can see the loan products dropdown
- [ ] I can search for a guarantor without 401 error
- [ ] The guarantor search returns results

If all checkboxes are checked, the issue is resolved.

---

## WHAT NOT TO DO

❌ Do NOT make code changes
❌ Do NOT modify the backend
❌ Do NOT clear the database
❌ Do NOT restart the backend multiple times
❌ Do NOT change the JWT configuration

Just log out and log back in. That's usually all that's needed.
