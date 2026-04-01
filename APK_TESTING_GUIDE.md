# APK Testing Guide - Samsung A14

## APK Location

The newly built APK with all latest fixes is located at:
```
minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk
```

**File size:** ~50-60 MB (debug APK)

## What's Included in This APK

✅ **Latest Fixes:**
- Fixed deposit approval transaction issue (no more rollback-only errors)
- Fixed M-Pesa transaction display (failed transactions no longer show in recent)
- All previous bug fixes and features

✅ **Features:**
- Member login and dashboard
- Deposit requests with receipt upload
- Loan applications
- Savings tracking
- M-Pesa integration
- Notifications
- Audit trail
- Reports

## Transfer APK to Samsung A14

### Method 1: USB Cable (Recommended)

1. **Connect phone to computer via USB cable**
   - Phone should show "File Transfer" or "MTP" mode
   - If not, go to Settings → Developer Options → USB Configuration → Select "File Transfer"

2. **Copy APK file**
   - On Windows: Open File Explorer
   - Navigate to: `minetsacco-main/android/app/build/outputs/apk/debug/`
   - Right-click `app-debug.apk` → Copy

3. **Paste on phone**
   - In File Explorer, navigate to phone storage
   - Create folder: `Downloads` (if doesn't exist)
   - Paste the APK file there

### Method 2: Email/Cloud Storage

1. **Email the APK to yourself**
   - Attach `app-debug.apk` to email
   - Open email on phone
   - Download the attachment

2. **Or use Google Drive/OneDrive**
   - Upload APK to cloud storage
   - Open on phone and download

### Method 3: ADB (Android Debug Bridge)

If you have ADB installed:
```bash
adb install minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk
```

## Install APK on Samsung A14

1. **Enable Unknown Sources**
   - Go to Settings → Security
   - Enable "Unknown sources" or "Install unknown apps"
   - Select "Files" app and allow installation

2. **Locate the APK**
   - Open Files app
   - Navigate to Downloads folder
   - Find `app-debug.apk`

3. **Install**
   - Tap on `app-debug.apk`
   - Tap "Install"
   - Wait for installation to complete
   - Tap "Open" or find app in app drawer

## Testing Checklist

### 1. Login Test
- [ ] Open app
- [ ] Login with member credentials (e.g., username: member1, password: password)
- [ ] Should see member dashboard
- [ ] Check that API URL is correct (192.168.0.195:8080)

### 2. Deposit Request Test (NEW FIX)
- [ ] Go to Savings page
- [ ] Click "Make Deposit"
- [ ] Enter amount: 5000
- [ ] Upload receipt (take photo or select from gallery)
- [ ] Submit
- [ ] Should see success message
- [ ] Go back to Savings - should see deposit in "Recent Transactions"
- [ ] **Important:** If you cancel/fail the M-Pesa payment, the transaction should NOT appear in recent transactions

### 3. M-Pesa Integration Test (NEW FIX)
- [ ] Go to Savings → Make Deposit
- [ ] Enter amount: 100 (small amount for testing)
- [ ] Upload receipt
- [ ] Click "Confirm Transaction"
- [ ] M-Pesa STK push should appear
- [ ] **Test 1 - Success:** Complete the payment → Should see success, transaction appears in recent
- [ ] **Test 2 - Failure:** Cancel the M-Pesa prompt → Should see error, transaction does NOT appear in recent

### 4. Dashboard Test
- [ ] Check member information displays correctly
- [ ] Check account balance shows
- [ ] Check recent transactions list
- [ ] Check notifications bell icon

### 5. Loan Application Test
- [ ] Go to Loans page
- [ ] Click "Apply for Loan"
- [ ] Fill in loan details
- [ ] Submit
- [ ] Should see success message

### 6. Navigation Test
- [ ] Test all menu items work
- [ ] Test back button navigation
- [ ] Test logout and login again

### 7. Offline Test
- [ ] Turn off WiFi/mobile data
- [ ] Try to perform an action
- [ ] Should show error message (not crash)
- [ ] Turn data back on
- [ ] Should work normally

## Test Credentials

Use these credentials to test:

**Member Account:**
- Username: `member1`
- Password: `password`
- Member Number: `MEM001`

**Teller Account (for testing approvals on web):**
- Username: `teller1`
- Password: `password`

## Important Notes

### Network Configuration
- The APK is configured to connect to: `http://192.168.0.195:8080/api`
- Make sure your Samsung A14 is on the same WiFi network as your backend server
- If backend is on different IP, you need to rebuild APK with new IP

### Backend Must Be Running
- The backend server must be running for the app to work
- If backend is not running, you'll see connection errors

### Debug APK
- This is a debug APK (not signed for production)
- Can only be installed on devices with "Unknown sources" enabled
- For production, you'd need to sign the APK

## Troubleshooting

### Issue: "App not installed"
**Solution:** 
- Enable "Unknown sources" in Settings → Security
- Try installing again

### Issue: "Cannot connect to server"
**Solution:**
- Check backend is running
- Verify phone is on same WiFi as backend
- Check IP address in .env file matches your backend IP
- Rebuild APK if IP changed

### Issue: "Login failed"
**Solution:**
- Verify credentials are correct
- Check backend is running
- Check network connection

### Issue: "Deposit approval shows success but no changes in database"
**Solution:**
- This should be FIXED now with the transaction fix
- If still happening, restart backend and try again

### Issue: "Failed M-Pesa transactions showing in recent"
**Solution:**
- This should be FIXED now
- If still happening, clear app cache and reinstall

## Performance Notes

- First load may take 5-10 seconds
- Subsequent loads should be faster
- If app is slow, try:
  - Clearing app cache (Settings → Apps → Minet SACCO → Storage → Clear Cache)
  - Restarting phone
  - Checking WiFi signal strength

## Uninstall

To uninstall:
1. Go to Settings → Apps
2. Find "Minet SACCO" or similar
3. Tap "Uninstall"
4. Confirm

## Next Steps

After testing:
1. Document any issues found
2. Test on multiple devices if possible
3. Test with real M-Pesa transactions (if available)
4. Provide feedback on user experience
5. Once satisfied, can proceed to production APK signing

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Check backend logs for errors
3. Check phone logs: Settings → About → Build number (tap 7 times to enable Developer Options) → Developer Options → Show logs
4. Take screenshots of errors for reference
