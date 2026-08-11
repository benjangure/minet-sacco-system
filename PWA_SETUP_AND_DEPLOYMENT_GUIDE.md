# PWA Setup and Deployment Guide
## Minet SACCO Members Portal - Step-by-Step Implementation

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Step 1: Generate VAPID Keys](#step-1-generate-vapid-keys)
3. [Step 2: Configure Backend](#step-2-configure-backend)
4. [Step 3: Configure Frontend](#step-3-configure-frontend)
5. [Step 4: Database Migration](#step-4-database-migration)
6. [Step 5: Build and Deploy Backend](#step-5-build-and-deploy-backend)
7. [Step 6: Build and Deploy Frontend](#step-6-build-and-deploy-frontend)
8. [Step 7: Integrate Components](#step-7-integrate-components)
9. [Step 8: Testing](#step-8-testing)
10. [Step 9: Production Deployment](#step-9-production-deployment)
11. [Troubleshooting](#troubleshooting)
12. [Maintenance Tasks](#maintenance-tasks)

---

## Prerequisites

Before starting, ensure you have:

- ✅ Node.js (v18 or higher) installed
- ✅ Java JDK 17 installed
- ✅ Maven installed
- ✅ MySQL database running
- ✅ HTTPS enabled (required for push notifications in production)
- ✅ Git for version control
- ✅ Access to server deployment environment

---

## Step 1: Generate VAPID Keys

VAPID (Voluntary Application Server Identification) keys are used to authenticate your server when sending push notifications.

### Using npx (Recommended)

```bash
# Navigate to your project directory
cd c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system

# Generate VAPID keys
npx web-push generate-vapid-keys
```

### Output Example:

```
=======================================
Public Key:
BNxZ8xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

Private Key:
5J3kxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
=======================================
```

**IMPORTANT:**
- ✅ **Save both keys securely** - you'll need them for configuration
- ✅ **Keep the private key secret** - never commit it to version control
- ✅ The public key will be shared with frontend
- ✅ Generate keys only once - changing them invalidates all existing subscriptions

---

## Step 2: Configure Backend

### 2.1 Update application.properties

**File:** `backend/src/main/resources/application.properties`

Find the push notification section and replace the placeholder values:

```properties
# ===== PUSH NOTIFICATION CONFIGURATION =====
push.vapid.public.key=BNxZ8xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
push.vapid.private.key=5J3kxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
push.vapid.subject=mailto:admin@minetsacco.co.ke
```

**Configuration Notes:**
- `push.vapid.public.key`: Your generated public key
- `push.vapid.private.key`: Your generated private key (keep secret!)
- `push.vapid.subject`: Your contact email (mailto: format required)

### 2.2 Update pom.xml (Already Done)

The following dependencies have been added:

```xml
<!-- Web Push for Push Notifications -->
<dependency>
    <groupId>nl.martijndwars</groupId>
    <artifactId>web-push</artifactId>
    <version>5.1.1</version>
</dependency>

<!-- BouncyCastle for Cryptography -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

### 2.3 Install Dependencies

```bash
cd backend
mvn clean install
```

---

## Step 3: Configure Frontend

### 3.1 Update Environment Files

**Development Environment:** `minetsacco-main/.env.development`

```env
VITE_API_URL=http://localhost:9090
VITE_VAPID_PUBLIC_KEY=BNxZ8xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Production Environment:** `minetsacco-main/.env.production`

```env
VITE_API_URL=http://10.39.60.15:9090
VITE_VAPID_PUBLIC_KEY=BNxZ8xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**IMPORTANT:** Use the same public key in both files that matches the backend configuration.

### 3.2 Install Dependencies

```bash
cd minetsacco-main
npm install
```

---

## Step 4: Database Migration

The push subscription table needs to be created in the database.

### 4.1 Enable Flyway (if disabled)

**File:** `backend/src/main/resources/application.properties`

```properties
# Flyway
spring.flyway.enabled=true
```

### 4.2 Run the Migration

**Option A: Automatic (on application startup)**

The migration will run automatically when you start the Spring Boot application.

**Option B: Manual (using SQL script)**

```bash
# Connect to MySQL
mysql -u minetsacco -p minetsacco

# Run the migration script
source backend/src/main/resources/db/migration/V149__Create_push_subscriptions_table.sql
```

### 4.3 Verify Table Creation

```sql
USE minetsacco;

SHOW TABLES LIKE 'push_subscriptions';

DESCRIBE push_subscriptions;
```

Expected output should show the table with columns: `id`, `user_id`, `endpoint`, `p256dh_key`, `auth_key`, `user_agent`, `created_at`, `updated_at`, `last_used_at`, `is_active`.

---

## Step 5: Build and Deploy Backend

### 5.1 Build the Application

```bash
cd backend

# Clean and build
mvn clean package -DskipTests

# Or with tests
mvn clean package
```

This creates: `target/minet-sacco-backend-0.0.1-SNAPSHOT.jar`

### 5.2 Run Locally for Testing

```bash
# Run with development profile
java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Or using Maven
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5.3 Verify Backend is Running

Check the logs for:
```
✅ Push Notification Service initialized successfully
VAPID Subject: mailto:admin@minetsacco.co.ke
```

Test the API:
```bash
# Check if push service is available
curl http://localhost:9090/api/member/push/vapid-public-key
```

---

## Step 6: Build and Deploy Frontend

### 6.1 Build for Production

```bash
cd minetsacco-main

# Build with production environment
npm run build
```

This creates the `dist/` directory with optimized production files.

### 6.2 Test Locally

```bash
# Preview the production build
npm run preview
```

Access at: `http://localhost:4173`

### 6.3 Deploy to Server

**Option A: Copy to Apache/Nginx**

```bash
# Copy dist folder to web server
xcopy /E /I dist C:\xampp\htdocs\minetsacco
```

**Option B: Deploy to specific server**

```powershell
# Use the existing deployment script
.\deploy-frontend-to-server.ps1
```

---

## Step 7: Integrate Components

Now integrate the PWA components into your application.

### 7.1 Add InstallPrompt Component

**File:** `minetsacco-main/src/App.tsx` or `main.tsx`

```typescript
import { InstallPrompt } from '@/components/InstallPrompt';

function App() {
  return (
    <>
      {/* Your existing app structure */}
      <InstallPrompt />
      {/* ... rest of your app */}
    </>
  );
}
```

### 7.2 Add NotificationSettings Component

**File:** `minetsacco-main/src/pages/MemberDashboard.tsx` (or appropriate settings page)

```typescript
import { NotificationSettings } from '@/components/NotificationSettings';

// In your settings/profile page
<NotificationSettings />
```

### 7.3 Initialize Push Notifications

**File:** `minetsacco-main/src/contexts/AuthContext.tsx` (or after login)

```typescript
import { initializePush } from '@/services/pushNotificationService';

// After successful login
const handleLogin = async () => {
  // ... existing login logic
  
  // Initialize push notifications
  await initializePush();
};
```

### 7.4 Add Service Worker Registration Check

**File:** `minetsacco-main/index.html` (Already added, verify it exists)

```html
<script>
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/service-worker.js')
        .then(registration => {
          console.log('Service Worker registered:', registration);
        })
        .catch(error => {
          console.log('Service Worker registration failed:', error);
        });
    });
  }
</script>
```

---

## Step 8: Testing

### 8.1 Test PWA Installation

**Desktop (Chrome/Edge):**
1. Open the app in Chrome/Edge
2. Look for the install icon in the address bar
3. Or use the custom install prompt
4. Click "Install"
5. Verify the app opens in standalone mode

**Android (Chrome):**
1. Open the app in Chrome
2. Wait for the install banner or custom prompt
3. Tap "Install" or "Add to Home Screen"
4. Find the app icon on your home screen
5. Launch and verify standalone mode

**iOS (Safari):**
1. Open the app in Safari
2. Tap the Share button
3. Scroll and tap "Add to Home Screen"
4. Verify the app icon appears

### 8.2 Test Push Notifications

**Step-by-step Test:**

1. **Enable Notifications:**
   - Login to the member portal
   - Navigate to Settings/Profile
   - Find the NotificationSettings component
   - Toggle "Enable Push Notifications"
   - Grant permission when browser prompts

2. **Send Test Notification:**
   - Click "Send Test Notification" button
   - You should receive a notification immediately
   - Click the notification to verify deep linking works

3. **Test API Endpoints:**

```bash
# Replace TOKEN with actual JWT token

# Check subscription status
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:9090/api/member/push/status

# Send test notification
curl -X POST \
  -H "Authorization: Bearer TOKEN" \
  http://localhost:9090/api/member/push/test
```

### 8.3 Test Offline Functionality

1. Open the app
2. Open DevTools → Network tab
3. Check "Offline" mode
4. Navigate between cached pages
5. Verify app still works
6. Re-enable network
7. Verify background sync works

### 8.4 Browser DevTools Testing

**Chrome DevTools:**
1. Open DevTools (F12)
2. Go to Application tab
3. Check:
   - ✅ Service Workers (registered and active)
   - ✅ Manifest (parsed correctly)
   - ✅ Cache Storage (pages cached)
   - ✅ Push Notifications (subscription active)

---

## Step 9: Production Deployment

### 9.1 Pre-Deployment Checklist

- [ ] HTTPS is enabled (required for push notifications)
- [ ] VAPID keys generated and configured
- [ ] Database migration completed
- [ ] Backend tests passing
- [ ] Frontend build successful
- [ ] Service worker registered correctly
- [ ] Manifest.json accessible
- [ ] All icons present and correct size
- [ ] Environment variables set correctly
- [ ] CORS configured properly

### 9.2 Deploy Backend

```bash
# Build production JAR
cd backend
mvn clean package -DskipTests

# Copy to server
scp target/minet-sacco-backend-0.0.1-SNAPSHOT.jar user@server:/opt/minetsacco/

# On server, run as service
sudo systemctl restart minetsacco-backend
```

### 9.3 Deploy Frontend

```bash
# Build production bundle
cd minetsacco-main
npm run build

# Deploy to server
# Option 1: Copy via SCP
scp -r dist/* user@server:/var/www/minetsacco/

# Option 2: Use deployment script
.\deploy-frontend-to-server.ps1
```

### 9.4 Configure Web Server (HTTPS Required)

**Nginx Example:**

```nginx
server {
    listen 443 ssl http2;
    server_name minetsacco.co.ke;

    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;

    root /var/www/minetsacco;
    index index.html;

    # PWA manifest and service worker
    location /manifest.json {
        add_header Cache-Control "public, max-age=3600";
    }

    location /service-worker.js {
        add_header Cache-Control "no-cache";
        add_header Service-Worker-Allowed "/";
    }

    # API proxy
    location /api/ {
        proxy_pass http://localhost:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 9.5 Verify Production Deployment

```bash
# Test HTTPS
curl https://minetsacco.co.ke/manifest.json

# Test backend API
curl https://minetsacco.co.ke/api/member/push/vapid-public-key

# Test service worker
curl https://minetsacco.co.ke/service-worker.js
```

---

## Troubleshooting

### Issue: "Push notifications not supported"

**Solution:**
- Ensure HTTPS is enabled (required in production)
- Check browser compatibility (Chrome, Firefox, Edge, Safari 16.4+)
- Verify service worker is registered

### Issue: "VAPID public key not configured"

**Solution:**
- Check `application.properties` has the correct public key
- Restart the backend application
- Verify environment variables are loaded

### Issue: "Subscription failed with HTTP 410"

**Solution:**
- Subscription has expired
- User should re-enable notifications
- Old subscription will be automatically removed

### Issue: "Service worker not registering"

**Solution:**
- Clear browser cache
- Check browser console for errors
- Verify service-worker.js is accessible at `/service-worker.js`
- Ensure HTTPS or localhost

### Issue: "Notifications not appearing"

**Solution:**
- Check browser notification permissions
- Verify subscription exists in database
- Check backend logs for push errors
- Test with a simple notification first

### Issue: "Install prompt not showing"

**Solution:**
- PWA criteria must be met (HTTPS, manifest, service worker, icons)
- User may have dismissed it (wait 7 days or clear browser data)
- Check DevTools → Application → Manifest for errors

### Issue: Database migration fails

**Solution:**
```sql
-- Check if table already exists
SHOW TABLES LIKE 'push_subscriptions';

-- If exists and needs reset
DROP TABLE IF EXISTS push_subscriptions;

-- Then run migration again
```

---

## Maintenance Tasks

### Daily Tasks

None required - system runs automatically.

### Weekly Tasks

**Monitor Subscription Health:**

```sql
-- Check active subscriptions
SELECT COUNT(*) FROM push_subscriptions WHERE is_active = true;

-- Check subscriptions by date
SELECT DATE(created_at), COUNT(*) 
FROM push_subscriptions 
GROUP BY DATE(created_at) 
ORDER BY DATE(created_at) DESC;
```

### Monthly Tasks

**1. Cleanup Old Subscriptions:**

```bash
# Call cleanup endpoint (requires admin token)
curl -X POST \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  http://localhost:9090/api/member/push/cleanup
```

Or run SQL manually:

```sql
-- Deactivate subscriptions not used in 90 days
UPDATE push_subscriptions 
SET is_active = false 
WHERE last_used_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Delete inactive subscriptions older than 180 days
DELETE FROM push_subscriptions 
WHERE is_active = false 
AND updated_at < DATE_SUB(NOW(), INTERVAL 180 DAY);
```

**2. Monitor Notification Delivery Rate:**

Check backend logs for success/failure rates:

```bash
grep "Push notification sent" backend.log | wc -l
```

**3. Update Service Worker Cache Version:**

When deploying new frontend changes:

**File:** `minetsacco-main/public/service-worker.js`

```javascript
const CACHE_NAME = 'minet-sacco-v3'; // Increment version
```

### Quarterly Tasks

**1. Rotate VAPID Keys (Optional):**

Only if security requires it:

```bash
# Generate new keys
npx web-push generate-vapid-keys

# Update backend configuration
# Update frontend environment variables
# All users will need to re-subscribe
```

**2. Review Analytics:**

- Installation rate
- Push notification opt-in rate
- Click-through rate
- User engagement metrics

---

## Integration with Existing Notification System

To send push notifications for existing events (loan approvals, deposits, etc.), integrate into your notification service:

**Example:** `backend/src/main/java/com/minet/sacco/service/NotificationService.java`

```java
@Autowired
private PushNotificationService pushNotificationService;

public void notifyLoanApproval(Loan loan) {
    User user = loan.getMember().getUser();
    
    // Create push notification
    PushNotificationDTO pushNotification = new PushNotificationDTO.Builder(
        "💰 Loan Approved!",
        String.format("Your loan of KES %,.2f has been approved.", loan.getPrincipal())
    )
    .type("LOAN_APPROVED")
    .url("/member/dashboard?tab=loans")
    .requireInteraction(true)
    .build();
    
    // Send push notification
    pushNotificationService.sendNotificationToUser(user, pushNotification);
    
    // Also create in-app notification (existing logic)
    // ...
}
```

---

## Security Recommendations

1. **Keep Private Key Secret:**
   - Never commit VAPID private key to version control
   - Use environment variables or secrets management
   - Rotate keys if compromised

2. **Validate Subscriptions:**
   - Always authenticate users before subscribing
   - Verify JWT tokens on all endpoints
   - Rate limit subscription requests

3. **HTTPS Required:**
   - PWA and push notifications require HTTPS in production
   - Use valid SSL certificates
   - Enable HSTS headers

4. **Content Security Policy:**
   - Configure CSP headers appropriately
   - Allow service worker registration
   - Restrict notification sources

---

## Monitoring and Analytics

### Track Key Metrics

**Installation Rate:**
```javascript
// Track in analytics when app is installed
window.addEventListener('appinstalled', () => {
  gtag('event', 'pwa_installed', {
    'event_category': 'PWA',
    'event_label': 'App Installed'
  });
});
```

**Push Notification Metrics:**
```sql
-- Opt-in rate
SELECT 
  (SELECT COUNT(DISTINCT user_id) FROM push_subscriptions WHERE is_active = true) /
  (SELECT COUNT(*) FROM users WHERE role = 'MEMBER') * 100 
AS opt_in_rate;

-- Subscriptions by date
SELECT DATE(created_at) as date, COUNT(*) as new_subscriptions
FROM push_subscriptions
GROUP BY DATE(created_at)
ORDER BY date DESC
LIMIT 30;
```

---

## Support and Contact

For issues or questions:
- **Email:** admin@minetsacco.co.ke
- **Documentation:** See PWA_PUSH_NOTIFICATIONS_IMPLEMENTATION.md
- **Logs Location:** `backend/logs/application.log`

---

## Quick Reference Commands

```bash
# Generate VAPID keys
npx web-push generate-vapid-keys

# Build backend
cd backend && mvn clean package

# Build frontend
cd minetsacco-main && npm run build

# Run backend locally
java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar

# Preview frontend
npm run preview

# Check subscription status
curl -H "Authorization: Bearer TOKEN" http://localhost:9090/api/member/push/status

# Send test notification
curl -X POST -H "Authorization: Bearer TOKEN" http://localhost:9090/api/member/push/test
```

---

## Success Indicators

After successful deployment, you should see:

- ✅ Install prompt appears for users
- ✅ App can be installed on home screen
- ✅ App opens in standalone mode
- ✅ Notifications can be enabled
- ✅ Test notifications are received
- ✅ Notifications open correct pages when clicked
- ✅ App works offline (cached pages)
- ✅ Backend logs show successful push deliveries
- ✅ No errors in browser console

---

**Document Version**: 1.0  
**Last Updated**: 2026-08-05  
**Author**: Kiro AI Development Environment
