# Minet SACCO Management System

[![Deployment](https://img.shields.io/badge/deployment-render-46E3B7)](https://minetsacco-backend.onrender.com)
[![License](https://img.shields.io/badge/license-Proprietary-red)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS%20%7C%20Web-blue)](https://github.com)

A comprehensive SACCO (Savings and Credit Cooperative) management platform built with Spring Boot and React, featuring member portals, loan management, and financial tracking.

## 🌟 Features
.\mvnw.cmd spring-boot:run
### Member Portal
- 📊 Real-time dashboard with account summaries
- 💰 Loan application and tracking
- 📈 Transaction history and statements
- 👤 Profile management
- 🔔 Push notifications
- 📱 Mobile apps (Android/iOS)

### Staff Portal
- 👥 Member management
- 💵 Loan approval and disbursement
- 📋 Transaction processing
- 📊 Financial reporting
- 🔐 User access control
- 📈 Analytics and insights

### Core Features
- ✅ Multi-account support (Savings, Shares)
- ✅ Flexible loan products with guarantors
- ✅ Automated interest calculations
- ✅ M-Pesa integration (Daraja API)
- ✅ Excel bulk uploads
- ✅ PDF report generation
- ✅ Real-time updates via WebSockets
- ✅ Mobile-first responsive design

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Web App    │  │  Android APK │  │   iOS App    │ │
│  │  (React/TS)  │  │  (Capacitor) │  │ (Capacitor)  │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────┘
                           │
                    HTTPS/REST API
                           │
┌─────────────────────────────────────────────────────────┐
│                    Backend Layer                         │
│              Spring Boot 3.2 (Java 17)                   │
│                                                           │
│  ├─ REST Controllers (JWT Authentication)                │
│  ├─ Service Layer (Business Logic)                       │
│  ├─ JPA Repositories (Data Access)                       │
│  ├─ Flyway Migrations (Schema Management)                │
│  └─ Security (Spring Security + JWT)                     │
└─────────────────────────────────────────────────────────┘
                           │
                    JDBC Connection
                           │
┌─────────────────────────────────────────────────────────┐
│                   Database Layer                         │
│              PostgreSQL 16 (Render)                      │
│                                                           │
│  ├─ Members & Accounts                                   │
│  ├─ Loans & Guarantors                                   │
│  ├─ Transactions                                          │
│  └─ Financial Records                                     │
└─────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (OpenJDK recommended)
- **Node.js 18+** and npm
- **PostgreSQL 14+** (or use Render database)
- **Git**

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/minet-sacco.git
   cd minet-sacco/backend
   ```

2. **Configure database**
   
   Copy `.env.example` to `.env` and update:
   ```properties
   SPRING_PROFILES_ACTIVE=production
   DATABASE_URL=postgresql://user:password@host:port/database
   JWT_SECRET=your-64-character-secret-key
   ```

3. **Build and run**
   ```bash
   ./mvnw clean package
   java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
   ```

   Backend will start on `http://localhost:9090`

### Frontend Setup

1. **Navigate to frontend**
   ```bash
   cd ../minetsacco-main
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment**
   
   Create `.env.local`:
   ```env
   VITE_API_URL=http://localhost:9090
   ```

4. **Start development server**
   ```bash
   npm run dev
   ```

   Frontend will start on `http://localhost:5173`

---

## 🌐 Deployment

### Deploy to Render (Recommended)

The application is configured for deployment on [Render](https://render.com).

#### Backend Deployment

1. **Push to GitHub**
   ```bash
   git add .
   git commit -m "Deploy to Render"
   git push origin main
   ```

2. **Create PostgreSQL Database on Render**
   - Go to [Render Dashboard](https://dashboard.render.com)
   - New → PostgreSQL
   - Configure database (already set up)

3. **Create Web Service**
   - New → Web Service
   - Connect GitHub repository
   - Set root directory: `backend`
   - Build command: `./mvnw clean package -DskipTests`
   - Start command: `java -Dserver.port=$PORT -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar`
   
4. **Configure Environment Variables**
   - `SPRING_PROFILES_ACTIVE=production`
   - `DATABASE_URL=` (auto-filled from database)
   - `JWT_SECRET=` (generate 64-char random string)
   - `MPESA_CONSUMER_KEY=your-key`
   - `MPESA_CONSUMER_SECRET=your-secret`

5. **Deploy**
   
   Render will automatically build and deploy. Check logs for:
   ```
   Started MinetSaccoBackendApplication in XX.XXX seconds
   ```

#### Frontend Deployment (Optional - Static Site)

1. **Create Static Site on Render**
   - New → Static Site
   - Root directory: `minetsacco-main`
   - Build command: `npm run build`
   - Publish directory: `dist`

2. **Environment Variables**
   ```
   VITE_API_URL=https://minetsacco-backend.onrender.com
   ```

---

## 📱 Mobile App Build

### Android APK

1. **Build web app**
   ```bash
   cd minetsacco-main
   npm run build
   ```

2. **Sync Capacitor**
   ```bash
   npx cap sync android
   ```

3. **Build APK**
   ```bash
   cd android
   ./gradlew assembleRelease
   ```

   APK location: `android/app/build/outputs/apk/release/app-release.apk`

### iOS App (requires macOS)

1. **Build web app**
   ```bash
   npm run build
   ```

2. **Sync Capacitor**
   ```bash
   npx cap sync ios
   ```

3. **Open Xcode**
   ```bash
   npx cap open ios
   ```

4. Build in Xcode for distribution

---

## 🔧 Configuration

### Backend Configuration Files

- `application.properties` - Default configuration
- `application-production.properties` - Production (PostgreSQL)
- `application-dev.properties` - Development (MySQL local)
- `render.yaml` - Render deployment configuration

### Frontend Configuration

- `.env.development` - Development environment
- `.env.production` - Production environment (Render)
- `.env.example` - Template for local setup

### Database Migrations

Located in `backend/src/main/resources/db/migration/`

- Flyway manages schema versions
- Auto-runs on application startup
- Baseline version: V0

---

## 🔐 Security

- **Authentication:** JWT tokens (24-hour expiration)
- **Password Hashing:** BCrypt
- **API Security:** Spring Security with role-based access
- **HTTPS:** Enforced in production (Render)
- **CORS:** Configured for mobile apps and web

### Default Credentials

Create admin user manually after first deployment:

```sql
INSERT INTO users (username, password_hash, email, role, active, created_at)
VALUES (
  'admin',
  '$2a$10$...', -- BCrypt hash of your password
  'admin@minetsacco.co.ke',
  'ADMIN',
  true,
  NOW()
);
```

---

## 📊 API Endpoints

### Health Check
- `GET /api/health` - Service health status
- `GET /api/health/detailed` - Detailed health with DB check
- `GET /api/ping` - Simple ping endpoint
- `GET /api/version` - Version information

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `POST /api/auth/refresh-token` - Refresh JWT token

### Member Portal
- `GET /api/member/dashboard` - Member dashboard
- `GET /api/member/profile` - Member profile
- `GET /api/member/loans` - Member loans
- `GET /api/member/accounts` - Member accounts
- `GET /api/member/transactions` - Transaction history

### Staff Portal
- `GET /api/staff/members` - List all members
- `POST /api/staff/loans/approve` - Approve loan
- `POST /api/staff/transactions` - Process transaction
- `GET /api/staff/reports` - Generate reports

Full API documentation: `/swagger-ui.html` (when running locally)

---

## 🧪 Testing

### Backend Tests

```bash
cd backend
./mvnw test
```

### Frontend Tests

```bash
cd minetsacco-main
npm run test
```

---

## 📦 Tech Stack

### Backend
- **Framework:** Spring Boot 3.2
- **Language:** Java 17
- **Database:** PostgreSQL 16 (Render) / MySQL 8 (Local)
- **ORM:** Hibernate/JPA
- **Migration:** Flyway
- **Security:** Spring Security + JWT
- **API Docs:** SpringDoc OpenAPI
- **Build:** Maven

### Frontend
- **Framework:** React 18 + TypeScript
- **Build Tool:** Vite
- **UI Library:** Tailwind CSS + shadcn/ui
- **State:** React Context API
- **HTTP Client:** Axios
- **Mobile:** Capacitor
- **Forms:** React Hook Form
- **Charts:** Recharts

### Infrastructure
- **Hosting:** Render
- **Database:** Render PostgreSQL
- **SSL:** Automatic (Render)
- **Domain:** Custom domain supported

---

## 📝 Environment Variables

### Backend

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | Yes | `production` |
| `DATABASE_URL` | PostgreSQL connection string | Yes | - |
| `JWT_SECRET` | JWT signing key | Yes | - |
| `MPESA_CONSUMER_KEY` | M-Pesa API key | No | - |
| `MPESA_CONSUMER_SECRET` | M-Pesa API secret | No | - |
| `MPESA_SHORTCODE` | M-Pesa shortcode | No | - |

### Frontend

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `VITE_API_URL` | Backend API URL | Yes | `https://minetsacco-backend.onrender.com` |
| `VITE_APP_ENV` | Environment | No | `production` |

---

## 🐛 Troubleshooting

### Backend won't start

**Issue:** Database connection failed

**Solution:** 
- Verify `DATABASE_URL` is correct
- Check PostgreSQL is running
- Ensure Flyway migrations are valid

### Frontend can't connect to backend

**Issue:** CORS or network error

**Solution:**
- Check `VITE_API_URL` in `.env.production`
- Verify backend is running
- Check CORS settings in `application-production.properties`

### Mobile app "Can't fetch" error

**Issue:** Backend not reachable

**Solution:**
- Ensure backend is deployed and running
- Test: `https://minetsacco-backend.onrender.com/api/health`
- Rebuild APK after backend URL change

---

## 📄 License

Proprietary - Minet SACCO. All rights reserved.

---

## 👥 Support

For support and questions:
- **Email:** support@minetsacco.co.ke
- **Documentation:** [Deployment Guides](./DEPLOY_TO_RENDER_CHECKLIST.md)

---

## 🔄 Version History

### v1.1.0 (Current)
- ✅ Render deployment support
- ✅ PostgreSQL migration
- ✅ Health check endpoints
- ✅ Production-ready configuration
- ✅ Mobile app builds (Android/iOS)

### v1.0.0
- ✅ Initial release
- ✅ Member and Staff portals
- ✅ Loan management
- ✅ M-Pesa integration

---

**Built with ❤️ for Minet SACCO**
