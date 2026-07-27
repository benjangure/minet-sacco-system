# Minet SACCO - Deployment Quick Reference

**Quick summary for deploying to production server**

---

## 3-Step Deployment Overview

### 1️⃣ DATABASE (MySQL)
```bash
# On server:
sudo apt install mysql-server -y
mysql -u root -p
CREATE DATABASE sacco_db_prod;
CREATE USER 'sacco_user'@'localhost' IDENTIFIED BY 'Password123!';
GRANT ALL PRIVILEGES ON sacco_db_prod.* TO 'sacco_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2️⃣ BACKEND (Java/Spring Boot)
```bash
# On server:
cd /opt/minet-sacco/minetsacco-main/backend

# Edit application.properties:
# - Change DB URL to: jdbc:mysql://localhost:3306/sacco_db_prod
# - Change DB user: sacco_user
# - Change DB password: Password123!
# - Change JWT secret to something strong
# - Update M-Pesa credentials

# Build:
mvn clean package -DskipTests

# Create systemd service and start:
sudo systemctl start minet-sacco-backend
sudo systemctl enable minet-sacco-backend
```

### 3️⃣ FRONTEND (React/Nginx)
```bash
# On your machine:
npm run build

# Upload to server:
scp -r dist/* user@server:/var/www/minet-sacco/

# Configure Nginx with SSL and restart:
sudo systemctl restart nginx
```

---

## Testing Checklist

| Test | Command | Expected Result |
|------|---------|-----------------|
| Backend Health | `curl https://yourdomain.com/api/auth/health` | `{"success":true}` |
| Frontend Load | Open `https://yourdomain.com` in browser | Page loads, no errors |
| Login | Use admin/password | Dashboard displays |
| Database | `mysql -u sacco_user -p sacco_db_prod` | Connects successfully |
| File Upload | Upload KYC doc as customer support | File saved to `/var/minet-sacco/uploads/kyc/` |

---

## Key Configuration Files

| Component | File | Key Changes |
|-----------|------|-------------|
| Backend | `backend/src/main/resources/application.properties` | DB credentials, JWT secret, M-Pesa keys |
| Frontend | `minetsacco-main/src/config/api.ts` | Backend URL (should auto-detect) |
| Nginx | `/etc/nginx/sites-available/minet-sacco` | Domain name, SSL paths |

---

## Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| Backend won't start | Check logs: `sudo journalctl -u minet-sacco-backend -f` |
| 404 on frontend | Verify files in `/var/www/minet-sacco/` |
| Database connection error | Check credentials in `application.properties` |
| SSL certificate error | Run: `sudo certbot renew --force-renewal` |
| Port 8080 in use | Change port in `application.properties` |

---

## File Locations on Server

```
/opt/minet-sacco/              # Application code
/var/www/minet-sacco/          # Frontend files
/var/minet-sacco/uploads/      # Uploaded documents
/var/log/nginx/                # Nginx logs
/etc/nginx/sites-available/    # Nginx config
/etc/systemd/system/           # Service files
/backups/                       # Database backups
```

---

## Monitoring Commands

```bash
# Check backend status
sudo systemctl status minet-sacco-backend

# View backend logs (last 50 lines)
sudo journalctl -u minet-sacco-backend -n 50

# Check database
mysql -u sacco_user -p sacco_db_prod -e "SELECT COUNT(*) FROM members;"

# Check disk space
df -h

# Check Nginx status
sudo systemctl status nginx

# View Nginx errors
sudo tail -50 /var/log/nginx/error.log
```

---

## Backup & Restore

```bash
# Backup database
mysqldump -u sacco_user -p sacco_db_prod > backup_$(date +%Y%m%d).sql

# Restore database
mysql -u sacco_user -p sacco_db_prod < backup_20260520.sql

# Backup frontend
tar -czf frontend_backup_$(date +%Y%m%d).tar.gz /var/www/minet-sacco/

# Backup backend JAR
cp /opt/minet-sacco/minetsacco-main/backend/target/sacco-*.jar /backups/
```

---

## Environment Variables (Recommended)

Create `/opt/minet-sacco/.env`:

```bash
DB_URL=jdbc:mysql://localhost:3306/sacco_db_prod
DB_USER=sacco_user
DB_PASSWORD=Password123!
JWT_SECRET=GenerateStrongRandomSecret256Bits
MPESA_KEY=your-production-key
MPESA_SECRET=your-production-secret
SENDGRID_API_KEY=your-sendgrid-key
```

---

## Pre-Deployment Checklist

- [ ] Server prepared (Java 21, MySQL 8, Node.js 18+)
- [ ] Domain name configured
- [ ] SSL certificate ready (Let's Encrypt)
- [ ] Database backup taken
- [ ] All credentials documented
- [ ] Team trained on deployment
- [ ] Rollback plan documented
- [ ] Monitoring configured

---

## Post-Deployment Checklist

- [ ] Backend service running
- [ ] Frontend accessible
- [ ] Login working
- [ ] Database connected
- [ ] File uploads working
- [ ] All critical workflows tested
- [ ] Backups configured
- [ ] Monitoring enabled
- [ ] Team notified

---

## Emergency Contacts

| Role | Contact | Phone |
|------|---------|-------|
| Tech Lead | - | - |
| Database Admin | - | - |
| DevOps | - | - |
| Support | - | - |

---

## Deployment Timeline

| Phase | Duration | Owner |
|-------|----------|-------|
| Database Setup | 30 min | DevOps |
| Backend Deploy | 20 min | DevOps |
| Frontend Deploy | 15 min | DevOps |
| Testing | 30 min | QA |
| Go-Live | 5 min | Tech Lead |
| **Total** | **~2 hours** | - |

---

## Success Criteria

✅ All services running  
✅ Health checks passing  
✅ Login successful  
✅ Database connected  
✅ File uploads working  
✅ No console errors  
✅ Backups configured  
✅ Team trained  

---

**For detailed instructions, see: PRODUCTION_DEPLOYMENT_GUIDE.md**

