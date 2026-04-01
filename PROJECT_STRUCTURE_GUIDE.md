# Minet SACCO - Project Structure Guide

Complete guide to the project structure, file purposes, and troubleshooting.

## Project Layout

```
minetsacco-main/
├── backend/                          # Spring Boot backend
├── minetsacco-main/                  # React web frontend
├── android/                          # Android APK configuration
├── SYSTEM_OVERVIEW.md               # System overview & architecture
├── SYSTEM_DESIGN_EXPLAINED.md        # Design decisions & implementation
├── PROJECT_STRUCTURE_GUIDE.md        # This file
├── USAGE_GUIDE.md                    # Step-by-step usage guide
└── README.md                         # Quick start guide
```

## Backend Structure

### `backend/`

```
backend/
├── pom.xml                           # Maven dependencies & build config
├── README.md                         # Backend setup instructions
├── QUICKSTART.md                     # Quick start guide
├── .env                              # Environment variables (local)
├── .env.example                      # Environment template
│
├── src/main/java/com/minet/sacco/
│   ├── SaccoApplication.java         # Spring Boot entry point
│   │
│   ├── config/                       # Configuration classes
│   │   ├── CorsConfig.java           # CORS settings for API access
│   │   ├── MpesaConfig.java          # M-Pesa integration config
│   │   └── SecurityConfig.java       # JWT & Spring Security config
│   │
│   ├── controller/                   # REST API endpoints
│   │   ├── AuthController.java       # Login endpoints
│   │   ├── MemberController.java     # Member management
│   │   ├── AccountController.java    # Account operations
│   │   ├── LoanController.java       # Loan management
│   │   ├── BulkProcessingController.java  # Excel uploads
│   │   ├── ReportsController.java    # Financial reports
│   │   ├── AuditController.java      # Audit trail
│   │   ├── NotificationController.java   # Notifications
│   │   ├── MemberPortalController.java   # Member dashboard
│   │   └── [other controllers]
│   │
│   ├── service/                      # Business logic
│   │   ├── AuthService.java          # Authentication logic
│   │   ├── MemberService.java        # Member operations
│   │   ├── AccountService.java       # Account management
│   │   ├── LoanService.java          # Loan calculations & workflow
│   │   ├── BulkProcessingService.java    # Bulk operations
│   │   ├── ExcelParserService.java   # Excel file parsing
│   │   ├── BulkValidationService.java    # Data validation
│   │   ├── AuditService.java         # Audit logging
│   │   ├── NotificationService.java  # Notification sending
│   │   ├── GuarantorValidationService.java  # Guarantor checks
│   │   └── [other services]
│   │
│   ├── entity/                       # Database entities (JPA)
│   │   ├── User.java                 # Staff & member users
│   │   ├── Member.java               # Member profile
│   │   ├── Account.java              # Savings/shares accounts
│   │   ├── Transaction.java          # Deposits/withdrawals
│   │   ├── Loan.java                 # Loan records
│   │   ├── LoanProduct.java          # Loan product config
│   │   ├── Guarantor.java            # Guarantor relationships
│   │   ├── AuditLog.java             # Audit trail records
│   │   ├── Notification.java         # System notifications
│   │   ├── BulkBatch.java            # Bulk upload batches
│   │   └── [other entities]
│   │
│   ├── repository/                   # Data access (JPA)
│   │   ├── UserRepository.java       # User queries
│   │   ├── MemberRepository.java     # Member queries
│   │   ├── AccountRepository.java    # Account queries
│   │   ├── LoanRepository.java       # Loan queries
│   │   ├── TransactionRepository.java    # Transaction queries
│   │   ├── AuditLogRepository.java   # Audit queries
│   │   └── [other repositories]
│   │
│   ├── dto/                          # Data Transfer Objects
│   │   ├── AuthRequest.java          # Login request
│   │   ├── AuthResponse.java         # Login response with token
│   │   ├── UserDTO.java              # User data transfer
│   │   ├── MemberDTO.java            # Member data transfer
│   │   ├── LoanApplicationRequest.java   # Loan application form
│   │   ├── DepositRequestDTO.java    # Deposit request
│   │   ├── LoanRepaymentDTO.java     # Repayment data
│   │   ├── ApiResponse.java          # Standard API response
│   │   └── [other DTOs]
│   │
│   ├── exception/                    # Custom exceptions
│   │   ├── ResourceNotFoundException.java
│   │   ├── UnauthorizedException.java
│   │   ├── ValidationException.java
│   │   └── [other exceptions]
│   │
│   ├── security/                     # Security components
│   │   ├── JwtProvider.java          # JWT token generation/validation
│   │   ├── JwtAuthenticationFilter.java  # JWT filter
│   │   └── CustomUserDetailsService.java # User loading
│   │
│   └── util/                         # Utility classes
│       ├── PasswordHashGenerator.java    # Password hashing
│       ├── DateUtils.java            # Date formatting
│       └── [other utilities]
│
├── src/main/resources/
│   ├── application.properties        # Spring configuration
│   ├── application-dev.properties    # Development config
│   ├── application-prod.properties   # Production config
│   │
│   └── db/migration/                 # Database migrations (Flyway)
│       ├── V1__Initial_schema.sql    # Initial tables
│       ├── V2__Insert_initial_data.sql   # Seed data
│       ├── V3__Add_audit_log.sql     # Audit table
│       ├── V4__Add_notifications.sql # Notification table
│       └── [other migrations]
│
└── src/test/java/                    # Unit & integration tests
    └── com/minet/sacco/
        ├── repository/
        ├── service/
        └── controller/
```

### Key Backend Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies (Spring Boot, JPA, JWT, Excel, etc.) |
| `application.properties` | Database URL, JWT secret, server port |
| `.env` | Sensitive config (passwords, API keys) |
| `SaccoApplication.java` | Spring Boot entry point |
| `SecurityConfig.java` | JWT filter, CORS, authentication |
| `JwtProvider.java` | Token generation & validation |
| `AuthService.java` | Login logic, role validation |
| `AccountService.java` | Deposit/withdrawal logic, balance updates |
| `LoanService.java` | Loan calculations, approval workflow |
| `ExcelParserService.java` | Excel file parsing with date format support |
| `AuditService.java` | Logs all user actions |

## Frontend Structure

### `minetsacco-main/`

```
minetsacco-main/
├── package.json                      # npm dependencies & scripts
├── vite.config.ts                    # Vite build configuration
├── tsconfig.json                     # TypeScript configuration
├── tailwind.config.js                # Tailwind CSS configuration
├── capacitor.config.ts               # Capacitor (mobile) config
│
├── src/
│   ├── main.tsx                      # React entry point
│   ├── App.tsx                       # Main app component & routing
│   │
│   ├── pages/                        # Page components
│   │   ├── Login.tsx                 # Staff login page
│   │   ├── MemberLogin.tsx           # Member login page
│   │   ├── Dashboard.tsx             # Staff dashboard
│   │   ├── MemberDashboard.tsx       # Member dashboard
│   │   ├── Members.tsx               # Member list (staff)
│   │   ├── Savings.tsx               # Savings management (staff)
│   │   ├── Loans.tsx                 # Loan management (staff)
│   │   ├── LoanEligibilityRules.tsx  # Loan product config
│   │   ├── BulkProcessing.tsx        # Excel upload page
│   │   ├── Reports.tsx               # Financial reports
│   │   ├── AuditTrail.tsx            # Audit log viewer
│   │   ├── UserManagement.tsx        # User management
│   │   ├── MemberLoanApplication.tsx # Member loan application
│   │   ├── MemberPortal.tsx          # Member portal layout
│   │   ├── ProfitLossReport.tsx      # P&L report
│   │   └── [other pages]
│   │
│   ├── components/                   # Reusable components
│   │   ├── ProtectedRoute.tsx        # Route protection wrapper
│   │   ├── PublicRoute.tsx           # Public route wrapper
│   │   ├── AppSidebar.tsx            # Staff sidebar navigation
│   │   ├── MemberSidebar.tsx         # Member sidebar navigation
│   │   ├── NotificationBell.tsx      # Notification icon
│   │   ├── DepositRequestForm.tsx    # Deposit form (member)
│   │   ├── GuarantorApprovalModal.tsx    # Guarantor approval dialog
│   │   ├── DocumentUpload.tsx        # KYC document upload
│   │   ├── MpesaTransaction.tsx      # M-Pesa payment UI
│   │   └── ui/                       # Shadcn/ui components
│   │       ├── button.tsx
│   │       ├── input.tsx
│   │       ├── dialog.tsx
│   │       ├── table.tsx
│   │       └── [other UI components]
│   │
│   ├── hooks/                        # Custom React hooks
│   │   ├── useAuth.ts                # Authentication hook
│   │   ├── useApi.ts                 # API request hook
│   │   └── [other hooks]
│   │
│   ├── context/                      # React Context
│   │   └── AuthContext.tsx           # Authentication state (staff)
│   │
│   ├── config/                       # Configuration
│   │   ├── api.ts                    # Axios instance with interceptors
│   │   └── constants.ts              # App constants
│   │
│   ├── types/                        # TypeScript types
│   │   ├── index.ts                  # Type definitions
│   │   └── api.ts                    # API response types
│   │
│   ├── assets/                       # Static assets
│   │   ├── images/
│   │   │   └── logo.png              # Minet logo
│   │   └── styles/
│   │       └── globals.css           # Global styles
│   │
│   └── utils/                        # Utility functions
│       ├── formatters.ts             # Date/currency formatting
│       ├── validators.ts             # Input validation
│       └── [other utilities]
│
├── public/                           # Static files
│   ├── index.html                    # HTML template
│   ├── manifest.json                 # PWA manifest
│   └── favicon.ico
│
├── android/                          # Android APK configuration
│   ├── app/
│   │   ├── build.gradle              # Android build config
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml   # App permissions & entry point
│   │   │   ├── java/                 # Kotlin/Java code
│   │   │   └── res/                  # Android resources
│   │   │       ├── mipmap-*/         # App icons (different densities)
│   │   │       ├── drawable*/        # Splash screens
│   │   │       └── values/           # Colors, strings, etc.
│   │   └── [other Android files]
│   │
│   ├── build.gradle                  # Root Gradle config
│   └── gradle/wrapper/               # Gradle wrapper
│
└── dist/                             # Built files (generated)
    ├── index.html
    ├── assets/
    └── [other built files]
```

### Key Frontend Files

| File | Purpose |
|------|---------|
| `App.tsx` | Main routing & layout |
| `config/api.ts` | Axios instance with JWT interceptor |
| `context/AuthContext.tsx` | Staff authentication state |
| `pages/Login.tsx` | Staff login page |
| `pages/MemberLogin.tsx` | Member login page |
| `pages/Dashboard.tsx` | Staff dashboard |
| `pages/MemberDashboard.tsx` | Member dashboard |
| `components/ProtectedRoute.tsx` | Route protection wrapper |
| `components/AppSidebar.tsx` | Staff navigation |
| `components/MemberSidebar.tsx` | Member navigation |
| `pages/BulkProcessing.tsx` | Excel upload interface |
| `pages/AuditTrail.tsx` | Audit log viewer |

## Android APK Structure

### `android/`

```
android/
├── app/
│   ├── build.gradle                  # Build configuration
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # App manifest
│   │   ├── java/                     # Kotlin/Java code
│   │   │   └── com/minet/sacco/
│   │   │       └── MainActivity.kt   # App entry point
│   │   │
│   │   └── res/                      # Resources
│   │       ├── mipmap-mdpi/          # App icon (160 dpi)
│   │       │   └── ic_launcher_foreground_logo.png
│   │       ├── mipmap-hdpi/          # App icon (240 dpi)
│   │       ├── mipmap-xhdpi/         # App icon (320 dpi)
│   │       ├── mipmap-xxhdpi/        # App icon (480 dpi)
│   │       ├── mipmap-xxxhdpi/       # App icon (640 dpi)
│   │       ├── mipmap-anydpi-v26/    # Adaptive icon (Android 8+)
│   │       │   └── ic_launcher.xml   # Icon definition
│   │       │
│   │       ├── drawable/             # Splash screen (portrait)
│   │       │   └── splash.png
│   │       ├── drawable-port-ldpi/   # Splash (portrait, low dpi)
│   │       ├── drawable-port-mdpi/   # Splash (portrait, medium dpi)
│   │       ├── drawable-port-hdpi/   # Splash (portrait, high dpi)
│   │       ├── drawable-port-xhdpi/  # Splash (portrait, extra high dpi)
│   │       ├── drawable-port-xxhdpi/ # Splash (portrait, 2x high dpi)
│   │       ├── drawable-port-xxxhdpi/# Splash (portrait, 3x high dpi)
│   │       │
│   │       ├── drawable-land-ldpi/   # Splash (landscape, low dpi)
│   │       ├── drawable-land-mdpi/   # Splash (landscape, medium dpi)
│   │       ├── drawable-land-hdpi/   # Splash (landscape, high dpi)
│   │       ├── drawable-land-xhdpi/  # Splash (landscape, extra high dpi)
│   │       ├── drawable-land-xxhdpi/ # Splash (landscape, 2x high dpi)
│   │       ├── drawable-land-xxxhdpi/# Splash (landscape, 3x high dpi)
│   │       │
│   │       ├── values/               # Default resources
│   │       │   ├── colors.xml        # Color definitions
│   │       │   ├── strings.xml       # String resources
│   │       │   └── styles.xml        # Style definitions
│   │       │
│   │       └── values-night/         # Dark mode resources
│   │
│   └── [other Android files]
│
├── build.gradle                      # Root Gradle config
├── gradle/wrapper/                   # Gradle wrapper
└── settings.gradle                   # Gradle settings
```

### Key Android Files

| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | App permissions, entry point, app name |
| `build.gradle` | Build configuration, dependencies |
| `MainActivity.kt` | App entry point (Capacitor bridge) |
| `mipmap-*/ic_launcher_foreground_logo.png` | App icon (different screen densities) |
| `drawable*/splash.png` | Splash screen image |
| `values/colors.xml` | Color definitions |
| `values/strings.xml` | String resources |

## Troubleshooting Guide

### Backend Issues

#### Issue: 401 Unauthorized on API calls

**Symptoms:** API returns 401, member can't access dashboard

**Check:**
1. Is JWT token being sent? → Check `Authorization: Bearer <token>` header
2. Is token valid? → Check token expiration in `JwtProvider.java`
3. Is user role correct? → Check `User.role` in database
4. Is endpoint protected? → Check `@PreAuthorize` annotation

**Fix:**
```bash
# Check token in browser console
localStorage.getItem('token')

# Verify token on backend
# Add logging in JwtProvider.validateToken()
```

#### Issue: Deposit to shares account not blocked

**Symptoms:** User can deposit to shares account

**Check:**
1. Is `AccountService.deposit()` validating account type?
2. Is frontend filtering shares from dropdown?

**Fix:**
```java
// In AccountService.deposit()
if (account.getType() == AccountType.SHARES) {
    throw new ValidationException("Deposits not allowed for shares account");
}
```

#### Issue: Date format validation error on bulk upload

**Symptoms:** Excel upload fails with date format error

**Check:**
1. Is `ExcelParserService.parseDateCell()` detecting Excel dates?
2. Are all date formats supported?

**Fix:**
```java
// In ExcelParserService
if (DateUtil.isCellDateFormatted(cell)) {
    // Parse as Excel date
    return cell.getDateCellValue();
}
```

#### Issue: Audit trail not logging actions

**Symptoms:** Audit log is empty

**Check:**
1. Is `AuditService.log()` being called?
2. Is `@Transactional` on service methods?
3. Is audit_log table created?

**Fix:**
```bash
# Check if audit_log table exists
SELECT * FROM audit_log;

# Check if migrations ran
SELECT * FROM flyway_schema_history;
```

### Frontend Issues

#### Issue: Member can't log in

**Symptoms:** Login page shows error, member portal inaccessible

**Check:**
1. Is backend running? → Check `localhost:8080/api/auth/member/login`
2. Is token being stored? → Check `localStorage.getItem('token')`
3. Is member role correct? → Check database `user.role = 'MEMBER'`

**Fix:**
```typescript
// In MemberLogin.tsx
console.log('Token:', localStorage.getItem('token'));
console.log('Response:', response);
```

#### Issue: Staff can't access dashboard

**Symptoms:** Blank page after login, redirect loop

**Check:**
1. Is token in AuthContext? → Check `useAuth()` hook
2. Is user role staff? → Check `user.role !== 'MEMBER'`
3. Is ProtectedRoute working? → Check route guards

**Fix:**
```typescript
// In App.tsx
console.log('User:', user);
console.log('Route:', location.pathname);
```

#### Issue: Logo not showing in APK

**Symptoms:** App icon and splash screen show "M" instead of logo

**Check:**
1. Is logo file at `src/assets/images/logo.png`?
2. Are icon files copied to `android/app/src/main/res/mipmap-*`?
3. Is splash screen copied to `android/app/src/main/res/drawable*`?

**Fix:**
```powershell
# Copy logo to all icon folders
$logo = "src\assets\images\logo.png"
$folders = @("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")

foreach ($f in $folders) {
    $dest = "android\app\src\main\res\$f\ic_launcher_foreground_logo.png"
    Copy-Item $logo $dest -Force
}

# Rebuild APK
npm run build
npx cap sync android
```

#### Issue: Shares account showing in deposit dropdown

**Symptoms:** Member can select shares account for deposit

**Check:**
1. Is `DepositRequestForm.tsx` filtering shares?
2. Is backend rejecting shares deposits?

**Fix:**
```typescript
// In DepositRequestForm.tsx
const availableAccounts = accounts.filter(
  acc => acc.type !== 'SHARES'
);
```

### Mobile (APK) Issues

#### Issue: APK won't build

**Symptoms:** Build fails with Gradle error

**Check:**
1. Is Java 17+ installed? → `java -version`
2. Is Android SDK installed? → Check `ANDROID_HOME`
3. Is Gradle wrapper working? → Check `android/gradle/wrapper/gradle-wrapper.properties`

**Fix:**
```bash
# Clean and rebuild
cd android
./gradlew clean
./gradlew build
```

#### Issue: APK opens staff login instead of member

**Symptoms:** APK shows staff login page

**Check:**
1. Is `capacitor.config.ts` set to `/member`?
2. Is `MainActivity.kt` loading correct URL?

**Fix:**
```typescript
// In capacitor.config.ts
const config: CapacitorConfig = {
  appId: 'com.minet.sacco',
  appName: 'Minet SACCO',
  webDir: 'dist',
  server: {
    url: 'http://localhost:3000/member',  // Start at member portal
    cleartext: true
  }
};
```

#### Issue: APK can't connect to backend

**Symptoms:** API calls fail, network error

**Check:**
1. Is backend running? → Check `localhost:8080`
2. Is API URL correct? → Check `config/api.ts`
3. Is CORS enabled? → Check `CorsConfig.java`

**Fix:**
```typescript
// In config/api.ts
const API_BASE_URL = 'http://192.168.x.x:8080';  // Use device IP, not localhost
```

## Making Updates That Reflect in Mobile App

### Workflow for Changes

```
1. Make code changes in React components
   └─ src/pages/MemberDashboard.tsx
   └─ src/components/DepositRequestForm.tsx

2. Test in web browser
   └─ npm run dev
   └─ Visit localhost:3000/member

3. Build React app
   └─ npm run build
   └─ Creates dist/ folder

4. Sync with Capacitor
   └─ npx cap sync android
   └─ Copies dist/ to Android assets

5. Rebuild APK
   └─ cd android
   └─ ./gradlew build
   └─ Creates APK file

6. Install on device
   └─ adb install app-release.apk
   └─ Or upload to Play Store
```

### Backend Changes

```
1. Make code changes in Java
   └─ src/main/java/com/minet/sacco/service/

2. Rebuild backend
   └─ mvn clean package
   └─ Creates JAR file

3. Restart backend server
   └─ java -jar target/sacco-app.jar

4. Both web and mobile automatically use new API
   └─ No rebuild needed for frontend
```

### Database Changes

```
1. Create migration file
   └─ src/main/resources/db/migration/V##__Description.sql

2. Restart backend
   └─ Flyway automatically runs migrations

3. Both web and mobile see new data
   └─ No rebuild needed
```

### Key Points

- **Backend changes** → Restart server, both web & mobile updated
- **Frontend changes** → Rebuild APK for mobile, web updates automatically
- **Database changes** → Create migration, restart server
- **Logo/Icon changes** → Copy to Android folders, rebuild APK
- **Configuration changes** → Update `.env` or `application.properties`, restart server

