# Production Deployment Guide - Minet SACCO System

**Date:** August 5, 2026  
**Build Status:** ✅ Production Ready  
**Server IP:** 10.39.60.15

---

## 📦 Build Artifacts

### Frontend (PWA)
- **Location:** `minetsacco-main/dist/`
- **Size:** ~2 MB (compressed assets)
- **API Endpoint:** http://10.39.60.15:9090
- **Features:**
  - Progressive Web App (PWA) with offline support
  - Push notifications enabled
  - Service Worker v4
  - Session expiry auto-logout
  - Auto-refresh on all member portal pages

### Backend (Spring Boot)
- **Location:** `backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar`
- **Size:** 103.8 MB
- **Port:** 9090
- **Profile:** Production (uses `minetsacco` database)

---

## 🚀 Deployment Steps

### 1. Backend Deployment

#### Prerequisites on Server:
- Java 17 or higher installed
- MySQL 8.0 running
- Database `minetsacco` created
- Database user `minetsacco` with password `0a0b0c0D.`

#### Steps:

**A. Upload JAR file to server:**
```bash
# On your local machine, copy to server
scp backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar user@10.39.60.15:/opt/minet-sacco/
```

**B. Create application.properties (if not exists on server):**
```bash
# On server
mkdir -p /opt/minet-sacco/config
nano /opt/minet-sacco/config/application.properties
```

Paste this configuration:
```properties
spring.profiles.active=prod
server.port=9090
server.address=0.0.0.0

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true&failOverReadOnly=false&maxReconnects=10
spring.datasource.username=minetsacco
spring.datasource.password=0a0b0c0D.
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true

# JWT
jwt.secret=YourVerySecureSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm
jwt.expiration=86400000

# Email (Office365)
spring.mail.host=smtp.office365.com
spring.mail.port=587
spring.mail.username=no_reply@minet.co.ke
spring.mail.password=fhcyvypyydghmyfp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Push Notifications VAPID Keys
push.vapid.public.key=BN21Dp26FQFRhUYw11RNHgQ1d1tibAFWBVA8Eh-mBuwkvdzxzq_27IXLahyAmXyBHcvWx5tXpMIOpE-RrJSywcE
push.vapid.private.key=OjKrwYCDGAluvQ_Bet1h1zFc8U3D8aZiTcf5kCIlLYI
push.vapid.subject=mailto:admin@minetsacco.co.ke

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1500000
spring.datasource.hikari.keepalive-time=120000

# File uploads
kyc.upload.directory=/opt/minet-sacco/uploads/kyc
deposit.upload.directory=/opt/minet-sacco/uploads/deposits
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB
```

**C. Create upload directories:**
```bash
mkdir -p /opt/minet-sacco/uploads/kyc
mkdir -p /opt/minet-sacco/uploads/deposits
chmod -R 755 /opt/minet-sacco/uploads
```

**D. Create systemd service (recommended for auto-restart):**
```bash
sudo nano /etc/systemd/system/minet-sacco.service
```

Paste this configuration:
```ini
[Unit]
Description=Minet SACCO Backend Service
After=mysql.service

[Service]
Type=simple
User=your-user
WorkingDirectory=/opt/minet-sacco
ExecStart=/usr/bin/java -jar /opt/minet-sacco/minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.config.location=/opt/minet-sacco/config/application.properties
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

**E. Start the service:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable minet-sacco
sudo systemctl start minet-sacco
sudo systemctl status minet-sacco
```

**F. Check logs:**
```bash
sudo journalctl -u minet-sacco -f
```

**Alternative: Run manually (not recommended for production):**
```bash
cd /opt/minet-sacco
nohup java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.config.location=config/application.properties > backend.log 2>&1 &
```

---

### 2. Frontend Deployment

#### Option A: IIS Deployment (Windows Server)

**A. Copy dist folder to IIS:**
```powershell
# Copy entire dist folder to server
Copy-Item -Recurse minetsacco-main\dist\* \\10.39.60.15\c$\inetpub\wwwroot\minetsacco\
```

**B. Configure IIS:**
1. Open IIS Manager
2. Create new Application Pool: `MinetSaccoAppPool` (.NET CLR: No Managed Code)
3. Create new Website:
   - Name: `Minet SACCO`
   - Physical path: `C:\inetpub\wwwroot\minetsacco`
   - Binding: Port 80 (or 443 for HTTPS)
   - Application Pool: `MinetSaccoAppPool`

**C. URL Rewrite (Important for SPA routing):**

The `web.config` file is already in the dist folder with these rules:
```xml
<rewrite>
  <rules>
    <rule name="React Routes" stopProcessing="true">
      <match url=".*" />
      <conditions logicalGrouping="MatchAll">
        <add input="{REQUEST_FILENAME}" matchType="IsFile" negate="true" />
        <add input="{REQUEST_FILENAME}" matchType="IsDirectory" negate="true" />
      </conditions>
      <action type="Rewrite" url="/" />
    </rule>
  </rules>
</rewrite>
```

**D. MIME Types (for PWA):**
Ensure these MIME types are configured in IIS:
- `.json` → `application/json`
- `.webmanifest` → `application/manifest+json`
- `.js` → `application/javascript`

---

#### Option B: Apache/Nginx Deployment (Linux Server)

**For Nginx:**

1. Copy dist folder:
```bash
sudo mkdir -p /var/www/minetsacco
sudo cp -r minetsacco-main/dist/* /var/www/minetsacco/
sudo chown -R www-data:www-data /var/www/minetsacco
```

2. Create Nginx config:
```bash
sudo nano /etc/nginx/sites-available/minetsacco
```

Paste:
```nginx
server {
    listen 80;
    server_name 10.39.60.15;
    root /var/www/minetsacco;
    index index.html;

    # PWA and SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Service worker (no cache)
    location = /service-worker.js {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        expires 0;
    }

    # Manifest
    location = /manifest.json {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        expires 0;
    }
}
```

3. Enable and restart:
```bash
sudo ln -s /etc/nginx/sites-available/minetsacco /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

## 🔥 Firewall Configuration

Ensure these ports are open:

```bash
# Backend
sudo ufw allow 9090/tcp

# Frontend (if using Nginx/Apache)
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Verify
sudo ufw status
```

---

## ✅ Post-Deployment Verification

### 1. Backend Health Check
```bash
# Check if backend is running
curl http://10.39.60.15:9090/actuator/health

# Expected response:
# {"status":"UP"}
```

### 2. Frontend Access
- **Staff Portal:** http://10.39.60.15/login
- **Member Portal:** http://10.39.60.15/member/login

### 3. Test PWA Features
1. Open browser DevTools → Application tab
2. Check Service Worker is registered
3. Check Manifest loaded
4. Test "Add to Home Screen" prompt

### 4. Test Push Notifications
1. Login as member
2. Go to Settings → Notifications
3. Toggle "Enable Push Notifications" ON
4. Click "Send Test Notification"
5. Should receive browser notification

### 5. Test Session Expiry
1. Login to any portal
2. Wait for session to expire (or manually expire token in backend)
3. Make any action (click refresh, navigate)
4. Should auto-logout and redirect to login page

---

## 📊 Monitoring

### Backend Logs
```bash
# If using systemd
sudo journalctl -u minet-sacco -f

# If running manually
tail -f /opt/minet-sacco/backend.log
```

### Nginx Logs
```bash
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### Database Connections
```sql
-- Check active connections
SHOW PROCESSLIST;

-- Check HikariCP pool status (check backend logs)
```

---

## 🔄 Updates & Rollback

### To Update Frontend:
```bash
# Build new version locally
npm run build

# Copy to server
scp -r minetsacco-main/dist/* user@10.39.60.15:/var/www/minetsacco/

# Force service worker update
# Users will see update on next visit
```

### To Update Backend:
```bash
# Build new JAR locally
./mvnw.cmd clean package -DskipTests

# Copy to server
scp backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar user@10.39.60.15:/opt/minet-sacco/

# Restart service
ssh user@10.39.60.15 "sudo systemctl restart minet-sacco"
```

### Rollback Plan:
```bash
# Keep previous versions
cp minet-sacco-backend-0.0.1-SNAPSHOT.jar minet-sacco-backend-0.0.1-SNAPSHOT.jar.backup-$(date +%Y%m%d)

# To rollback, restore backup and restart
```

---

## 🔐 Security Checklist

- [ ] Change JWT secret in production
- [ ] Use HTTPS (install SSL certificate)
- [ ] Set strong database passwords
- [ ] Configure CORS properly (already done in SecurityConfig)
- [ ] Keep VAPID private key secret
- [ ] Secure file upload directories
- [ ] Enable firewall on server
- [ ] Regular database backups
- [ ] Keep Java and MySQL updated

---

## 📱 APK Deployment (Future)

When ready to deploy Android APK:
1. User will need to configure backend URL in app settings
2. Default URL in APK: http://10.39.60.15:9090
3. APK will be available at: `minetsacco-main/android/app/build/outputs/apk/`

---

## 🆘 Troubleshooting

### Backend won't start:
```bash
# Check Java version
java -version

# Check database connection
mysql -u minetsacco -p -h localhost minetsacco

# Check port 9090 is free
netstat -an | grep 9090
```

### Frontend not loading:
- Clear browser cache (Ctrl+Shift+Delete)
- Check browser console for errors
- Verify API URL in .env.production
- Check CORS errors in backend logs

### Push notifications not working:
- Verify VAPID keys match in backend and frontend
- Check browser supports notifications (not Safari on iOS)
- Ensure HTTPS is enabled (required for push)

### Session expiry not redirecting:
- Check browser console for errors
- Verify api.ts interceptor is working
- Check 401/403 responses in Network tab

---

## 📞 Support

For deployment issues:
- Check logs first
- Review this guide
- Contact: admin@minetsacco.co.ke

---

**Build Information:**
- Frontend Build: August 5, 2026 3:00 PM
- Backend Build: August 5, 2026 3:02 PM
- Build Machine: Windows 11
- Node Version: Latest
- Java Version: 17
- Maven Version: 3.9.6

✅ **Ready for Production Deployment**
