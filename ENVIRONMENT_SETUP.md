# Environment Setup Guide

## Current Configuration: LOCAL DEVELOPMENT

### Backend (Running on port 9090)
- **Profile**: `dev`
- **Database**: `tminet` (localhost)
- **User**: `tminet`
- **Password**: `0a0b0c0D.`
- **Status**: ✅ Running

**Start Backend:**
```powershell
cd backend
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Frontend (Port 5173)
- **API URL**: `http://localhost:9090/api`
- **Backend URL**: `http://localhost:9090`
- **Environment**: Development

**Start Frontend:**
```powershell
cd minetsacco-main
npm run dev
```

**Important:** Restart the frontend after changing `.env` files!

---

## Switching to Production

### Backend Production Profile
```powershell
cd backend
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

This connects to:
- Database: `minetsacco`
- Server: `localhost:3306` or production server

### Frontend Production Config
Edit `minetsacco-main/.env`:
```env
VITE_API_URL="http://10.39.60.15:9090/api"
VITE_NATIVE_BACKEND_URL="http://10.39.60.15:9090"
```

Or copy from backup:
```powershell
cd minetsacco-main
Copy-Item .env.production.backup .env -Force
```

---

## Environment Files Reference

### Development (Current)
- `backend/src/main/resources/application-dev.properties` → tminet database
- `minetsacco-main/.env` → localhost:9090
- `minetsacco-main/.env.development` → localhost:9090

### Production
- `backend/src/main/resources/application-prod.properties` → minetsacco database
- `minetsacco-main/.env.production` → 10.39.60.15:9090
- `minetsacco-main/.env.production.backup` → Production backup

---

## Quick Commands

### Start Development Environment
```powershell
# Terminal 1 - Backend
cd backend
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# Terminal 2 - Frontend
cd minetsacco-main
npm run dev
```

### Test Backend
```powershell
curl http://localhost:9090/api/auth/health
```

### Test Frontend
Open browser: `http://localhost:5173`

---

## Database Info

### Development Database (tminet)
- 42 tables imported from production dump
- Full production data for testing
- Safe to modify without affecting production

### Production Database (minetsacco)
- Live production data
- Connected via prod profile
- Use with caution

---

## Troubleshooting

### Frontend shows "Connection Refused"
1. Check backend is running: `curl http://localhost:9090/api/auth/health`
2. Verify `.env` has correct URL: `VITE_API_URL="http://localhost:9090/api"`
3. Restart frontend: Stop dev server and run `npm run dev` again

### Backend won't start
1. Check MySQL is running
2. Verify tminet user exists: `mysql -u tminet -p0a0b0c0D. tminet`
3. Check correct profile: `-Dspring-boot.run.profiles=dev`

### Frontend connects to wrong backend
1. Check `.env` file in `minetsacco-main/`
2. Restart frontend dev server (must restart after .env changes!)
3. Clear browser cache if needed
