# Render Deployment with Docker - Complete Guide

## Why Docker?

Render's free tier doesn't have "Java" as a language option in the UI. The available languages are:
- Docker ✅ **(Use this for Java/Spring Boot)**
- Node
- Python 3
- Ruby
- Elixir
- Go
- Rust

## Solution: Dockerize the Backend

We've created a **multi-stage Dockerfile** that:
1. **Builds** the Spring Boot JAR using Maven (build stage)
2. **Runs** the JAR in a lightweight JRE container (runtime stage)

This approach:
- ✅ Works on Render's free tier
- ✅ Smaller final image (~200MB vs 600MB+)
- ✅ Faster deployments
- ✅ No JAVA_HOME issues

## Files Created

### 1. `backend/Dockerfile`
Multi-stage Docker build:
- **Stage 1:** Build with Maven + JDK 17
- **Stage 2:** Run with JRE 17 (smaller)

### 2. `backend/.dockerignore`
Excludes unnecessary files from Docker build

### 3. `render.yaml` (Updated)
Uses Docker environment instead of Java

## Render Setup Instructions

### Option A: Using render.yaml (Recommended)

The `render.yaml` at repository root will auto-configure everything.

**Just push to GitHub and Render will:**
1. Detect the Dockerfile
2. Build the Docker image
3. Deploy automatically

### Option B: Manual Dashboard Setup

If you prefer manual configuration:

#### Step 1: Delete Current Service
Since you created it with "Node" language, you need to recreate it:
1. Go to your service: `minetsacco-backend`
2. Click **Settings** → **Delete Service**
3. Confirm deletion

#### Step 2: Create New Docker Service
1. Click **+ New** → **Web Service**
2. Connect your repository: `hachizeus/minet-sacco-system`
3. Configure:

**Basic Settings:**
- **Name:** `minetsacco-backend`
- **Region:** Oregon (same as your database)
- **Branch:** `main`

**Build Settings:**
- **Language:** Select **"Docker"** ⬅️ **IMPORTANT**
- **Dockerfile Path:** `./backend/Dockerfile`
- **Docker Context:** `./backend`
- **Docker Build Context:** `./backend` (or leave empty)

**Instance Settings:**
- **Plan:** Free

**Advanced Settings:**
- **Health Check Path:** `/api/health`
- **Auto-Deploy:** Yes

#### Step 3: Add Environment Variables

Click **Environment** tab and add all variables from `RENDER_PASTE_THIS.env`:

```bash
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-d9u3rcbncjis73af26j0-a.oregon-postgres.render.com/minetsacco
SPRING_DATASOURCE_USERNAME=minetsacco
SPRING_DATASOURCE_PASSWORD=5WNIl6Ko7KCLmWkavtYeayPS69H3TxbM
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
JWT_SECRET=DOXkSd25jt1AuTN6ECieIU0oFpmcRxVHQhY7z3WnKasbrMwJZ84BLqGfvy9lPg
JWT_EXPIRATION_MS=86400000
SPRING_MAIL_HOST=smtp.office365.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=no_reply@minet.co.ke
SPRING_MAIL_PASSWORD=fhcyvypyydghmyfp
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

#### Step 4: Deploy
Click **Create Web Service** and wait for build.

## What Happens During Deployment

### Build Process (5-10 minutes first time)
```
1. Clone repository from GitHub
2. Navigate to backend/
3. Detect Dockerfile
4. Build Stage 1: Maven build
   - Download dependencies (~2 minutes)
   - Compile and package JAR (~3 minutes)
5. Build Stage 2: Create runtime image
   - Copy JAR to lightweight JRE image (~1 minute)
6. Push Docker image to Render's registry
```

### Runtime Process
```
1. Start container from built image
2. Expose PORT (Render provides this)
3. Run: java -Dserver.port=$PORT -jar app.jar
4. Spring Boot starts (~30 seconds)
5. Health check: /api/health
6. Service becomes live ✅
```

## Expected Build Logs

```
==> Building image from Dockerfile...
Step 1/11 : FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
 ---> Pulling from library/maven
Step 2/11 : WORKDIR /app
 ---> Running in abc123
Step 3/11 : COPY pom.xml .
 ---> 456def
Step 4/11 : RUN mvn dependency:go-offline -B
 ---> Running in 789ghi
[INFO] Downloading dependencies...
[INFO] Downloaded successfully
Step 5/11 : COPY src ./src
 ---> abc456
Step 6/11 : RUN mvn clean package -DskipTests
 ---> Running in def789
[INFO] Building JAR...
[INFO] BUILD SUCCESS
Step 7/11 : FROM eclipse-temurin:17-jre-alpine
 ---> Pulling from library/eclipse-temurin
Step 8/11 : WORKDIR /app
Step 9/11 : COPY --from=build /app/target/*.jar app.jar
Step 10/11 : EXPOSE 8080
Step 11/11 : CMD ["sh", "-c", "java -jar app.jar"]
==> Successfully built image
==> Deploying...
==> Your service is live 🎉
```

## Troubleshooting

### Issue: "Error building image"
**Check:** Docker build logs for specific Maven/compilation errors

### Issue: "Container failed to start"
**Check:** 
1. Environment variables are set correctly
2. Database connection string is correct
3. Application logs for errors

### Issue: "Health check failing"
**Check:**
1. `/api/health` endpoint exists (we created HealthController)
2. Application actually started (check logs)
3. PORT is correctly configured

### Issue: "Out of memory"
**Solution:** The free tier has 512MB RAM. Our Dockerfile sets:
```
-Xmx512m -Xms256m
```
This should fit. If issues persist, consider Starter plan ($7/month, 1GB RAM).

## Testing Locally (Optional)

To test the Docker build locally before pushing:

```powershell
# Build the image
cd minet-sacco-system/backend
docker build -t minetsacco-backend .

# Run the container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://... \
  minetsacco-backend

# Test health endpoint
curl http://localhost:8080/api/health
```

## Commit and Deploy

Now let's commit all Docker files and push:

```powershell
cd minet-sacco-system
git add .
git commit -m "Add Docker support for Render deployment"
git push my-github main
```

Render will automatically:
1. Detect the new commit
2. Build the Docker image
3. Deploy the container
4. Run health checks
5. Make service live

## Monitoring

After deployment:
1. **View Logs:** Render Dashboard → Your Service → Logs
2. **Check Metrics:** CPU, Memory, Request count
3. **Test Endpoints:**
   - `https://minetsacco-backend.onrender.com/api/health`
   - `https://minetsacco-backend.onrender.com/api/ping`

## Next Steps After Successful Deployment

1. ✅ Test health endpoint
2. ✅ Test authentication: POST `/api/auth/login`
3. ✅ Test database queries: GET `/api/members`
4. ✅ Update mobile app (already configured)
5. ✅ Build Android APK
6. ✅ Delete sensitive credential files

---

**Status:** Ready to deploy with Docker
**Last Updated:** August 12, 2026
