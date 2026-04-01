# Quick Reference - Loan Application Testing

## 🎯 WHAT TO TEST

### Desktop (npm run dev)
```
1. Loan product dropdown → shows all products
2. Select product → shows min/max amount and term
3. Enter amount → validates against product limits
4. Enter duration → validates against product limits
5. Search guarantor by employee ID → finds member
6. Add guarantor → appears in list
7. Submit → success message
```

### Mobile (Samsung A14)
```
Same as desktop, but on phone
+ Verify responsive layout
+ Verify touch interactions work
+ Verify downloads work
```

### Guarantor Approval
```
1. Member applies for loan with guarantor
2. Guarantor logs in
3. Clicks "Guarantor Requests"
4. Reviews loan details and eligibility
5. Clicks "Approve" or "Reject"
6. Loan status updates
```

## 📱 APK LOCATION

```
minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk
```

## 🔧 INSTALL APK

```powershell
adb install -r minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk
```

## 🌐 DESKTOP TESTING

```powershell
cd minetsacco-main
npm run dev
# Open http://localhost:5173
```

## ✅ KEY VALIDATIONS

| Field | Min | Max | Error Message |
|-------|-----|-----|---------------|
| Amount | Product Min | Product Max | "Loan amount must be at least..." |
| Duration | Product Min | Product Max | "Loan term must be at least..." |
| Eligibility | - | 3x Savings | "Amount exceeds your maximum..." |
| Guarantors | 1 | 3 | "Maximum 3 guarantors allowed" |

## 📊 TEST DATA

**Member**: Use your manually onboarded test users
**Guarantor**: Another test user with sufficient savings
**Loan Product**: Any active product (e.g., "Emergency Loan")

## 🐛 COMMON ISSUES

| Issue | Solution |
|-------|----------|
| Dropdown empty | Check backend running, check `/loan-products` endpoint |
| Guarantor not found | Verify employee ID exists, member is ACTIVE |
| Download fails | Check Capacitor plugins installed, file permissions |
| Approval modal empty | Check member has pending guarantor requests |

## 📝 IMPORTANT NOTES

- Loan numbers assigned ONLY on disbursement
- Loan status: PENDING_GUARANTOR_APPROVAL → PENDING_LOAN_OFFICER_REVIEW
- All guarantors must approve before moving forward
- Member can borrow max 3x their savings
- Guarantor must have sufficient available capacity

## 🎯 SUCCESS INDICATORS

✅ No console errors
✅ All validations work
✅ Guarantor workflow complete
✅ Downloads work on mobile
✅ Responsive on phone
✅ Notifications sent

## 📞 QUICK COMMANDS

```powershell
# Build frontend
cd minetsacco-main
npm run build

# Sync Capacitor
npx cap sync android

# Build APK
cd android
.\gradlew.bat assembleDebug

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Run dev server
cd minetsacco-main
npm run dev
```

---

**Status**: ✅ READY FOR TESTING
**Last Build**: March 31, 2026
