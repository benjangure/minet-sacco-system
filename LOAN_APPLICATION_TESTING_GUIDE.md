# Loan Application Testing Guide

## ✅ APK BUILD COMPLETE

**APK Location**: `minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk`

**Build Status**: ✅ SUCCESS (7 seconds)

## 📱 DEPLOYMENT TO SAMSUNG A14

1. Connect Samsung A14 to PC via USB
2. Enable USB Debugging on phone (Settings → Developer Options → USB Debugging)
3. Run: `adb install -r minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk`
4. Or manually copy APK to phone and install

## 🧪 TESTING CHECKLIST

### Phase 1: Desktop Testing (npm run dev)

**Test User**: Use your manually onboarded test users

1. **Loan Product Dropdown**
   - [ ] Navigate to Member Portal → Apply for Loan
   - [ ] Verify dropdown shows all loan products
   - [ ] Each product shows: name, max amount
   - [ ] No errors in console

2. **Product Details Display**
   - [ ] Select a loan product
   - [ ] Verify displays:
     - Interest rate
     - Min/Max amount
     - Min/Max term (months)
   - [ ] Details update when product changes

3. **Amount Validation**
   - [ ] Try entering amount below minimum → Error message
   - [ ] Try entering amount above maximum → Error message
   - [ ] Enter valid amount → No error
   - [ ] Error messages are clear and helpful

4. **Duration Validation**
   - [ ] Try entering duration below minimum → Error message
   - [ ] Try entering duration above maximum → Error message
   - [ ] Enter valid duration → No error
   - [ ] Error messages show the allowed range

5. **Eligibility Display**
   - [ ] Verify shows "Your Loan Eligibility" section
   - [ ] Shows max eligible amount (3x savings)
   - [ ] Shows savings balance
   - [ ] Shows shares balance
   - [ ] Shows total balance
   - [ ] Shows eligibility status (Eligible/Not Eligible)

6. **Guarantor Search**
   - [ ] Enter employee ID of a test guarantor
   - [ ] Click "Search" button
   - [ ] Verify guarantor found and displayed
   - [ ] Shows: Name, Employee ID, Member Number
   - [ ] Click "Add" button
   - [ ] Guarantor added to list

7. **Guarantor Management**
   - [ ] Add multiple guarantors (up to 3)
   - [ ] Verify counter shows "Added Guarantors (X/3)"
   - [ ] Remove guarantor by clicking X
   - [ ] Verify removed from list
   - [ ] Try adding same guarantor twice → Error message
   - [ ] Try adding 4th guarantor → Error message

8. **Loan Summary**
   - [ ] Fill amount and duration
   - [ ] Verify summary shows:
     - Amount
     - Duration
     - Interest rate
     - Estimated monthly payment
   - [ ] Calculation is correct

9. **Form Submission**
   - [ ] Fill all required fields
   - [ ] Click "Submit Application"
   - [ ] Success message appears
   - [ ] Redirected to dashboard
   - [ ] No errors in console

### Phase 2: Mobile Testing (Samsung A14)

**Same tests as Phase 1, but on mobile device**

1. **UI Responsiveness**
   - [ ] All elements visible on small screen
   - [ ] No horizontal scrolling needed
   - [ ] Buttons are easily tappable
   - [ ] Text is readable

2. **Touch Interactions**
   - [ ] Dropdown opens/closes smoothly
   - [ ] Search button responds to tap
   - [ ] Add/Remove buttons work
   - [ ] Submit button works

3. **Keyboard**
   - [ ] Keyboard appears when needed
   - [ ] Keyboard dismisses properly
   - [ ] No layout shift when keyboard appears

### Phase 3: Guarantor Approval Workflow

**Test User 1**: Member applying for loan
**Test User 2**: Guarantor

1. **Member Applies for Loan**
   - [ ] Member logs in
   - [ ] Applies for loan with Test User 2 as guarantor
   - [ ] Receives success message
   - [ ] Redirected to dashboard

2. **Guarantor Receives Notification**
   - [ ] Guarantor logs in
   - [ ] Sees notification about loan guarantee request
   - [ ] Notification shows member name and loan amount

3. **Guarantor Reviews Request**
   - [ ] Guarantor clicks "Guarantor Requests" button
   - [ ] Dialog opens showing pending requests
   - [ ] Clicks on the loan request
   - [ ] Modal opens showing:
     - Member name and ID
     - Loan amount
     - Loan product
     - Term
     - Monthly repayment
     - Purpose

4. **Guarantor Eligibility Display**
   - [ ] Shows guarantor's savings balance
   - [ ] Shows guarantor's shares balance
   - [ ] Shows current pledges (frozen)
   - [ ] Shows available capacity
   - [ ] Shows active guarantorships count
   - [ ] Shows eligibility status (can/cannot guarantee)
   - [ ] Shows any errors or warnings

5. **Guarantor Approves**
   - [ ] Click "Approve" button
   - [ ] Success message appears
   - [ ] Modal closes
   - [ ] Guarantor request removed from list

6. **Loan Status After Approval**
   - [ ] Member logs in
   - [ ] Checks loan status
   - [ ] Status should be PENDING_LOAN_OFFICER_REVIEW
   - [ ] Loan officer receives notification

7. **Guarantor Rejects**
   - [ ] (Test with different loan)
   - [ ] Click "Reject" button
   - [ ] Enter rejection reason
   - [ ] Click "Confirm Rejection"
   - [ ] Success message appears
   - [ ] Member receives notification about rejection

### Phase 4: Member Portal Downloads

**Test all four downloads on mobile**

1. **Deposit Receipt Download**
   - [ ] Navigate to Dashboard
   - [ ] Find a deposit request
   - [ ] Click "View Receipt"
   - [ ] File downloads successfully
   - [ ] Can open with native app

2. **Account Statement Download**
   - [ ] Navigate to Reports
   - [ ] Select "Account Statement"
   - [ ] Download button works
   - [ ] File downloads successfully

3. **Loan Statement Download**
   - [ ] Navigate to Reports
   - [ ] Select "Loan Statement"
   - [ ] Download button works
   - [ ] File downloads successfully

4. **Transaction History Download**
   - [ ] Navigate to Reports
   - [ ] Select "Transaction History"
   - [ ] Download button works
   - [ ] File downloads successfully

## 🐛 TROUBLESHOOTING

### Loan Product Dropdown Empty
- Check backend is running
- Check `/loan-products` endpoint returns data
- Check browser console for errors

### Guarantor Search Not Working
- Verify employee ID is correct
- Check backend endpoint `/member/member-by-employee-id/{id}`
- Check member exists and is ACTIVE

### Guarantor Approval Modal Not Opening
- Check backend endpoint `/loans/member/guarantor-requests`
- Check member has pending guarantor requests
- Check browser console for errors

### Downloads Not Working on Mobile
- Verify Capacitor plugins installed: `@capacitor/filesystem`, `@capacitor-community/file-opener`
- Check file permissions in AndroidManifest.xml
- Check browser console for errors

## 📊 SUCCESS CRITERIA

✅ All loan product validations work
✅ Guarantor search by employee ID works
✅ Guarantor approval workflow complete
✅ All member portal downloads work on mobile
✅ No console errors
✅ Responsive on mobile device
✅ All notifications sent correctly

## 📝 NOTES

- Loan numbers are NOT assigned until disbursement
- Loan status should be PENDING_GUARANTOR_APPROVAL after application
- All guarantors must approve before moving to loan officer
- Member can only borrow up to 3x their savings
- Guarantor can only guarantee if they have sufficient capacity
