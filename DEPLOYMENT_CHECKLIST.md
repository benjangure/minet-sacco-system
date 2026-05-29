# Minet SACCO - Deployment Execution Checklist

**Deployment Date**: _______________  
**Deployed By**: _______________  
**Verified By**: _______________  
**Environment**: ☐ Staging  ☐ Production

---

## PRE-DEPLOYMENT (1 Day Before)

### Infrastructure Preparation
- [ ] Server access verified (SSH key working)
- [ ] Server specs confirmed (CPU, RAM, Disk)
- [ ] Java 21 installed: `java -version`
- [ ] MySQL 8.x installed: `mysql --version`
- [ ] Node.js 18+ installed: `node --version`
- [ ] Nginx installed: `nginx -v`
- [ ] Domain DNS configured
- [ ] SSL certificate ready (or Let's Encrypt setup)

### Code Preparation
- [ ] All code committed to git
- [ ] No uncommitted changes: `git status`
- [ ] Latest code pulled: `git pull origin main`
- [ ] All tests passing locally
- [ ] Build successful locally: `mvn clean package`
- [ ] Frontend builds successfully: `npm run build`

### Backup & Documentation
- [ ] Current database backed up
- [ ] Current backend JAR backed up
- [ ] Current frontend files backed up
- [ ] Deployment plan reviewed with team
- [ ] Rollback procedure documented
- [ ] Emergency contacts listed

---

## DATABASE DEPLOYMENT

### Database Setup
- [ ] MySQL service running: `sudo systemctl status mysql`
- [ ] Production database created: `sacco_db_prod`
- [ ] Production user created: `sacco_user`
- [ ] User permissions granted
- [ ] Database connection tested

**Test Command:**
```bash
mysql -u sacco_user -p sacco_db_prod -e "SELECT NOW();"
```

**Result**: _______________

### Data Migration (if applicable)
- [ ] Backup exported from development
- [ ] Backup uploaded to server
- [ ] Backup imported to production
- [ ] Data integrity verified
- [ ] Row counts match: 
  - Members: _______________
  - Loans: _______________
  - Accounts: _______________

---

## BACKEND DEPLOYMENT

### Configuration
- [ ] `application.properties` updated with production values:
  - [ ] Database URL: `jdbc:mysql://localhost:3306/sacco_db_prod`
  - [ ] Database user: `sacco_user`
  - [ ] Database password: ✓ (changed from default)
  - [ ] JWT secret: ✓ (strong random value)
  - [ ] M-Pesa consumer key: ✓ (production)
  - [ ] M-Pesa consumer secret: ✓ (production)
  - [ ] M-Pesa business short code: ✓ (production)
  - [ ] M-Pesa passkey: ✓ (production)
  - [ ] M-Pesa environment: `production`
  - [ ] SendGrid API key: ✓ (if using email)
  - [ ] Logging level: `INFO`

### Build & Deploy
- [ ] Backend code uploaded to server
- [ ] Build successful: `mvn clean package -DskipTests`
- [ ] JAR file created: `target/sacco-*.jar`
- [ ] Upload directories created:
  - [ ] `/var/minet-sacco/uploads/kyc`
  - [ ] `/var/minet-sacco/uploads/deposits`
- [ ] Permissions set correctly

### Service Setup
- [ ] Systemd service file created: `/etc/systemd/system/minet-sacco-backend.service`
- [ ] Service enabled: `sudo systemctl enable minet-sacco-backend`
- [ ] Service started: `sudo systemctl start minet-sacco-backend`
- [ ] Service status verified: `sudo systemctl status minet-sacco-backend`

**Status Output**: _______________

### Backend Verification
- [ ] Service running: `sudo systemctl is-active minet-sacco-backend`
- [ ] No errors in logs: `sudo journalctl -u minet-sacco-backend -n 20`
- [ ] Port 8080 listening: `sudo netstat -tlnp | grep 8080`
- [ ] Database connection successful (check logs)

---

## FRONTEND DEPLOYMENT

### Build
- [ ] Frontend code uploaded to server
- [ ] Dependencies installed: `npm install`
- [ ] Build successful: `npm run build`
- [ ] Dist folder created with files

### Deployment
- [ ] Web directory created: `/var/www/minet-sacco`
- [ ] Frontend files copied to web directory
- [ ] File permissions set: `755`
- [ ] Owner set to www-data

### Nginx Configuration
- [ ] Nginx config file created: `/etc/nginx/sites-available/minet-sacco`
- [ ] Config includes:
  - [ ] HTTP to HTTPS redirect
  - [ ] SSL certificate paths
  - [ ] Frontend location block
  - [ ] API proxy to backend
  - [ ] Security headers
  - [ ] Cache settings
- [ ] Config syntax valid: `sudo nginx -t`
- [ ] Site enabled: `sudo ln -s /etc/nginx/sites-available/minet-sacco /etc/nginx/sites-enabled/`
- [ ] Nginx restarted: `sudo systemctl restart nginx`

### SSL Certificate
- [ ] Certificate obtained (Let's Encrypt or purchased)
- [ ] Certificate path: `/etc/letsencrypt/live/yourdomain.com/`
- [ ] Certificate valid: `sudo certbot certificates`
- [ ] Auto-renewal enabled: `sudo systemctl enable certbot.timer`

---

## TESTING PHASE

### Backend Health Checks
- [ ] Health endpoint responds:
  ```bash
  curl https://yourdomain.com/api/auth/health
  ```
  **Response**: _______________

- [ ] No errors in backend logs:
  ```bash
  sudo journalctl -u minet-sacco-backend -n 50
  ```
  **Status**: ✓ No errors

### Frontend Checks
- [ ] Frontend loads: `https://yourdomain.com`
- [ ] Page displays correctly
- [ ] No 404 errors
- [ ] No console errors (F12 → Console)
- [ ] Logo and styling visible
- [ ] Responsive on mobile

### Authentication Tests
- [ ] Admin login successful
  - Username: `admin`
  - Password: `password`
  - **Result**: ✓ Logged in

- [ ] Dashboard loads after login
- [ ] User menu displays
- [ ] Logout works

### API Tests
- [ ] Get JWT token:
  ```bash
  curl -X POST https://yourdomain.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"password"}'
  ```
  **Token**: _______________

- [ ] Members endpoint works:
  ```bash
  curl -H "Authorization: Bearer TOKEN" \
    https://yourdomain.com/api/members
  ```
  **Result**: ✓ Returns member list

- [ ] Other endpoints tested:
  - [ ] GET /api/loans
  - [ ] GET /api/accounts
  - [ ] GET /api/users

### Database Tests
- [ ] Database connection verified
- [ ] Tables exist: `SHOW TABLES;`
- [ ] Data accessible: `SELECT COUNT(*) FROM members;`
- [ ] Count: _______________

### File Upload Tests
- [ ] Login as customer support user
- [ ] Navigate to KYC Document Upload
- [ ] Upload test document
- [ ] File saved to `/var/minet-sacco/uploads/kyc/`
- [ ] File accessible via API

### Critical Workflows
- [ ] Member login and dashboard
- [ ] Staff login and member management
- [ ] Loan application process
- [ ] Deposit/withdrawal
- [ ] KYC document upload
- [ ] Report generation
- [ ] User management

**All workflows tested**: ☐ Yes  ☐ No

### Performance Tests
- [ ] Page load time acceptable (< 3 seconds)
- [ ] API response time acceptable (< 1 second)
- [ ] No memory leaks in logs
- [ ] CPU usage normal

---

## POST-DEPLOYMENT

### Monitoring Setup
- [ ] Backend logs monitored: `sudo journalctl -u minet-sacco-backend -f`
- [ ] Nginx logs monitored: `sudo tail -f /var/log/nginx/error.log`
- [ ] Database monitored
- [ ] Disk space monitored: `df -h`

### Backup Configuration
- [ ] Database backup script created
- [ ] Backup scheduled (daily)
- [ ] Backup location: `/backups/`
- [ ] Test restore successful

### Documentation
- [ ] Deployment documented
- [ ] Configuration documented
- [ ] Credentials stored securely
- [ ] Team trained on monitoring
- [ ] Runbook updated

### Security
- [ ] Default passwords changed
- [ ] SSH key-based auth only
- [ ] Firewall configured
- [ ] SSL certificate valid
- [ ] Security headers enabled
- [ ] CORS configured correctly

---

## SIGN-OFF

### Deployment Verification
- [ ] All tests passed
- [ ] No critical errors
- [ ] Performance acceptable
- [ ] Security verified
- [ ] Backups working

### Team Approval
- [ ] Tech Lead approval: _______________
- [ ] QA approval: _______________
- [ ] DevOps approval: _______________

### Go-Live Decision
- [ ] Ready for production: ☐ Yes  ☐ No
- [ ] Rollback plan ready: ☐ Yes  ☐ No
- [ ] Team on standby: ☐ Yes  ☐ No

**Go-Live Time**: _______________

---

## ROLLBACK PROCEDURE (If Needed)

**Initiated By**: _______________  
**Time**: _______________  
**Reason**: _______________

### Rollback Steps
- [ ] Stop backend: `sudo systemctl stop minet-sacco-backend`
- [ ] Restore database: `mysql -u sacco_user -p sacco_db_prod < backup.sql`
- [ ] Restore backend JAR: `cp /backups/sacco-previous.jar target/`
- [ ] Restore frontend: `cp -r /backups/frontend/* /var/www/minet-sacco/`
- [ ] Start backend: `sudo systemctl start minet-sacco-backend`
- [ ] Verify services: `sudo systemctl status minet-sacco-backend`
- [ ] Test frontend: `https://yourdomain.com`

**Rollback Completed**: ☐ Yes  ☐ No  
**Verified By**: _______________

---

## NOTES & ISSUES

### Issues Encountered
1. _______________
   - Resolution: _______________
   - Time to resolve: _______________

2. _______________
   - Resolution: _______________
   - Time to resolve: _______________

### Lessons Learned
- _______________
- _______________
- _______________

### Follow-up Actions
- [ ] _______________
- [ ] _______________
- [ ] _______________

---

## FINAL SIGN-OFF

**Deployment Status**: ☐ Successful  ☐ Partial  ☐ Failed

**Deployed By**: _______________  
**Date**: _______________  
**Time**: _______________

**Verified By**: _______________  
**Date**: _______________  
**Time**: _______________

**Approved By**: _______________  
**Date**: _______________  
**Time**: _______________

---

## CONTACT INFORMATION

| Role | Name | Phone | Email |
|------|------|-------|-------|
| Tech Lead | | | |
| DevOps | | | |
| Database Admin | | | |
| Support Lead | | | |

---

**Keep this checklist for audit and reference purposes.**

