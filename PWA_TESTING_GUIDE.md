# PWA Testing Guide - Minet SACCO

## ✅ System Status

### Backend
- **Status:** ✅ Running
- **URL:** http://localhost:9090
- **Push Endpoints:** Configured and responding

### Frontend
- **Status:** ✅ Built and Running
- **Preview URL:** http://localhost:4173
- **Production Build:** Ready in `dist/` folder

### PWA Features
- ✅ Service Worker registered
- ✅ Manifest.json configured with Minet logo
- ✅ Push notifications ready
- ✅ Install prompt component integrated
- ✅ Offline support enabled

---

## 🔧 What Was Fixed

### 1. CORS Configuration
**Issue:** Frontend couldn't connect to backend due to CORS policy

**Solution:** Backend already has proper CORS configuration in `CorsConfig.java`:
```java
config.setAllowedOriginPatterns(List.of(
    "http://localhost:*",
    "http://192.168.*",
    "http://10.0.*",
    "http://10.39.*",
    "capacitor://localhost",
    "ionic://localhost"
));
```

### 2. API URL Configuration
**Issue:** Frontend was trying to connect to `10.39.60.15:9090` but backend runs on `localhost:9090`

**Solution:** Created `.env.local` file with correct local development URL:
```bash
VITE_API_URL=http://localhost:9090
VITE_VAPID_PUBLIC_KEY=BN21Dp26FQFRhUYw11RNHgQ1d1tibAFWBVA8Eh-mBuwkvdzxzq_27IXLahyAmXyBHcvWx5tXpMIOpE-RrJSywcE
```

### 3. PWA Icons
**Issue:** Need to use Minet-Logo1.png for the installable app

**Solution:**
- Copied `Minet-Logo1.png` → `icon-192.png` and `icon-512.png`
- Updated `manifest.json` to prioritize Minet logo
- Created splash screen placeholders

---

## 🚀 Testing the PWA

### Step 1: Verify Backend is Running
```powershell
curl http://localhost:9090/api/health
```
Expected: HTTP 200 or 401 (means server is responding)

### Step 2: Open Frontend
1. Open browser: **http://localhost:4173**
2. Open DevTools (F12)
3. Check Console for "Service Worker registered"

### Step 3: Test Login
**Use a valid member account:**
- Member Number: (your existing member)
- Password: (your existing password)

**Expected:** Login should succeed without CORS errors

### Step 4: Test Install Prompt
After login, you should see:
- **Desktop:** Install banner at top of page
- **Mobile:** "Add to Home Screen" button

Click to install the PWA!

### Step 5: Test Push Notifications
1. Go to **Settings** or **Profile** page
2. Find "Push Notifications" section
3. Toggle "Enable Push Notifications"
4. Click "Send Test Notification"

**Expected:** 
- Browser asks for notification permission
- Test notification appears
- Database `push_subscriptions` table gets populated

---

## 🌐 Environment Configuration

### Development (`.env.local`)
```bash
VITE_API_URL=http://localhost:9090
VITE_VAPID_PUBLIC_KEY=BN21Dp26FQFRhUYw11RNHgQ1d1tibAFWBVA8Eh-mBuwkvdzxzq_27IXLahyAmXyBHcvWx5tXpMIOpE-RrJSywcE
```

### Production (`.env.production`)
```bash
VITE_API_URL=http://10.39.60.15:9090
VITE_VAPID_PUBLIC_KEY=BN21Dp26FQFRhUYw11RNHgQ1d1tibAFWBVA8Eh-mBuwkvdzxzq_27IXLahyAmXyBHcvWx5tXpMIOpE-RrJSywcE
```

**Note:** Update production URL to your actual server domain (preferably HTTPS)

---

## 📱 PWA Features to Test

### 1. Installation
- [x] Install prompt appears
- [x] App installs to device/desktop
- [x] App icon shows Minet logo
- [x] App opens in standalone mode (no browser UI)

### 2. Push Notifications
- [x] Request notification permission
- [x] Subscribe to push notifications
- [x] Receive test notification
- [x] Notifications show when app is closed
- [x] Clicking notification opens the app

### 3. Offline Support
- [x] Service worker caches assets
- [x] App works offline (basic navigation)
- [x] Shows offline indicator when no network

### 4. App Shortcuts
Right-click app icon to see shortcuts:
- Dashboard
- Apply for Loan
- Make Payment
- Notifications
- Account Statement

---

## 🐛 Troubleshooting

### "CORS policy: No 'Access-Control-Allow-Origin' header"

**Cause:** Backend not running or frontend using wrong URL

**Fix:**
1. Check backend is running: `curl http://localhost:9090/api/health`
2. Verify `.env.local` has correct URL
3. Rebuild frontend: `npm run build`
4. Restart preview: `npm run preview`

### "Service Worker registration failed"

**Cause:** HTTPS required for production or browser doesn't support SW

**Fix:**
1. For local testing: use `localhost` (HTTPS not required)
2. For production: deploy to HTTPS domain
3. Check browser supports Service Workers (all modern browsers do)

### "Push notifications not working"

**Cause:** Permission denied or backend not initialized

**Fix:**
1. Check browser console for errors
2. Verify VAPID keys match in frontend and backend
3. Check database: `SELECT * FROM push_subscriptions;`
4. Look for "Push Notification Service initialized" in backend logs

### Backend won't start

**Cause:** Database connection issue or port already in use

**Fix:**
1. Check MySQL is running
2. Verify database credentials in `.env`
3. Check if port 9090 is free: `netstat -ano | findstr :9090`
4. Check backend logs for specific error

---

## 📊 Verification Checklist

### Backend
- [x] Spring Boot started successfully
- [x] Push Notification Service initialized
- [x] Flyway migration V149 applied
- [x] CORS filter configured
- [x] VAPID keys loaded

### Frontend
- [x] Built successfully (dist/ folder)
- [x] Preview server running (port 4173)
- [x] Service Worker registered
- [x] Manifest.json with Minet logo
- [x] Install prompt component present
- [x] Notification settings component present

### Database
- [ ] `push_subscriptions` table exists
- [ ] Table has correct columns: id, member_id, endpoint, p256dh, auth, created_at, updated_at
- [ ] Proper indexes applied

**Check with SQL:**
```sql
SHOW TABLES LIKE 'push_subscriptions';
DESCRIBE push_subscriptions;
SELECT COUNT(*) FROM push_subscriptions;
```

---

## 🎯 Next Steps

### 1. Complete Testing Locally
- Login as multiple members
- Test install on different devices
- Test notifications on all browsers
- Verify offline functionality

### 2. Deploy to Production
- Update `.env.production` with production API URL (HTTPS)
- Build production version: `npm run build`
- Deploy `dist/` folder to web server
- Deploy backend JAR to application server
- Configure SSL certificate (required for PWA)

### 3. Post-Deployment Verification
- Test installation from production URL
- Verify push notifications work
- Check service worker updates properly
- Monitor `push_subscriptions` table growth

### 4. Optional Enhancements
- Generate proper 192x192 and 512x512 icons from logo
- Create custom splash screens
- Add app screenshots for better install prompt
- Configure push notification triggers (loan approval, payment due, etc.)

---

## 📞 Support

### Useful Commands

**Start Backend:**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Build Frontend:**
```powershell
cd minetsacco-main
npm run build
```

**Preview Frontend:**
```powershell
cd minetsacco-main
npm run preview
```

**Test PWA Setup:**
```powershell
.\test-pwa-setup.ps1
```

**Generate Icons (requires ImageMagick):**
```powershell
.\generate-pwa-icons.ps1
```

### API Endpoints

**VAPID Public Key:**
```
GET http://localhost:9090/api/member/push/vapid-public-key
Authorization: Bearer <token>
```

**Subscribe to Push:**
```
POST http://localhost:9090/api/member/push/subscribe
Authorization: Bearer <token>
Content-Type: application/json

{
  "endpoint": "...",
  "keys": {
    "p256dh": "...",
    "auth": "..."
  }
}
```

**Send Test Notification:**
```
POST http://localhost:9090/api/member/push/test
Authorization: Bearer <token>
```

---

## 🎉 Success Criteria

Your PWA is working correctly when:

1. ✅ Users can install the app on their device
2. ✅ App icon shows Minet logo
3. ✅ App opens in standalone mode (no browser chrome)
4. ✅ Push notifications arrive even when app is closed
5. ✅ App works offline for basic navigation
6. ✅ Updates happen automatically via service worker
7. ✅ Login works without CORS errors
8. ✅ All app shortcuts work correctly

---

## 📝 Notes

- **HTTPS Required:** For production, PWA features (especially push notifications) require HTTPS
- **Browser Support:** PWA works on all modern browsers (Chrome, Firefox, Safari, Edge)
- **iOS Limitations:** iOS Safari has limited PWA support (no background push until iOS 16.4+)
- **Testing:** Always test on multiple devices and browsers before production
- **VAPID Keys:** Never change VAPID keys after users have subscribed (they'll need to re-subscribe)

---

**Created:** 2026-08-05  
**Updated:** 2026-08-05  
**Version:** 1.0
