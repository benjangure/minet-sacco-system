# Quick Start Guide - Local Development

## For Developers Taking Over This Project

This guide will help you set up the Minet SACCO system on your local machine for development.

---

## 📋 Prerequisites Installed

✅ Java 17+ (You have Java 17)
✅ MySQL 8.0 (You have MySQL80 service running)
✅ Node.js 18+ (For frontend)
✅ Maven wrapper (Already included in project)

---

## 🚀 Quick Setup (5 Minutes)

### Step 1: Set MySQL Password

**Option A: If you know your MySQL password**
Skip to Step 2.

**Option B: If you don't know your MySQL password**

Run PowerShell **as Administrator**:

```powershell
# Stop MySQL
net stop MySQL80

# Start MySQL in safe mode (leave this window open)
mysqld --skip-grant-tables --skip-networking
```

Open **another** PowerShell window:

```powershell
mysql -u root

# In MySQL prompt:
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'admin123';
EXIT;
```

Close the safe mode window (Ctrl+C) and restart MySQL:

```powershell
net start MySQL80
```

---

### Step 2: Run Database Setup Script

```powershell
cd backend
.\setup-database.ps1
# Enter your MySQL password when prompted
```

This script will:
- Test MySQL connection
- Create `sacco_db` database
- Update `application.properties` with your password

---

### Step 3: Start Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Wait for: `Started MinetSaccoBackendApplication`

**Backend running at:** http://localhost:8080
**API Docs:** http://localhost:8080/swagger-ui/index.html

---

### Step 4: Start Frontend (Optional)

Open a **new** terminal:

```powershell
cd minetsacco-main
npm install
npm run dev
```

**Frontend running at:** http://localhost:5173

---

## 🧪 Test the System

### Test Backend API

**Login (get JWT token):**
```powershell
curl -X POST http://localhost:9090/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"admin\",\"password\":\"admin123\"}'
```

Copy the token from the response.

**Get members:**
```powershell
curl -X GET http://localhost:9090/api/members `
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 🎯 Default Test Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| treasurer | admin123 | TREASURER |
| loan_officer | admin123 | LOAN_OFFICER |

---

## 📊 Database Schema

The system uses **Flyway migrations** to create all database tables automatically.

**73 migration files** in: `backend/src/main/resources/db/migration/`

Tables created include:
- `users` - Staff accounts
- `members` - SACCO members  
- `accounts` - Member savings/shares accounts
- `loans` - Loan records
- `transactions` - Financial transactions
- `audit_logs` - System audit trail
- And many more...

---

## 🔧 Development Workflow

### 1. Make Code Changes

Edit files in:
- **Backend:** `backend/src/main/java/com/minet/sacco/`
- **Frontend:** `minetsacco-main/src/`

### 2. Test Locally

- Backend auto-reloads on save (Spring Boot DevTools)
- Frontend hot-reloads (Vite)

### 3. Database Changes

If you need to modify the database schema:

**Create new migration file:**
```
backend/src/main/resources/db/migration/V74__Your_change_description.sql
```

Example:
```sql
-- V74__Add_email_verified_column.sql
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;
```

Restart backend - Flyway will apply the migration automatically.

### 4. Commit Changes

```bash
git add .
git commit -m "Description of changes"
git push origin your-branch-name
```

---

## 🐛 Troubleshooting

### Backend won't start

**Problem:** "Access denied for user 'root'@'localhost'"
**Solution:** Update password in `application.properties`

**Problem:** "Port 8080 already in use"
**Solution:** Stop other Java processes or change port in `application.properties`

**Problem:** "Flyway migration failed"
**Solution:** 
```sql
-- Connect to MySQL
mysql -u root -p

-- Drop and recreate database
DROP DATABASE sacco_db;
CREATE DATABASE sacco_db;
EXIT;
```

Then restart backend.

### Frontend won't start

**Problem:** "npm install fails"
**Solution:** 
```powershell
npm cache clean --force
rm -r node_modules
npm install
```

**Problem:** "Cannot connect to backend"
**Solution:** Verify backend is running on http://localhost:8080

---

## 📁 Project Structure

```
minet-sacco-system/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/         # Java source code
│   ├── src/main/resources/    # Config & migrations
│   ├── pom.xml                # Maven dependencies
│   └── mvnw.cmd               # Maven wrapper
│
├── minetsacco-main/           # React frontend
│   ├── src/                   # React components
│   ├── package.json           # npm dependencies
│   └── vite.config.ts         # Vite configuration
│
└── Documentation files
```

---

## 🚢 Deploying to Production

### 1. Test Everything Locally

```powershell
# Run tests
cd backend
.\mvnw.cmd test

# Build production JAR
.\mvnw.cmd clean package
```

### 2. Build Frontend

```powershell
cd minetsacco-main
npm run build
```

### 3. Deploy to Server

- Copy `backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar` to server
- Copy `minetsacco-main/dist/` to web server
- Update production `application.properties` with production database credentials
- Run: `java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar`

---

## 📞 Need Help?

1. Check logs in `backend/logs/` (if configured)
2. Check console output for errors
3. Review `SYSTEM_OVERVIEW.md` for architecture details
4. Review `PROJECT_STRUCTURE_GUIDE.md` for file locations

---

## ✅ Summary Checklist

- [ ] MySQL password set
- [ ] Database `sacco_db` created
- [ ] `application.properties` updated with password
- [ ] Backend running on port 8080
- [ ] Can login with admin/admin123
- [ ] Frontend running on port 5173 (optional)
- [ ] Can access Swagger UI

**You're ready to start development!** 🎉

