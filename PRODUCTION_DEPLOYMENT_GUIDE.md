# Minet SACCO - Production Deployment Guide

**Last Updated**: May 2026  
**Status**: Ready for Production Deployment

---

## Table of Contents
1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Database Deployment](#database-deployment)
3. [Backend Deployment](#backend-deployment)
4. [Frontend Deployment](#frontend-deployment)
5. [Post-Deployment Testing](#post-deployment-testing)
6. [Monitoring & Maintenance](#monitoring--maintenance)

---

## Pre-Deployment Checklist

Before deploying to production, ensure you have:

- [ ] Server with Linux OS (Ubuntu 20.04+ recommended)
- [ ] Java 21 JDK installed on server
- [ ] MySQL 8.x installed on server
- [ ] Node.js 18+ installed on server
- [ ] Nginx or Apache web server
- [ ] SSL certificate (Let's Encrypt recommended)
- [ ] Domain name configured
- [ ] Backup of current database (if migrating)
- [ ] All environment variables documented
- [ ] Team trained on deployment process

---

## Database Deployment

### Step 1: Prepare MySQL Server

**On Your Server:**

```bash
# Update system packages
sudo apt update && sudo apt upgrade -y

# Install MySQL 8.x
sudo apt install mysql-server -y

# Secure MySQL installation
sudo mysql_secure_installation

# Start MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql
```

### Step 2: Create Production Database

```bash
# Connect to MySQL
mysql -u root -p

# Create database and user
CREATE DATABASE sacco_db_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'sacco_user'@'localhost' IDENTIFIED BY 'StrongPassword123!@#';
GRANT ALL PRIVILEGES ON sacco_db_prod.* TO 'sacco_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Step 3: Backup Current Database (if migrating)

```bash
# Export current database from development
mysqldump -u root sacco_db > sacco_db_backup.sql

# Transfer to server
scp sacco_db_backup.sql user@server:/home/user/

# Import on production server
mysql -u sacco_user -p sacco_db_prod < sacco_db_backup.sql
```

### Step 4: Verify Database

```bash
# Connect and verify
mysql -u sacco_user -p sacco_db_prod

# Check tables
SHOW TABLES;
SELECT COUNT(*) FROM members;
EXIT;
```

---

## Backend Deployment

### Step 1: Prepare Backend on Server

```bash
# SSH into server
ssh user@server

# Create application directory
mkdir -p /opt/minet-sacco
cd /opt/minet-sacco

# Clone repository (or upload files)
git clone https://github.com/your-org/minetsacco-main.git
cd minetsacco-main/backend
```

### Step 2: Configure Production Properties

**Edit `src/main/resources/application.properties`:**

```properties
# Server Configuration
server.port=8080
server.address=0.0.0.0

# Database Configuration (PRODUCTION)
spring.datasource.url=jdbc:mysql://localhost:3306/sacco_db_prod?createDatabaseIfNotExist=false&useSSL=true&serverTimezone=UTC
spring.datasource.username=sacco_user
spring.datasource.password=StrongPassword123!@#
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

# Flyway (Database Migrations)
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true

# JWT (CHANGE THIS TO A STRONG SECRET)
jwt.secret=GenerateNewSecureKeyWith256BitsMinimumLength_ChangeThisInProduction
jwt.expiration=1800000

# Email Configuration (SendGrid or AWS SES)
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=SG.your-sendgrid-api-key-here
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Logging (Production - less verbose)
logging.level.com.minet.sacco=INFO
logging.level.org.springframework=WARN

# File Upload Directories
kyc.upload.directory=/var/minet-sacco/uploads/kyc
deposit.upload.directory=/var/minet-sacco/uploads/deposits

# M-Pesa Configuration (Update with production credentials)
mpesa.consumer-key=YOUR_PRODUCTION_CONSUMER_KEY
mpesa.consumer-secret=YOUR_PRODUCTION_CONSUMER_SECRET
mpesa.business-short-code=YOUR_PRODUCTION_SHORT_CODE
mpesa.passkey=YOUR_PRODUCTION_PASSKEY
mpesa.environment=production
mpesa.callback-url=https://yourdomain.com/api/mpesa/callback/stk
mpesa.timeout-url=https://yourdomain.com/api/mpesa/callback/timeout
```

### Step 3: Create Upload Directories

```bash
# Create directories for file uploads
sudo mkdir -p /var/minet-sacco/uploads/kyc
sudo mkdir -p /var/minet-sacco/uploads/deposits

# Set permissions
sudo chown -R $USER:$USER /var/minet-sacco
sudo chmod -R 755 /var/minet-sacco
```

### Step 4: Build Backend

```bash
cd /opt/minet-sacco/minetsacco-main/backend

# Clean and build
mvn clean package -DskipTests

# Verify JAR was created
ls -lh target/sacco-*.jar
```

### Step 5: Create Systemd Service

**Create `/etc/systemd/system/minet-sacco-backend.service`:**

```ini
[Unit]
Description=Minet SACCO Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=sacco
WorkingDirectory=/opt/minet-sacco/minetsacco-main/backend
ExecStart=/usr/bin/java -jar target/sacco-*.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### Step 6: Start Backend Service

```bash
# Create sacco user
sudo useradd -r -s /bin/bash sacco

# Set permissions
sudo chown -R sacco:sacco /opt/minet-sacco
sudo chown -R sacco:sacco /var/minet-sacco

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable minet-sacco-backend
sudo systemctl start minet-sacco-backend

# Check status
sudo systemctl status minet-sacco-backend

# View logs
sudo journalctl -u minet-sacco-backend -f
```

---

## Frontend Deployment

### Step 1: Build Frontend

**On Your Development Machine:**

```bash
cd minetsacco-main

# Install dependencies
npm install

# Build for production
npm run build

# Verify dist folder was created
ls -la dist/
```

### Step 2: Upload Frontend to Server

```bash
# From your development machine
scp -r dist/* user@server:/home/user/frontend-dist/
```

### Step 3: Configure Nginx

**Create `/etc/nginx/sites-available/minet-sacco`:**

```nginx
# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS Configuration
server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    # SSL Certificates (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Frontend
    location / {
        root /var/www/minet-sacco;
        try_files $uri $uri/ /index.html;
        expires 1h;
        add_header Cache-Control "public, immutable";
    }

    # Static assets (cache longer)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /var/www/minet-sacco;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # API Proxy to Backend
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Deny access to sensitive files
    location ~ /\. {
        deny all;
    }
}
```

### Step 4: Deploy Frontend Files

```bash
# Create web directory
sudo mkdir -p /var/www/minet-sacco
sudo chown -R www-data:www-data /var/www/minet-sacco

# Copy frontend files
sudo cp -r /home/user/frontend-dist/* /var/www/minet-sacco/

# Set permissions
sudo chmod -R 755 /var/www/minet-sacco
```

### Step 5: Enable Nginx Site

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/minet-sacco /etc/nginx/sites-enabled/

# Test Nginx configuration
sudo nginx -t

# Restart Nginx
sudo systemctl restart nginx
```

### Step 6: Setup SSL Certificate (Let's Encrypt)

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx -y

# Get certificate
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com

# Auto-renewal
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

---

## Post-Deployment Testing

### Test 1: Backend Health Check

```bash
# Check if backend is running
curl https://yourdomain.com/api/auth/health

# Expected response:
# {"success":true,"message":"Backend is healthy","data":null}
```

### Test 2: Frontend Access

```bash
# Open in browser
https://yourdomain.com

# Verify:
# - Page loads without errors
# - Logo and styling display correctly
# - No console errors (F12 → Console tab)
```

### Test 3: Login Test

1. Open https://yourdomain.com
2. Login with test credentials:
   - Username: `admin`
   - Password: `password`
3. Verify dashboard loads
4. Check browser console for errors (F12)

### Test 4: API Endpoints

```bash
# Get JWT token
TOKEN=$(curl -s -X POST https://yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r '.token')

# Test members endpoint
curl -H "Authorization: Bearer $TOKEN" \
  https://yourdomain.com/api/members

# Should return member list
```

### Test 5: Database Connectivity

```bash
# SSH to server
ssh user@server

# Check database
mysql -u sacco_user -p sacco_db_prod -e "SELECT COUNT(*) as member_count FROM members;"
```

### Test 6: File Upload Test

1. Login as customer support user
2. Navigate to KYC Document Upload
3. Upload a test document
4. Verify file appears in `/var/minet-sacco/uploads/kyc/`

### Test 7: Critical Workflows

Test these key workflows:

- [ ] Member login and dashboard access
- [ ] Staff login and member management
- [ ] Loan application and approval
- [ ] Deposit and withdrawal
- [ ] KYC document upload
- [ ] Report generation
- [ ] User management

---

## Monitoring & Maintenance

### Daily Checks

```bash
# Check backend service
sudo systemctl status minet-sacco-backend

# Check database
mysql -u sacco_user -p sacco_db_prod -e "SELECT NOW();"

# Check disk space
df -h

# Check logs
sudo journalctl -u minet-sacco-backend -n 50
```

### Weekly Tasks

```bash
# Backup database
mysqldump -u sacco_user -p sacco_db_prod > /backups/sacco_db_$(date +%Y%m%d).sql

# Check SSL certificate expiry
sudo certbot certificates

# Review error logs
sudo tail -100 /var/log/nginx/error.log
```

### Monthly Tasks

- Review system performance metrics
- Update system packages: `sudo apt update && sudo apt upgrade -y`
- Verify backups are working
- Test disaster recovery procedure
- Review user access logs

### Troubleshooting

**Backend won't start:**
```bash
sudo journalctl -u minet-sacco-backend -n 100
# Check application.properties for errors
# Verify MySQL is running: sudo systemctl status mysql
```

**Frontend shows 404:**
```bash
# Check Nginx configuration
sudo nginx -t
# Verify files in /var/www/minet-sacco
ls -la /var/www/minet-sacco/
```

**Database connection error:**
```bash
# Test connection
mysql -u sacco_user -p -h localhost sacco_db_prod
# Check credentials in application.properties
```

**SSL certificate issues:**
```bash
# Renew certificate
sudo certbot renew --force-renewal
# Check certificate
sudo certbot certificates
```

---

## Environment Variables (Optional but Recommended)

Instead of hardcoding values, use environment variables:

**Create `/opt/minet-sacco/.env`:**

```bash
DB_URL=jdbc:mysql://localhost:3306/sacco_db_prod
DB_USER=sacco_user
DB_PASSWORD=StrongPassword123!@#
JWT_SECRET=YourSecureJWTSecret
MPESA_KEY=your-mpesa-key
MPESA_SECRET=your-mpesa-secret
SENDGRID_API_KEY=your-sendgrid-key
```

**Update systemd service to load env:**

```ini
[Service]
EnvironmentFile=/opt/minet-sacco/.env
ExecStart=/usr/bin/java -jar target/sacco-*.jar
```

---

## Rollback Procedure

If deployment fails:

```bash
# Stop backend
sudo systemctl stop minet-sacco-backend

# Restore previous database backup
mysql -u sacco_user -p sacco_db_prod < /backups/sacco_db_previous.sql

# Restore previous JAR
cp /backups/sacco-previous.jar /opt/minet-sacco/minetsacco-main/backend/target/

# Restart backend
sudo systemctl start minet-sacco-backend

# Verify
sudo systemctl status minet-sacco-backend
```

---

## Performance Optimization

### Database Optimization

```sql
-- Create indexes for frequently queried columns
CREATE INDEX idx_member_status ON members(status);
CREATE INDEX idx_loan_member ON loans(member_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);

-- Enable query cache
SET GLOBAL query_cache_type = 1;
SET GLOBAL query_cache_size = 268435456;
```

### Backend Optimization

- Connection pool already configured in `application.properties`
- Caching enabled (Caffeine cache)
- Batch processing enabled for Hibernate

### Frontend Optimization

- Already built with production optimizations
- Gzip compression enabled in Nginx
- Static assets cached for 30 days

---

## Security Checklist

- [ ] Change default admin password
- [ ] Update JWT secret to strong random value
- [ ] Enable HTTPS/SSL
- [ ] Configure firewall rules
- [ ] Set up database backups
- [ ] Enable audit logging
- [ ] Configure email for alerts
- [ ] Set up monitoring/alerting
- [ ] Document all credentials securely
- [ ] Restrict SSH access
- [ ] Disable unnecessary services

---

## Support & Escalation

**For Issues:**
1. Check logs: `sudo journalctl -u minet-sacco-backend -f`
2. Verify services: `sudo systemctl status minet-sacco-backend mysql nginx`
3. Check disk space: `df -h`
4. Review error logs: `/var/log/nginx/error.log`

**Contact:**
- Technical Team: tech@minetsacco.com
- Emergency: +254-XXX-XXX-XXX

---

## Deployment Checklist

- [ ] Database created and configured
- [ ] Backend built and deployed
- [ ] Frontend built and deployed
- [ ] Nginx configured with SSL
- [ ] All services running
- [ ] Health checks passing
- [ ] Login test successful
- [ ] File uploads working
- [ ] Backups configured
- [ ] Monitoring enabled
- [ ] Team trained
- [ ] Documentation updated

---

**Deployment Date**: _______________  
**Deployed By**: _______________  
**Verified By**: _______________

