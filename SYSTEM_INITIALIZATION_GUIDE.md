# Minet SACCO - System Initialization & Maintenance Guide

## Quick Start

### Prerequisites
- Java 25+
- Node.js 18+
- MySQL 5.7+
- Android SDK (for APK building)
- Git

### Initial Setup (First Time)

```bash
# 1. Clone repository
git clone <repo-url>
cd minetsacco-main

# 2. Backend setup
cd backend
mvn clean install
mvn spring-boot:run

# 3. Frontend setup (new terminal)
cd minetsacco-main
npm install
npm run dev

# 4. Access
- Staff Portal: http://localhost:3000
- Member Portal: http://localhost:3000/member
- API Docs: http://localhost:8080/swagger-ui.html
```

---

## Database Initialization

### First Time Setup
```sql
-- Database is auto-created by Flyway migrations
-- Just ensure MySQL is running and accessible
-- Check application.properties for connection details
```

### Default Admin User
```
Username: admin
Password: admin123
Role: ADMIN
```

---

## Building & Deployment

### Backend Build
```bash
cd backend
mvn clean package -DskipTests
# Output: target/minetsacco-backend-1.0.0.jar
```

### Frontend Build
```bash
cd minetsacco-main
npm run build
# Output: dist/ folder
```

### APK Build (Android)
```bash
cd minetsacco-main
npm run build
npx cap sync android
cd android
./gradlew.bat assembleDebug
# Output: android/app/build/outputs/apk/debug/app-debug.apk
```

### APK Installation
```bash
# Connect Android device with USB debugging enabled
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Development Workflow

### Making Code Changes

**Backend Changes:**
```bash
# 1. Make changes in backend/src/
# 2. Rebuild
mvn clean package -DskipTests
# 3. Restart backend (Ctrl+C, then mvn spring-boot:run)
```

**Frontend Changes:**
```bash
# 1. Make changes in minetsacco-main/src/
# 2. Dev server auto-reloads (npm run dev)
# 3. For production build: npm run build
```

**APK Changes:**
```bash
# 1. Make frontend changes
# 2. npm run build
# 3. npx cap sync android
# 4. cd android && ./gradlew.bat assembleDebug
# 5. adb install -r app-debug.apk
```

### Database Migrations
```bash
# New migrations go in: backend/src/main/resources/db/migration/
# Format: V{number}__{description}.sql
# Example: V63__Add_new_field.sql

# Flyway auto-runs on startup
# Check logs for migration status
```

---

## Testing Data Management

### Scenario 1: Clean Member Data (Keep Staff)
```bash
# Use: backend/CLEAN_MEMBER_DATA_SMART.sql
# Deletes: All members, accounts, loans, transactions, KYC docs, guarantors, bulk batches
# Keeps: Staff users, loan products, fund config, eligibility rules
# Use Case: E2E testing with fresh member data
```

### Scenario 2: Full System Reset (Keep Admin Only)
```bash
# Use: backend/SETUP_AND_CLEAN.sql
# Deletes: Everything except admin user
# Keeps: Admin user only
# Use Case: Complete system reset for fresh testing
```

### Scenario 3: Inspect Database
```bash
# Use: backend/INSPECT_DATABASE.sql
# Shows: All tables and row counts
# Use Case: Verify data state before cleanup
```

---

## SQL Scripts Reference

### Location
All scripts are in `backend/` directory

### Available Scripts

| Script | Purpose | Keeps |
|--------|---------|-------|
| `INSPECT_DATABASE.sql` | View all tables and counts | N/A |
| `CLEAN_MEMBER_DATA_SMART.sql` | Remove all member data | Staff users, system config |
| `SETUP_AND_CLEAN.sql` | Full reset | Admin user only |
| `INSPECT_TABLES.sql` | Detailed table structure | N/A |

### How to Run
```bash
# Option 1: MySQL CLI
mysql -u root -p sacco_db < backend/CLEAN_MEMBER_DATA_SMART.sql

# Option 2: MySQL Workbench
# Open script file and execute

# Option 3: DBeaver
# Right-click database → Execute SQL Script
```

---

## Common Tasks

### Add New Staff User
```bash
# Via UI: Staff Portal → User Management → Create User
# Or via SQL:
INSERT INTO user (username, password_hash, role, member_id, created_at)
VALUES ('username', 'hashed_password', 'TELLER', NULL, NOW());
```

### Reset Member Password
```bash
# Via UI: Not available (members use their own credentials)
# Via SQL:
UPDATE user SET password_hash = 'new_hash' WHERE username = 'member_username';
```

### View Audit Trail
```bash
# Via UI: Staff Portal → Audit Trail
# Or via SQL:
SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 100;
```

### Check Loan Status
```bash
# Via UI: Staff Portal → Loans → View Details
# Or via SQL:
SELECT id, loan_number, status, amount, outstanding_balance 
FROM loans 
WHERE member_id = ? 
ORDER BY created_at DESC;
```

---

## Troubleshooting

### Backend Won't Start
```
Error: Port 8080 already in use
Solution: Kill process on port 8080 or change port in application.properties
```

### Frontend Build Fails
```
Error: npm ERR! code ERESOLVE
Solution: npm install --legacy-peer-deps
```

### APK Installation Fails
```
Error: INSTALL_FAILED_VERSION_DOWNGRADE
Solution: Uninstall old APK first: adb uninstall com.minet.sacco
```

### Database Connection Error
```
Error: Communications link failure
Solution: Verify MySQL is running and credentials in application.properties are correct
```

### Loan Eligibility Shows 0
```
Cause: Member has no savings account
Solution: Create savings account via Staff Portal → Member Management
```

---

## Performance Tuning

### Database Optimization
```sql
-- Add indexes for frequently queried fields
CREATE INDEX idx_member_id ON loans(member_id);
CREATE INDEX idx_account_member ON accounts(member_id);
CREATE INDEX idx_transaction_date ON transactions(transaction_date);
```

### Backend Optimization
- Increase heap size: `export JAVA_OPTS="-Xmx2g"`
- Enable query caching in application.properties
- Use connection pooling (already configured)

### Frontend Optimization
- Run `npm run build` for production
- Use CDN for static assets
- Enable gzip compression in web server

---

## Backup & Recovery

### Backup Database
```bash
mysqldump -u root -p sacco_db > backup_$(date +%Y%m%d).sql
```

### Restore Database
```bash
mysql -u root -p sacco_db < backup_20260402.sql
```

### Backup Application Files
```bash
# Backend
tar -czf backend_backup.tar.gz backend/

# Frontend
tar -czf frontend_backup.tar.gz minetsacco-main/
```

---

## Deployment Checklist

- [ ] Backend builds without errors
- [ ] Frontend builds without errors
- [ ] Database migrations run successfully
- [ ] Admin user can login
- [ ] Staff users can access portal
- [ ] Members can access mobile app
- [ ] Loan eligibility calculates correctly
- [ ] Repayments process successfully
- [ ] Reports generate without errors
- [ ] Audit trail records all actions
- [ ] All tests pass

---

## Support & Documentation

- **API Documentation:** http://localhost:8080/swagger-ui.html
- **System Overview:** See SYSTEM_OVERVIEW.md
- **Usage Guide:** See USAGE_GUIDE.md
- **Staff Roles:** See STAFF_ROLES_HIERARCHY.md

---

**Last Updated:** April 2, 2026  
**Version:** 1.0.0
