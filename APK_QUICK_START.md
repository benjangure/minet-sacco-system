# APK Quick Start - 5 Minutes

## Get the APK

**Location:** `minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk`

**Size:** ~50-60 MB

## Transfer to Phone (Pick One)

### USB Cable (Fastest)
1. Connect phone via USB
2. Copy `app-debug.apk` to phone's Downloads folder
3. Done

### Email
1. Email the APK to yourself
2. Download on phone
3. Done

### Google Drive
1. Upload APK to Google Drive
2. Download on phone
3. Done

## Install on Samsung A14

1. **Enable Unknown Sources**
   - Settings → Security → Unknown sources → ON

2. **Install**
   - Open Files app
   - Go to Downloads
   - Tap `app-debug.apk`
   - Tap "Install"
   - Wait for completion

3. **Open App**
   - Tap "Open" or find in app drawer

## Login

- **Username:** member1
- **Password:** password

## Test Key Features

### ✅ Deposit (NEW FIX)
1. Savings → Make Deposit
2. Enter 5000
3. Upload receipt
4. Submit
5. Check recent transactions (should appear)

### ✅ M-Pesa (NEW FIX)
1. Savings → Make Deposit
2. Enter 100
3. Upload receipt
4. Confirm → M-Pesa prompt
5. **Cancel it** → Should NOT appear in recent
6. Try again → **Complete it** → Should appear in recent

### ✅ Dashboard
- Check balance displays
- Check recent transactions
- Check notifications

### ✅ Loans
- Loans → Apply for Loan
- Fill details
- Submit

## Important

⚠️ **Backend must be running** at `192.168.0.195:8080`

⚠️ **Phone must be on same WiFi** as backend

⚠️ **First load takes 5-10 seconds** (normal)

## Issues?

| Issue | Solution |
|-------|----------|
| "App not installed" | Enable Unknown sources in Settings |
| "Cannot connect" | Check backend is running, same WiFi |
| "Login failed" | Check credentials, backend running |
| App is slow | Clear cache: Settings → Apps → Minet SACCO → Storage → Clear Cache |

## What's Fixed in This APK

✅ Deposit approval now works (no more silent rollbacks)
✅ Failed M-Pesa transactions don't show in recent
✅ All previous fixes included

## Uninstall

Settings → Apps → Minet SACCO → Uninstall

---

**Ready to test?** Start with the Deposit test above!
