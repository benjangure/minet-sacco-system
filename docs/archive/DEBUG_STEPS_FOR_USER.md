# Step-by-Step Debug Guide for Repayment Display Bug

## CRITICAL: You Must Be Logged In First

The console.log debugging won't work unless you're properly logged in and the token is stored.

---

## Step 1: Verify You're Logged In

### 1.1 Check localStorage for Token

1. Open your browser
2. Go to http://localhost:3000/member
3. Press **F12** to open Developer Tools
4. Click on the **Console** tab
5. Paste this command:

```javascript
console.log('Token:', localStorage.getItem('token'));
console.log('Session:', localStorage.getItem('session'));
```

### Expected Output (If Logged In):
```
Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Session: {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...","user":{"id":0,"username":"member1","email":"member1@example.com","role":"MEMBER"}}
```

### If You See `null`:
**You are NOT logged in.** Follow these steps:

1. Go to http://localhost:3000/member
2. You should see a login form
3. Enter your member credentials:
   - Username: `member1` (or your member username)
   - Password: `password123` (or your password)
4. Click "Login"
5. Wait for the page to redirect to the Member Dashboard
6. You should see your dashboard with balance information
7. Then repeat Step 1.1 to verify the token is now stored

---

## Step 2: Navigate to Loans Page

Once you're logged in:

1. On the Member Dashboard, click the **"Loans"** tab
2. Wait for the loans to load (you should see a list of your loans)
3. Keep the Developer Tools open (F12)

---

## Step 3: Check the Console Output

The console should automatically show debug messages like:

```
[DEBUG] Loan 13 (LN-2026-00004) - Raw API Data: {
  id: 13,
  loanNumber: "LN-2026-00004",
  status: "DISBURSED",
  amount: 200000,
  totalInterest: 80000,
  totalRepayable: 280000,
  outstandingBalance: 280000,
  calculatedRepaid: 0,
  calculatedPercentage: 0
}
```

### What to Look For:

- **outstandingBalance**: Should equal totalRepayable (280,000)
- **calculatedRepaid**: Should be 0 (totalRepayable - outstandingBalance)
- **calculatedPercentage**: Should be 0%

### If You See Negative Values:

If the console shows something like:
```
calculatedRepaid: -80000,
calculatedPercentage: -28.57
```

Then the frontend is receiving **wrong data** from the API.

---

## Step 4: Check the Network Response

1. In Developer Tools, click on the **Network** tab
2. Refresh the page (F5)
3. Look for a request to `member/loans` (it should be a GET request)
4. Click on it
5. Click on the **Response** tab
6. You should see JSON data like:

```json
[
  {
    "id": 13,
    "loanNumber": "LN-2026-00004",
    "status": "DISBURSED",
    "amount": 200000,
    "totalInterest": 80000,
    "totalRepayable": 280000,
    "outstandingBalance": 280000,
    "monthlyRepayment": 23333.33,
    "repayments": []
  }
]
```

### What to Check:

- **outstandingBalance**: Should equal totalRepayable
- **totalRepayable**: Should be amount + totalInterest

### If You See Different Values:

If the API response shows `outstandingBalance: 360000` or something else, then the **backend is sending wrong data**.

---

## Step 5: Share the Output

Once you've completed Steps 1-4, share:

1. **Console output** - Copy the [DEBUG] messages
2. **Network response** - Copy the JSON from the Response tab
3. **What you see on screen** - Screenshot of the repayment progress showing negative values

This will help Claude identify exactly where the bug is.

---

## Quick Checklist

- [ ] Token is stored in localStorage (not null)
- [ ] You can see the Member Dashboard
- [ ] You can navigate to the Loans tab
- [ ] You can see your loans listed
- [ ] You can see the [DEBUG] messages in the console
- [ ] You can see the Network response for member/loans

If all of these are checked, you have everything needed to debug the issue.

---

## If You're Still Getting 401 Unauthorized

This means the token is not being sent with the API request.

### Possible Causes:

1. **Token is null** - You're not logged in
2. **Token is expired** - Log out and log in again
3. **Token is invalid** - Clear localStorage and log in again

### How to Fix:

```javascript
// Clear everything
localStorage.clear();

// Then refresh the page
location.reload();

// Then log in again
```

---

## If You Can't See the [DEBUG] Messages

The console.log might not be showing because:

1. **You're not on the Loans page** - Make sure you're on the Loans tab
2. **The loans haven't loaded yet** - Wait a few seconds
3. **The console is filtered** - Make sure the filter is set to "All" or "Info"
4. **The page is cached** - Try a hard refresh: **Ctrl+Shift+R** (Windows) or **Cmd+Shift+R** (Mac)

---

## What Claude Needs to Fix This

Once you provide the console output and network response, Claude can:

1. **Identify the root cause** - Is the backend sending wrong data, or is the frontend calculating wrong?
2. **Apply the fix** - Either fix the backend API or fix the frontend calculation
3. **Add safety guards** - Prevent negative values from ever displaying
4. **Verify the fix** - Test that repayment progress shows 0% and 0 KES for loans with no repayments

---

## Example: What Correct Output Should Look Like

### Console Output:
```
[DEBUG] Loan 13 (LN-2026-00004) - Raw API Data: {
  id: 13,
  loanNumber: "LN-2026-00004",
  status: "DISBURSED",
  amount: 200000,
  totalInterest: 80000,
  totalRepayable: 280000,
  outstandingBalance: 280000,
  calculatedRepaid: 0,
  calculatedPercentage: 0
}
```

### Network Response:
```json
{
  "id": 13,
  "loanNumber": "LN-2026-00004",
  "status": "DISBURSED",
  "amount": 200000,
  "totalInterest": 80000,
  "totalRepayable": 280000,
  "outstandingBalance": 280000,
  "monthlyRepayment": 23333.33,
  "repayments": []
}
```

### Frontend Display:
```
Progress: KES 0 / KES 280,000
0% repaid
KES 280,000 remaining
```

If you see this, the bug is fixed!

---

## Example: What Wrong Output Looks Like

### Console Output (WRONG):
```
[DEBUG] Loan 13 (LN-2026-00004) - Raw API Data: {
  id: 13,
  loanNumber: "LN-2026-00004",
  status: "DISBURSED",
  amount: 200000,
  totalInterest: 80000,
  totalRepayable: 280000,
  outstandingBalance: 360000,  // ← WRONG! Should be 280,000
  calculatedRepaid: -80000,    // ← WRONG! Should be 0
  calculatedPercentage: -28.57 // ← WRONG! Should be 0
}
```

If you see this, the backend is sending wrong data for `outstandingBalance`.

---

## Need Help?

If you get stuck:

1. Make sure you're logged in (token is not null)
2. Make sure you're on the Loans page
3. Make sure Developer Tools are open (F12)
4. Make sure you're looking at the Console tab
5. Try a hard refresh: **Ctrl+Shift+R**

Then share the console output and network response with Claude.
