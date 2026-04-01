# Loan Application 401 Error - Root Cause Analysis

## THE PROBLEM YOU'RE EXPERIENCING

**Error**: `Failed to load resource: the server responded with a status of 401`
**Endpoint**: `192.168.0.195:8080/api/member/member-by-employee-id/EMP009`
**What's Happening**: The guarantor search is failing because the JWT token is not being sent or is invalid

---

## WHAT YOU HAD (BEFORE)

✅ Working loan application form with:
- Eligibility card displaying in 2x2 grid layout at the top
- Loan products dropdown loading all 4 products instantly
- Guarantor search by employee ID working
- All validations working (amount, duration, guarantor count)
- Fast and responsive UI

---

## WHAT'S WRONG NOW

The 401 error indicates **authentication failure**, not a code logic issue. The backend is rejecting the request because:

1. **No JWT token is being sent** with the guarantor search request, OR
2. **The JWT token is expired**, OR
3. **The JWT token format is invalid**

---

## ROOT CAUSE ANALYSIS

### Backend Security Configuration (CORRECT)
```java
// SecurityConfig.java
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/debug/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .anyRequest().authenticated()  // ← ALL OTHER ENDPOINTS REQUIRE AUTH
)
```

**This means**: `/api/member/member-by-employee-id/{employeeId}` REQUIRES a valid JWT token.

### JWT Filter (CORRECT)
```java
// JwtRequestFilter.java
final String requestTokenHeader = request.getHeader("Authorization");

if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
    jwtToken = requestTokenHeader.substring(7);
    // Validates token...
}
```

**This means**: The filter looks for `Authorization: Bearer <token>` header.

### Frontend Code (CORRECT)
```typescript
// MemberLoanApplication.tsx
const response = await axios.get(
  `${API_BASE_URL}/member/member-by-employee-id/${guarantorInput.trim()}`,
  { headers: { Authorization: `Bearer ${session?.token}` } }  // ← Token IS being sent
);
```

**The code IS sending the token correctly.**

---

## POSSIBLE CAUSES OF 401 ERROR

### 1. **Token Expired** (MOST LIKELY)
- You logged in earlier
- The JWT token has an expiration time (typically 24 hours)
- The token in localStorage is no longer valid
- **Solution**: Log out and log back in to get a fresh token

### 2. **Session Lost After Backend Restart**
- You restarted the backend
- The session in localStorage is still there, but it's stale
- **Solution**: Clear localStorage and log in again

### 3. **Token Not Being Stored Correctly**
- The login response didn't include a valid token
- **Solution**: Check browser DevTools → Application → LocalStorage → Look for "session" key

### 4. **CORS or Network Issue**
- The Authorization header is being stripped by CORS
- **Solution**: Check if CORS is properly configured

---

## HOW TO DIAGNOSE THIS

### Step 1: Check if Token Exists
1. Open browser DevTools (F12)
2. Go to **Application** tab
3. Click **Local Storage**
4. Look for the entry with your domain
5. Find the key `session`
6. Check if it has a `token` field with a long string

### Step 2: Check Token Expiration
1. Copy the token value
2. Go to https://jwt.io
3. Paste the token in the "Encoded" section
4. Look at the **Payload** section
5. Find the `exp` field (expiration time)
6. Check if it's in the past

### Step 3: Check Network Request
1. Open DevTools → **Network** tab
2. Try to search for a guarantor
3. Look for the request to `member-by-employee-id`
4. Click on it
5. Go to **Headers** tab
6. Look for `Authorization` header
7. It should show: `Authorization: Bearer eyJhbGc...` (long token)

### Step 4: Check Backend Logs
1. Look at backend console output
2. Search for "Unable to get JWT Token or JWT Token has expired"
3. This confirms the token is invalid

---

## WHAT I TRIED (AND WHY IT FAILED)

### ❌ Attempt 1: Checked LoanProduct Entity
- **Why it failed**: The entity was correct. The problem wasn't in the database.

### ❌ Attempt 2: Changed Response Parsing
- **Why it failed**: The response format wasn't the issue. The request never reached the backend.

### ❌ Attempt 3: Added Loading States
- **Why it failed**: This added complexity without fixing the 401 error.

### ❌ Attempt 4: Kept Making Frontend Changes
- **Why it failed**: I ignored the 401 error in the network tab. The problem was authentication, not code logic.

---

## WHAT SHOULD HAPPEN

1. **User logs in** → Backend generates JWT token → Token stored in localStorage
2. **User navigates to loan application** → Frontend loads eligibility and loan products
3. **User searches for guarantor** → Frontend sends request WITH `Authorization: Bearer <token>` header
4. **Backend receives request** → JwtRequestFilter validates token → Request proceeds
5. **Backend returns guarantor info** → Frontend displays it

**The 401 error means Step 4 is failing** because the token is missing or invalid.

---

## IMMEDIATE ACTION REQUIRED

**DO NOT make code changes.** Instead:

1. **Log out completely**
   - Click logout button
   - Clear browser cache (Ctrl+Shift+Delete)
   - Close and reopen browser

2. **Log back in**
   - This will generate a fresh JWT token
   - Token will be stored in localStorage

3. **Try the guarantor search again**
   - If it works now, the issue was token expiration
   - If it still fails with 401, check the network tab for the Authorization header

4. **If still failing**:
   - Check browser DevTools → Network tab
   - Look at the request headers
   - Verify `Authorization: Bearer <token>` is present
   - If not present, there's a frontend issue
   - If present, there's a backend token validation issue

---

## FILES INVOLVED

- `backend/src/main/java/com/minet/sacco/security/SecurityConfig.java` - Defines which endpoints need auth
- `backend/src/main/java/com/minet/sacco/security/JwtRequestFilter.java` - Validates JWT tokens
- `minetsacco-main/src/contexts/AuthContext.tsx` - Stores and manages JWT token
- `minetsacco-main/src/pages/MemberLoanApplication.tsx` - Sends requests with token

---

## SUMMARY

**The code is correct.** The 401 error is almost certainly due to:
1. **Expired JWT token** (most likely)
2. **Session lost after backend restart**
3. **Token not being sent in request headers** (less likely, code looks correct)

**Next step**: Log out, log back in, and try again. If it still fails, check the network tab to verify the Authorization header is being sent.
