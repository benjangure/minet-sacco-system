# Render Deployment Build Fix

## Problem Encountered
```
Error: JAVA_HOME is not defined correctly.
We cannot execute
```

## Root Causes
1. **Render auto-detected Node.js** instead of Java environment
2. **Missing Java version specification** in configuration
3. **render.yaml was in wrong location** (backend/ instead of root)
4. **Maven wrapper permissions** not preserved in git

## Solution Applied

### 1. Fixed render.yaml Location
- **Moved** from `backend/render.yaml` → `render.yaml` (repository root)
- Render looks for configuration at the root level

### 2. Specified Java Environment
```yaml
env: java              # Forces Java runtime (not Node.js)
JAVA_VERSION: "17"     # Explicit Java 17
```

### 3. Added system.properties
Created `backend/system.properties`:
```properties
java.runtime.version=17
```

### 4. Enhanced Build Command
```bash
chmod +x mvnw && ./mvnw clean package -DskipTests
```
- Ensures mvnw is executable (backup safety)
- Skips tests for faster builds

### 5. Improved Start Command
```bash
java -Dserver.port=$PORT \
     -Dspring.profiles.active=production \
     -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

## Configuration Methods

### Option A: Use render.yaml (Recommended)
The `render.yaml` at repository root will automatically configure everything.

**Pros:**
- Infrastructure as code
- Version controlled
- Automatic setup

**Cons:**
- Less flexible for manual tweaks

### Option B: Manual Dashboard Configuration
If you prefer manual setup, use these settings in Render dashboard:

**Build Settings:**
- **Environment:** Java
- **Build Command:** `cd backend && chmod +x mvnw && ./mvnw clean package -DskipTests`
- **Start Command:** `cd backend && java -Dserver.port=$PORT -Dspring.profiles.active=production -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar`

**Environment Variables:**
```
JAVA_VERSION=17
SPRING_PROFILES_ACTIVE=production
JAVA_TOOL_OPTIONS=-Xmx512m -Xms256m
```

Plus all the variables from `RENDER_PASTE_THIS.env`

## Testing the Fix

### Before Pushing to GitHub

1. **Verify mvnw is executable:**
```bash
cd backend
ls -l mvnw
# Should show: -rwxr-xr-x (100755)
```

2. **Test local build:**
```bash
cd backend
./mvnw clean package -DskipTests
```

3. **Verify JAR exists:**
```bash
ls -lh target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

### After Pushing to GitHub

1. **Check Render Build Logs** for:
```
✓ Detected Java environment
✓ Java version: 17.x.x
✓ JAVA_HOME is set
✓ mvnw clean package -DskipTests
✓ BUILD SUCCESS
✓ JAR file created
```

2. **Test Health Endpoint:**
```bash
curl https://minetsacco-backend.onrender.com/api/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2026-08-12T10:52:40.714Z"
}
```

## Render Build Process

Render will execute these steps:

1. **Clone repository** from GitHub
2. **Detect environment** → Java (from render.yaml)
3. **Install Java 17** (specified in JAVA_VERSION)
4. **Set JAVA_HOME** automatically
5. **Navigate to rootDir** (`backend/`)
6. **Run buildCommand:**
   ```bash
   chmod +x mvnw && ./mvnw clean package -DskipTests
   ```
7. **Create container** with built JAR
8. **Run startCommand** on assigned PORT

## Common Issues & Solutions

### Issue: "Permission denied" on mvnw
**Solution:** Build command includes `chmod +x mvnw` as safety

### Issue: "JAVA_HOME not defined"
**Solution:** Added `JAVA_VERSION=17` env var + `env: java` in render.yaml

### Issue: "No such file or directory"
**Solution:** Moved render.yaml to repository root + added `rootDir: backend`

### Issue: Node.js detected instead of Java
**Solution:** Explicit `env: java` in render.yaml

### Issue: Build succeeds but app won't start
**Solution:** Check environment variables are set (use RENDER_PASTE_THIS.env)

## Verification Checklist

Before declaring success:

- [ ] Build completes without errors
- [ ] JAR file is created (check logs)
- [ ] Service starts successfully
- [ ] Health endpoint responds: `/api/health`
- [ ] No crash loops in logs
- [ ] Database connection works
- [ ] API endpoints accessible

## Next Steps After Successful Deploy

1. **Test all endpoints:**
   - GET `/api/health` → 200 OK
   - POST `/api/auth/login` → Authentication works
   - GET `/api/members` → Database query works

2. **Monitor logs** for any runtime errors

3. **Update mobile app** with Render URL (already done)

4. **Build Android APK** with production backend

5. **Delete sensitive files:**
   - `C:\Users\Lenovo\Desktop\minet-sacco\RENDER_PASTE_THIS.env`
   - `C:\Users\Lenovo\Desktop\minet-sacco\RENDER_ENV_VARIABLES_ACTUAL.txt`

## Support

If build still fails:
1. Check Render build logs for exact error
2. Verify all environment variables are set
3. Test mvnw locally: `cd backend && ./mvnw clean package`
4. Check Java version matches: Java 17

---

**Last Updated:** August 12, 2026
**Status:** Ready to deploy
