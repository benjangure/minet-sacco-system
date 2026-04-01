# APK Testing Checklist

## Pre-Testing Setup

- [ ] APK file located: `minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk`
- [ ] Backend server running at `192.168.0.195:8080`
- [ ] Samsung A14 on same WiFi network
- [ ] APK transferred to phone
- [ ] Unknown sources enabled in Settings
- [ ] APK installed on phone

## Installation Verification

- [ ] App appears in app drawer
- [ ] App icon displays correctly
- [ ] App opens without crashing
- [ ] Splash screen shows

## Login Test

- [ ] App loads login screen
- [ ] Username field accepts input
- [ ] Password field accepts input
- [ ] Login button is clickable
- [ ] Login with member1/password succeeds
- [ ] Dashboard loads after login
- [ ] Member name displays correctly

## Dashboard Test

- [ ] Member information displays
- [ ] Account balance shows
- [ ] Recent transactions list visible
- [ ] Notifications bell icon present
- [ ] Menu items are accessible
- [ ] No error messages on dashboard

## Deposit Request Test (NEW FIX ✅)

### Submission
- [ ] Savings page loads
- [ ] "Make Deposit" button visible
- [ ] Amount input accepts numbers
- [ ] Receipt upload works
- [ ] Can take photo for receipt
- [ ] Can select photo from gallery
- [ ] Submit button is clickable
- [ ] Success message appears after submit

### Verification
- [ ] Deposit appears in recent transactions
- [ ] Amount is correct
- [ ] Timestamp is current
- [ ] Status shows correctly
- [ ] Account balance increased

## M-Pesa Integration Test (NEW FIX ✅)

### Successful Payment
- [ ] Go to Savings → Make Deposit
- [ ] Enter amount: 100
- [ ] Upload receipt
- [ ] Click "Confirm Transaction"
- [ ] M-Pesa STK push appears
- [ ] Complete payment in M-Pesa
- [ ] Success message appears
- [ ] Transaction appears in recent
- [ ] Balance increased

### Failed Payment
- [ ] Go to Savings → Make Deposit
- [ ] Enter amount: 100
- [ ] Upload receipt
- [ ] Click "Confirm Transaction"
- [ ] M-Pesa STK push appears
- [ ] Cancel/decline payment
- [ ] Error message appears
- [ ] Transaction does NOT appear in recent ✅ (NEW FIX)
- [ ] Balance NOT increased

## Loan Application Test

- [ ] Loans page loads
- [ ] "Apply for Loan" button visible
- [ ] Loan form displays all fields
- [ ] Can enter loan amount
- [ ] Can select loan term
- [ ] Can enter purpose
- [ ] Submit button works
- [ ] Success message appears
- [ ] Loan appears in loan list

## Navigation Test

- [ ] All menu items clickable
- [ ] Back button works
- [ ] Navigation is smooth
- [ ] No stuck screens
- [ ] Can navigate between pages

## Notifications Test

- [ ] Notifications bell shows count
- [ ] Can open notifications
- [ ] Notifications display correctly
- [ ] Can mark as read
- [ ] Notifications persist

## Performance Test

- [ ] App loads in < 10 seconds
- [ ] Navigation is responsive
- [ ] No lag when scrolling
- [ ] Buttons respond immediately
- [ ] No freezing or crashes

## Offline Test

- [ ] Turn off WiFi
- [ ] Try to perform action
- [ ] Error message appears (not crash)
- [ ] Turn WiFi back on
- [ ] App works normally again

## UI/UX Test

- [ ] Text is readable
- [ ] Buttons are clearly visible
- [ ] Colors are appropriate
- [ ] Layout is organized
- [ ] No overlapping elements
- [ ] Responsive to screen size

## Error Handling Test

- [ ] Invalid login shows error
- [ ] Network error shows message
- [ ] Empty fields show validation
- [ ] No crashes on errors
- [ ] Error messages are clear

## Logout Test

- [ ] Logout button visible
- [ ] Logout works
- [ ] Returns to login screen
- [ ] Can login again

## Crash Test

- [ ] No crashes during testing
- [ ] App recovers from errors
- [ ] No ANR (Application Not Responding) errors
- [ ] Smooth operation throughout

## Final Verification

- [ ] All critical features work
- [ ] No major bugs found
- [ ] Performance is acceptable
- [ ] User experience is good
- [ ] Ready for next phase

## Issues Found

| Issue | Severity | Description | Status |
|-------|----------|-------------|--------|
| | | | |
| | | | |
| | | | |

## Test Summary

**Total Tests:** ___
**Passed:** ___
**Failed:** ___
**Issues Found:** ___

**Overall Status:** ☐ PASS ☐ FAIL ☐ NEEDS FIXES

## Tester Information

- **Tester Name:** _______________
- **Device:** Samsung A14
- **Test Date:** _______________
- **Test Duration:** _______________
- **Backend IP:** 192.168.0.195:8080
- **WiFi Network:** _______________

## Notes

```
[Space for additional notes and observations]




```

## Sign-Off

- **Tested By:** _______________
- **Date:** _______________
- **Approved By:** _______________
- **Date:** _______________

---

## Key Fixes Verified

✅ **Deposit Approval Transaction Fix**
- [ ] Deposit approval works
- [ ] Balance updates correctly
- [ ] Transaction record created
- [ ] No rollback-only errors

✅ **M-Pesa Failed Transaction Fix**
- [ ] Failed transactions don't appear in recent
- [ ] Only confirmed transactions recorded
- [ ] Transaction tracking accurate

✅ **APK Build Configuration**
- [ ] APK builds successfully
- [ ] APK installs without errors
- [ ] App runs without crashes

---

**Print this checklist and use it during testing!**
