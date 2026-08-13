# Render Free Tier Backend Issue - SOLVED

## Problem
Android app shows "**failed to fetch**" when trying to login.

## Root Cause
**Render's free tier spins down your backend after 15 minutes of inactivity.**

When the backend is asleep:
- First request triggers a "cold start" 
- Cold start takes **3-5 minutes** to complete
- Android app timeout is **15 seconds**
- Result: Request times out → "failed to fetch" error

## Evidence
```powershell
# Test from PC showed:
Response time: 30.0459974 seconds
✗ Backend error: The operation has timed out.
```

The backend was asleep and didn't respond within 30 seconds.

## Solution

### Immediate Fix (Done ✓)
Backend has been woken up by pinging it. **Try the APK now** - it should work!

Latest APK location:
```
minet-sacco-system/minetsacco-main/android/app/build/outputs/apk/release/app-release.apk
```
- Built: August 12, 2026 at 5:06 PM
- Size: 4.68 MB
- URL configured: `https://minetsacco-backend-docker.onrender.com`

### Long-term Solutions

#### Option 1: Keep-Alive GitHub Action (Implemented ✓)
A GitHub Action (`.github/workflows/keep-backend-alive.yml`) now pings your backend every 10 minutes to prevent it from sleeping.

**To activate:**
1. Push this code to GitHub
2. Go to your repo → Actions tab
3. Enable workflows
4. The action will run automatically every 10 minutes

#### Option 2: Upgrade Render Plan (Recommended for Production)
- **Free tier**: Spins down after 15 minutes, cold start takes 3-5 minutes
- **Paid tier ($7/month)**: Never spins down, always instant response

For production use with real members, the paid tier is essential.

#### Option 3: External Cron Service (Alternative)
Use a free service like:
- **UptimeRobot** (https://uptimerobot.com) - Free monitoring + keep-alive
- **Cron-job.org** (https://cron-job.org) - Free scheduled pings

Set them to ping `https://minetsacco-backend-docker.onrender.com/` every 10 minutes.

## How to Test Right Now

1. **First, wake up the backend** (if it's asleep):
   ```powershell
   Invoke-WebRequest -Uri "https://minetsacco-backend-docker.onrender.com/" -TimeoutSec 60
   ```
   Wait for 401 response (means it's alive)

2. **Install the latest APK** on your phone:
   - Transfer `app-release.apk` to phone
   - Install it
   - Try logging in

3. **If it still says "failed to fetch"**:
   - Wait 30 seconds for backend to fully start
   - Try again
   - Check your phone's internet connection
   - Try opening https://minetsacco-backend-docker.onrender.com in phone's browser

## Backend Startup Time
- **Cold start** (was asleep): 3-5 minutes
- **Warm start** (recently used): 2-5 seconds
- **Hot** (actively used): < 1 second

## Status Check
You can always check if backend is alive:
```bash
curl -I https://minetsacco-backend-docker.onrender.com/
```

Expected responses:
- `401 Unauthorized` = ✓ Backend is working perfectly
- `200 OK` = ✓ Backend is working
- Timeout after 30s = ✗ Backend is asleep, wait 3-5 minutes

## Files Changed
1. ✅ `android/app/src/main/res/xml/network_security_config.xml` - Added explicit Render domain trust
2. ✅ `capacitor.config.ts` - Removed cleartext conflicts, set androidScheme to 'https'
3. ✅ `.github/workflows/keep-backend-alive.yml` - Keep-alive ping every 10 minutes

## Commits
- `f07a243` - Fix Android network security: remove cleartext conflicts, add explicit HTTPS trust for Render domain
- Previous commits with backend URL configuration

---

**Next Steps:**
1. Try the APK now (backend is awake)
2. Push code to GitHub to activate keep-alive action
3. Consider upgrading to Render paid tier for production
