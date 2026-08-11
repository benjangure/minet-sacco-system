# Quick Start - PWA Implementation
## Get Up and Running in 10 Minutes

---

## ✅ Configuration Complete!

Your VAPID keys have been configured in all the necessary files:

**Public Key:** `BN21Dp26FQFRhUYw11RNHgQ1d1tibAFWBVA8Eh-mBuwkvdzxzq_27IXLahyAmXyBHcvWx5tXpMIOpE-RrJSywcE`

**Files Updated:**
- ✅ `backend/src/main/resources/application.properties`
- ✅ `minetsacco-main/.env.development`
- ✅ `minetsacco-main/.env.production`
- ✅ `backend/pom.xml` (dependencies added)
- ✅ Database migration ready (`V149__Create_push_subscriptions_table.sql`)
- ✅ Flyway enabled in application.properties

---

## 🚀 Quick Deployment Steps

### Step 1: Build Backend (2 minutes)

```powershell
cd c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend

# Install dependencies and build
.\mvnw.cmd clean install

# Or if Maven is in PATH
mvn clean install
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Step 2: Start Backend (1 minute)

```powershell
# Start the application (migration will run automatically)
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

# Or run the JAR directly
java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

**Look for these logs:**
```
✅ Push Notification Service initialized successfully
VAPID Subject: mailto:admin@minetsacco.co.ke
Flyway: Successfully applied 1 migration to schema `minetsacco`
```

### Step 3: Build Frontend (2 minutes)

```powershell
cd c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main

# Install dependencies (if not already done)
npm install

# Build for production
npm run build
```

**Expected Output:**
```
✓ built in [time]
✓ [number] modules transformed
dist/index.html                  [size]
```

### Step 4: Test Locally (2 minutes)

```powershell
# Preview the production build
npm run preview
```

Access at: `http://localhost:4173`

**Test Checklist:**
- [ ] Open browser DevTools (F12)
- [ ] Go to Application tab
- [ ] Check Service Workers → should show "activated and running"
- [ ] Check Manifest → should show no errors
- [ ] Try installing the app (install icon in address bar)
- [ ] Test notifications in Settings/Profile

---

## 🔧 Integration Steps

### 1. Add Install Prompt to Your App

**File:** `minetsacco-main/src/App.tsx` (or wherever your root component is)

```typescript
import { InstallPrompt } from '@/components/InstallPrompt';

function App() {
  return (
    <>
      <InstallPrompt />  {/* Add this line */}
      {/* Your existing app structure */}
    </>
  );
}
```

### 2. Add Notification Settings to Profile/Settings Page

**File:** Find your profile or settings page (e.g., `src/pages/MemberProfile.tsx`)

```typescript
import { NotificationSettings } from '@/components/NotificationSettings';

function MemberProfile() {
  return (
    <div className="profile-container">
      <h1>Profile Settings</h1>
      
      {/* Your existing profile content */}
      
      {/* Add notification settings */}
      <NotificationSettings />
    </div>
  );
}
```

### 3. Initialize Push Notifications After Login

**File:** `minetsacco-main/src/contexts/AuthContext.tsx` (or your login handler)

```typescript
import { initializePush } from '@/services/pushNotificationService';

// In your login function
const handleLogin = async (credentials) => {
  try {
    // Your existing login code
    const response = await loginAPI(credentials);
    localStorage.setItem('session', JSON.stringify(response.data));
    
    // Add this: Initialize push notifications
    await initializePush();
    
    navigate('/member/dashboard');
  } catch (error) {
    console.error('Login failed:', error);
  }
};
```

---

## 🧪 Testing

### Test 1: Install the App

**Desktop (Chrome/Edge):**
1. Open `http://localhost:4173` (or your deployed URL)
2. Look for install icon in address bar (⊕ or ⬇)
3. Click to install
4. App should open in new window without browser UI

**Android (Chrome):**
1. Open the app in Chrome mobile
2. Wait for install banner or custom prompt
3. Tap "Install" or "Add to Home Screen"
4. Find app icon on home screen

### Test 2: Enable Notifications

1. Login to member portal
2. Go to Settings/Profile (where you added NotificationSettings)
3. Toggle "Enable Push Notifications"
4. Grant permission when browser asks
5. Click "Send Test Notification"
6. You should receive a notification immediately

### Test 3: Verify Backend API

```powershell
# Get your JWT token from browser localStorage
# Then test the API:

# Check subscription status
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:9090/api/member/push/status

# Send test notification
curl -X POST -H "Authorization: Bearer YOUR_TOKEN" http://localhost:9090/api/member/push/test
```

### Test 4: Check Database

```sql
-- Connect to MySQL
mysql -u minetsacco -p minetsacco

-- Check if table was created
SHOW TABLES LIKE 'push_subscriptions';

-- View subscriptions
SELECT id, user_id, endpoint, is_active, created_at 
FROM push_subscriptions;
```

---

## 📱 Send Your First Push Notification

### Backend Integration Example

Add this to any service where you handle loan approvals:

```java
@Autowired
private PushNotificationService pushNotificationService;

public void approveLoan(Loan loan) {
    // Your existing loan approval logic
    loan.setStatus(LoanStatus.APPROVED);
    loanRepository.save(loan);
    
    // NEW: Send push notification
    User user = loan.getMember().getUser();
    PushNotificationDTO notification = new PushNotificationDTO.Builder(
        "💰 Loan Approved!",
        String.format("Your loan of KES %,.2f has been approved.", loan.getPrincipal())
    )
    .type("LOAN_APPROVED")
    .url("/member/dashboard?tab=loans")
    .requireInteraction(true)
    .build();
    
    pushNotificationService.sendNotificationToUser(user, notification);
}
```

**More examples in:** `minetsacco-main/src/INTEGRATION_GUIDE.tsx`

---

## 🚨 Troubleshooting

### Issue: "Push service not initialized"

**Check:**
```powershell
# Backend logs should show:
✅ Push Notification Service initialized successfully
```

**If not, verify:**
- VAPID keys are in `application.properties`
- No typos in the keys
- Backend was restarted after adding keys

### Issue: "Service worker not registered"

**Check browser console:**
```javascript
navigator.serviceWorker.getRegistrations().then(registrations => {
  console.log('Service workers:', registrations);
});
```

**Fix:**
- Clear browser cache (Ctrl+Shift+Del)
- Make sure `/service-worker.js` is accessible
- Check for HTTPS (required in production, localhost is OK)

### Issue: "Notification permission denied"

**Fix:**
1. Click lock icon in browser address bar
2. Find "Notifications" in permissions
3. Change to "Allow"
4. Reload page

### Issue: "Table push_subscriptions doesn't exist"

**Fix:**
```sql
-- Run migration manually
mysql -u minetsacco -p minetsacco < backend/src/main/resources/db/migration/V149__Create_push_subscriptions_table.sql
```

Or check Flyway is enabled:
```properties
spring.flyway.enabled=true
```

---

## 📋 Deployment Checklist

### Before Deploying to Production:

- [ ] VAPID keys configured (✅ Done)
- [ ] Backend builds successfully
- [ ] Frontend builds successfully
- [ ] Database migration completed
- [ ] HTTPS enabled on production server (required!)
- [ ] Components integrated into UI
- [ ] Tested install prompt
- [ ] Tested push notifications
- [ ] Backend logs show no errors
- [ ] Service worker registered in production

### Production Deployment:

1. **Deploy Backend:**
   ```powershell
   # Build production JAR
   cd backend
   mvn clean package -DskipTests
   
   # Deploy to server (your deployment method)
   ```

2. **Deploy Frontend:**
   ```powershell
   # Build for production
   cd minetsacco-main
   npm run build
   
   # Deploy dist folder (your deployment method)
   ```

3. **Verify Production:**
   - Access via HTTPS (required for push notifications)
   - Test install on real devices
   - Send test notifications
   - Monitor backend logs

---

## 📊 Monitor Success

### Check Metrics:

```sql
-- Active subscriptions
SELECT COUNT(*) FROM push_subscriptions WHERE is_active = true;

-- Subscriptions today
SELECT COUNT(*) FROM push_subscriptions 
WHERE DATE(created_at) = CURDATE();

-- Subscriptions by day (last 7 days)
SELECT DATE(created_at) as date, COUNT(*) as count 
FROM push_subscriptions 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

### Backend Logs:

Monitor for successful push deliveries:
```
✅ Notification sent successfully to subscription [id]
Push notification sent: [X] successful, [Y] failed out of [Z] total
```

---

## 🎯 Success Indicators

You'll know it's working when:

- ✅ Install prompt appears on supported devices
- ✅ App can be installed to home screen
- ✅ App opens in standalone mode
- ✅ Notifications can be enabled in settings
- ✅ Test notification is received
- ✅ Clicking notification opens correct page
- ✅ Backend logs show successful deliveries
- ✅ Database contains push_subscriptions records

---

## 📚 Complete Documentation

For detailed information:

1. **Architecture & Design:** `PWA_PUSH_NOTIFICATIONS_IMPLEMENTATION.md`
2. **Full Setup Guide:** `PWA_SETUP_AND_DEPLOYMENT_GUIDE.md`
3. **Implementation Summary:** `PWA_IMPLEMENTATION_SUMMARY.md`
4. **Integration Examples:** `minetsacco-main/src/INTEGRATION_GUIDE.tsx`

---

## 🆘 Need Help?

**Check logs:**
- Backend: `backend/logs/application.log`
- Frontend: Browser DevTools Console (F12)

**Verify configuration:**
- VAPID keys in `application.properties`
- Public key in `.env` files
- Flyway enabled
- Database table exists

**Test endpoints:**
```bash
# Get VAPID public key
curl http://localhost:9090/api/member/push/vapid-public-key

# Check status (requires auth token)
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:9090/api/member/push/status
```

---

## 🎉 You're Ready!

Everything is configured and ready to deploy. Follow the integration steps above to add the components to your UI, then test and deploy!

**Next:** Integrate the components (5 minutes) → Test (5 minutes) → Deploy! 🚀
