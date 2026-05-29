# What You Need to Do - Clear Action Summary

## The Problem

Your loans are showing **negative repayment values** (-80,000 KES and -40%) when they should show **zero** (0 KES and 0%).

The database is correct. The bug is in the frontend.

---

## Why This Happened

The frontend is displaying exactly the **interest amount as negative**, which means it's either:
1. Using the wrong field in the calculation
2. Receiving wrong data from the API
3. Doing the calculation backwards

---

## What You Need to Do

### Step 1: Make Sure You're Logged In

1. Go to http://localhost:3000/member
2. If you see a login form, enter your credentials and click Login
3. Wait for the page to load your dashboard
4. You should see your balance information

### Step 2: Open Developer Tools

1. Press **F12** on your keyboard
2. You should see a panel open at the bottom of the screen
3. Click on the **Console** tab

### Step 3: Check Your Token

In the console, type this and press Enter:

```javascript
localStorage.getItem('token')
```

**If you see `null`:** You're not logged in. Go back to Step 1.

**If you see a long string starting with `eyJ`:** You're logged in. Continue to Step 4.

### Step 4: Go to Loans Page

1. Click on the **Loans** tab in your dashboard
2. Wait for your loans to load
3. Keep the Developer Tools open

### Step 5: Look at the Console

You should see messages like:

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

**Copy this entire message and share it with Claude.**

### Step 6: Check the Network Response

1. Click on the **Network** tab in Developer Tools
2. Refresh the page (F5)
3. Look for a request called `member/loans` (it should be a GET request)
4. Click on it
5. Click on the **Response** tab
6. You should see JSON data

**Copy this JSON and share it with Claude.**

---

## What to Share with Claude

1. **The console output** - The [DEBUG] messages
2. **The network response** - The JSON data
3. **A screenshot** - Show what you see on the screen (the negative values)

---

## Why This Matters

With this information, Claude can:
1. See exactly what data the frontend is receiving
2. See exactly what calculations the frontend is doing
3. Identify where the bug is
4. Fix it

Without this information, Claude is just guessing.

---

## If You Get Stuck

### "I don't see the [DEBUG] messages"

- Make sure you're on the Loans page
- Make sure the loans have loaded
- Try refreshing the page (F5)
- Try a hard refresh (Ctrl+Shift+R on Windows, Cmd+Shift+R on Mac)
- Make sure the Console tab is selected (not Network or Elements)

### "I see `null` when I check the token"

- You're not logged in
- Go back to the login page
- Enter your credentials
- Click Login
- Wait for the dashboard to load
- Then try again

### "I can't find the member/loans request in Network"

- Make sure you're on the Loans page
- Make sure the Network tab was open BEFORE you navigated to Loans
- Try refreshing the page (F5) with Network tab open
- Look for any request that contains "loans" in the name

---

## Quick Checklist

Before you share with Claude, make sure you have:

- [ ] Logged in successfully (token is not null)
- [ ] Navigated to the Loans page
- [ ] Opened Developer Tools (F12)
- [ ] Looked at the Console tab
- [ ] Found the [DEBUG] messages
- [ ] Copied the console output
- [ ] Found the member/loans request in Network tab
- [ ] Copied the JSON response
- [ ] Taken a screenshot of the negative values

---

## What Happens Next

1. You share the debug data with Claude
2. Claude analyzes the data to find the root cause
3. Claude fixes the bug
4. You test the fix
5. Your loans show 0 KES and 0% (correct values)

---

## Important Notes

- **Don't try to fix the database** - The database is already correct
- **Don't try to fix the backend** - The backend is already correct
- **Just provide the debug data** - That's all Claude needs to fix this

---

## Questions?

If you have any questions about these steps, ask Claude. But first, try to get the debug data. That's the most important thing.

---

## TL;DR (Too Long; Didn't Read)

1. Log in
2. Go to Loans page
3. Open Developer Tools (F12)
4. Look at Console tab
5. Find [DEBUG] messages
6. Copy console output
7. Copy Network response for member/loans
8. Share with Claude

That's it!
