# How to Request a Loan Top-Up

## 🎯 Quick Guide for Members

The **"Request Top-Up"** button is now **visible directly on your loan card** without needing to expand anything!

### Step-by-Step Instructions:

1. **Login to Member Portal**
   - Go to the member portal at your SACCO's web address
   - Login with your credentials

2. **Navigate to Loans Tab**
   - Click on the **"Loans"** tab in your member dashboard
   - This is located in the main navigation tabs

3. **Find Your Active/Disbursed Loan**
   - Look for loan cards with status: **"Active"** or **"Disbursed"** (green badge)
   - Only active loans with outstanding balance show the top-up button

4. **Click "Request Top-Up" Button**
   - You'll see a **purple button** with an arrow icon labeled **"Request Top-Up"**
   - The button appears directly on the loan card - **no need to expand the card!**
   - Click this button to start the top-up process

5. **Complete the Top-Up Request Form**
   - Enter the top-up amount you need
   - Select guarantors (just like your first loan)
   - Submit the request

6. **Wait for Guarantor Approvals**
   - Each guarantor will receive a notification
   - They need to approve their guarantee portion
   - You can track progress in the **"My Guarantees"** section

7. **Treasurer Review & Disbursement**
   - Once all guarantors approve, the treasurer reviews
   - After approval, funds are disbursed to your account
   - Your loan balance updates automatically

---

## 🔍 Where is the Button Located?

### Visual Location:
```
┌─────────────────────────────────────────────────────────┐
│ Loan #12345                              [Active] [▼]   │
│ Amount: KES 50,000                                      │
│                                                         │
│ [🔼 Request Top-Up]  ← PURPLE BUTTON HERE              │
└─────────────────────────────────────────────────────────┘
```

### Button Appearance:
- **Color:** Purple background (`bg-purple-600`)
- **Icon:** Arrow pointing up (🔼)
- **Text:** "Request Top-Up"
- **Size:** Small/compact button
- **Location:** Below the loan amount, on the collapsed card

---

## ✅ Eligibility Criteria

You can only request a top-up if:

1. ✅ Loan status is **ACTIVE** or **DISBURSED**
2. ✅ Outstanding balance is **greater than 0** (loan not fully paid)
3. ✅ You have active guarantors available
4. ✅ Your SACCO allows top-up requests

---

## 📋 Top-Up Workflow

```
Member Requests Top-Up
         ↓
Select Guarantors & Amount
         ↓
Guarantors Approve/Reject
         ↓
All Approved? → Treasurer Reviews
         ↓
Treasurer Approves → Funds Disbursed
         ↓
Loan Balance Updated
```

---

## 🎨 Recent UI Changes (v2.0)

**Before:** Button was hidden inside expanded loan card - users had to click to expand first

**Now:** Button is **always visible** on active/disbursed loan cards - no expansion needed!

This change was made to improve discoverability and user experience.

---

## ❓ Troubleshooting

### "I don't see the button"

**Check these:**
1. Is your loan status "Active" or "Disbursed"? (Check the green badge)
2. Do you have an outstanding balance > 0?
3. Try refreshing the page (Ctrl+R or F5)
4. Clear browser cache and reload

### "Button is grayed out or disabled"

**Possible reasons:**
- Your loan might be fully repaid
- Loan status might have changed (check status badge)
- There might be a pending top-up request already

### "I clicked but nothing happens"

**Try:**
1. Check browser console for errors (F12 → Console tab)
2. Ensure you're logged in (session might have expired)
3. Try a different browser (Chrome, Firefox, Edge)
4. Contact your SACCO administrator

---

## 🔐 Security & Permissions

- Only **members** can request top-ups on their own loans
- **Guarantors** receive notifications to approve/reject
- **Treasurer** has final approval authority
- All actions are logged in the audit trail

---

## 📞 Need Help?

Contact your SACCO administrator or support team if:
- Button doesn't appear on eligible loans
- You encounter errors during the request process
- You need clarification on eligibility criteria

---

**Last Updated:** 2026-07-23  
**Feature Version:** 2.0 (Button now visible without expansion)
