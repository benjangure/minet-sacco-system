# Quick Start Guide

## 🚀 Local Development (5 Minutes)

### Step 1: Setup Database (1 minute)
```powershell
mysql -u root -p
```
```sql
CREATE DATABASE tminet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';
GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Step 2: Start Backend (2 minutes)
```powershell
cd backend
./mvnw.cmd spring-boot:run
```
✅ Backend runs at: `http://localhost:9090`

### Step 3: Start Frontend (2 minutes)
```powershell
cd minetsacco-main
npm run dev
```
✅ Frontend runs at: `http://localhost:5173`

**That's it! Open your browser to http://localhost:5173**

---

## 📦 Production Deployment

### Build
```powershell
# Backend
cd backend
./mvnw.cmd clean package -DskipTests

# Frontend
cd minetsacco-main
npm run build
```

### Deploy
```powershell
# Copy to server
Copy-Item backend\target\*.jar \\10.39.60.15\C$\minetsacco-deploy\backend\
Copy-Item minetsacco-main\dist\* \\10.39.60.15\C$\inetpub\minetsacco\ -Recurse
```

### Run on Server
```powershell
java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 🎯 Key Features

### Treasurer Can:
- ✅ Edit ALL loan fields (principal, interest, term, etc.)
- ✅ Reset loans to 0 (start from scratch)
- ✅ Delete ANY loan (with automatic accounting reversals)
- ✅ Maintain 100% system accuracy

### Environments:
- **dev** → tminet database (local testing)
- **prod** → minetsacco database (production server)

---

## 📚 Full Documentation

| Guide | Purpose |
|-------|---------|
| **LOCAL_DEVELOPMENT_GUIDE.md** | Complete local setup |
| **TREASURER_LOAN_MANAGEMENT.md** | Treasurer features guide |
| **DEPLOYMENT_GUIDE.md** | Production deployment |
| **ENHANCED_FEATURES_SUMMARY.md** | Everything that was done |

---

## 🆘 Quick Troubleshooting

**MySQL Error?**
```sql
-- Recreate user
DROP USER IF EXISTS 'tminet'@'localhost';
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';
GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';
FLUSH PRIVILEGES;
```

**Port 9090 in use?**
```powershell
netstat -ano | findstr :9090
taskkill /PID <pid> /F
```

**Frontend can't connect?**
- Check backend is running: `curl http://localhost:9090/api/members`
- Check browser console (F12) for errors

---

## 📞 Ports

| Service | Development | Production |
|---------|-------------|------------|
| Backend | localhost:9090 | 10.39.60.15:9090 |
| Frontend | localhost:5173 | 10.39.60.15:8090 |

---

**Need help? Check the detailed guides!** 📖
