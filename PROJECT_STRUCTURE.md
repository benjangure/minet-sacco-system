# Minet SACCO - Project Structure (Updated April 2026)

Complete overview of the project file organization and architecture.

---

## Directory Structure

```
minet-sacco/
├── backend/                          # Spring Boot REST API
│   ├── src/main/java/com/minet/sacco/
│   │   ├── config/                   # Configuration classes
│   │   │   ├── CorsConfig.java
│   │   │   ├── FlywayConfig.java
│   │   │   ├── MpesaConfig.java
│   │   │   └── SecurityConfig.java
│   │   │
│   │   ├── controller/               # REST API endpoints (27 controllers)
│   │   │   ├── AccountController.java
│   │   │   ├── AuditController.java
│   │   │   ├── AuthController.java
│   │   │   ├── BulkProcessingController.java
│   │   │   ├── CustomerSupportController.java
│   │   │   ├── DataMigrationController.java
│   │   │   ├── DebugController.java
│   │   │   ├── EligibilityCalculationController.java
│   │   │   ├── FundConfigurationController.java
│   │   │   ├── HealthController.java
│   │   │   ├── KycDocumentController.java
│   │   │   ├── LoanController.java
│   │   │   ├── LoanEligibilityRulesController.java
│   │   │   ├── LoanProductController.java
│   │   │   ├── LoanRepaymentController.java
│   │   │   ├── MemberController.java
│   │   │   ├── MemberExitController.java
│   │   │   ├── MemberPortalController.java
│   │   │   ├── MemberSuspensionController.java
│   │   │   ├── MpesaDarajaController.java
│   │   │   ├── NotificationController.java
│   │   │   ├── ReportsController.java
│   │   │   ├── SASRAComplianceController.java
│   │   │   ├── SystemSettingsController.java
│   │   │   ├── TellerController.java
│   │   │   ├── TellerContextController.java
│   │   │   ├── TellerLoanRepaymentController.java
│   │   │   └── UserController.java
│   │   │
│   │   ├── service/                  # Business logic (20+ services)
│   │   │   ├── AccountService.java
│   │   │   ├── AuditService.java
│   │   │   ├── AuthService.java
│   │   │   ├── BulkProcessingService.java
│   │   │   ├── BulkValidationService.java
│   │   │   ├── CustomerSupportService.java
│   │   │   ├── DataMigrationService.java
│   │   │   ├── EligibilityCalculationService.java
│   │   │   ├── ExcelParserService.java
│   │   │   ├── GuarantorTrackingService.java
│   │   │   ├── GuarantorValidationService.java
│   │   │   ├── LoanDisbursementService.java
│   │   │   ├── LoanEligibilityRulesService.java
│   │   │   ├── LoanProductService.java
│   │   │   ├── LoanService.java
│   │   │   ├── MemberService.java
│   │   │   ├── NotificationService.java
│   │   │   ├── UserService.java
│   │   │   └── [more services...]
│   │   │
│   │   ├── repository/               # Data access layer (JPA)
│   │   │   ├── AccountRepository.java
│   │   │   ├── AuditLogRepository.java
│   │   │   ├── GuarantorRepository.java
│   │   │   ├── LoanProductRepository.java
│   │   │   ├── LoanRepository.java
│   │   │   ├── MemberRepository.java
│   │   │   ├── NotificationRepository.java
│   │   │   ├── TransactionRepository.java
│   │   │   ├── UserRepository.java
│   │   │   └── [more repositories...]
│   │   │
│   │   ├── entity/                   # JPA entities (database models)
│   │   │   ├── Account.java
│   │   │   ├── AuditLog.java
│   │   │   ├── Guarantor.java
│   │   │   ├── Loan.java
│   │   │   ├── LoanProduct.java
│   │   │   ├── Member.java
│   │   │   ├── Notification.java
│   │   │   ├── Transaction.java
│   │   │   ├── User.java
│   │   │   └── [more entities...]
│   │   │
│   │   ├── dto/                      # Data Transfer Objects (40+ DTOs)
│   │   │   ├── ApiResponse.java
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── BulkBatchDTO.java
│   │   │   ├── BulkUploadRequest.java
│   │   │   ├── DepositRequest.java
│   │   │   ├── DepositRequestDTO.java
│   │   │   ├── GuarantorDetailsDTO.java
│   │   │   ├── GuarantorRequest.java
│   │   │   ├── KycDocumentDTO.java
│   │   │   ├── LoanApplicationRequest.java
│   │   │   ├── LoanApprovalRequest.java
│   │   │   ├── LoanRepaymentDTO.java
│   │   │   ├── MemberApprovalRequest.java
│   │   │   ├── MemberContributionDTO.java
│   │   │   ├── MemberDashboardDTO.java
│   │   │   ├── UserDTO.java
│   │   │   └── [more DTOs...]
│   │   │
│   │   ├── exception/                # Exception handlers
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── ValidationException.java
│   │   │   └── [more exceptions...]
│   │   │
│   │   ├── security/                 # JWT & Spring Security
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── CustomUserDetailsService.java
│   │   │   └── SecurityUtil.java
│   │   │
│   │   ├── util/                     # Utility classes
│   │   │   ├── PasswordHashGenerator.java
│   │   │   ├── ExcelUtil.java
│   │   │   ├── DateUtil.java
│   │   │   └── [more utilities...]
│   │   │
│   │   └── MinetSaccoBackendApplication.java  # Main Spring Boot class
│   │
│   ├── src/main/resources/
│   │   ├── application.properties    # Application configuration
│   │   ├── application-dev.properties
│   │   ├── application-prod.properties
│   │   └── db/migration/             # Flyway SQL migrations (73+ versions)
│   │       ├── V1__Initial_schema.sql
│   │       ├── V2__Insert_initial_data.sql
│   │       ├── V3__Add_audit_logging.sql
│   │       ├── V6__Add_loan_calculated_fields.sql
│   │       ├── V15__Add_member_exit_tracking.sql
│   │       ├── V39__Rename_notification_read_column.sql
│   │       ├── V56__Add_notification_context_fields.sql
│   │       ├── V64__Backfill_guarantor_pledge_amounts.sql
│   │       ├── V67__Add_frozen_savings_to_accounts.sql
│   │       ├── V73__Backfill_guarantee_amounts.sql
│   │       └── [more migrations...]
│   │
│   ├── src/test/java/                # Unit tests
│   │   └── com/minet/sacco/
│   │       ├── repository/
│   │       │   └── TransactionRepositoryTest.java
│   │       └── [more tests...]
│   │
│   ├── pom.xml                       # Maven configuration
│   ├── README.md                     # Backend documentation
│   ├── QUICKSTART.md                 # Quick start guide
│   ├── .env                          # Environment variables (local)
│   ├── .env.example                  # Environment template
│   ├── run_eligibility_debug.sh      # Debug script (Linux/Mac)
│   ├── run_eligibility_debug.ps1     # Debug script (Windows)
│   └── .github/                      # GitHub workflows
│
├── minetsacco-main/                  # React frontend
│   ├── src/
│   │   ├── pages/                    # Page components (20+ pages)
│   │   │   ├── Index.tsx             # Home page
│   │   │   ├── Dashboard.tsx         # Staff dashboard
│   │   │   ├── Members.tsx           # Member management
│   │   │   ├── Loans.tsx             # Loan management (with Loan Officer feature)
│   │   │   ├── MemberLoanApplication.tsx  # Member loan application
│   │   │   ├── LoanRepaymentRecording.tsx # Loan repayment
│   │   │   ├── Savings.tsx           # Savings management
│   │   │   ├── MyGuarantees.tsx      # Guarantor management
│   │   │   ├── BulkProcessing.tsx    # Bulk operations
│   │   │   ├── Reports.tsx           # Reports dashboard
│   │   │   ├── ProfitLossReport.tsx  # P&L report
│   │   │   ├── AuditTrail.tsx        # Audit trail
│   │   │   ├── UserManagement.tsx    # User management
│   │   │   ├── LoanEligibilityRules.tsx  # Eligibility rules
│   │   │   ├── MemberDashboard.tsx   # Member portal dashboard
│   │   │   ├── MemberPortalController.tsx # Member portal
│   │   │   ├── CustomerSupportPortal.tsx  # Customer support
│   │   │   ├── Guide.tsx             # Help/guide page
│   │   │   └── [more pages...]
│   │   │
│   │   ├── components/               # Reusable components
│   │   │   ├── AppSidebar.tsx        # Staff sidebar navigation
│   │   │   ├── MemberSidebar.tsx     # Member sidebar navigation
│   │   │   ├── ProtectedRoute.tsx    # Route protection
│   │   │   ├── NotificationBell.tsx  # Notification display
│   │   │   ├── DocumentUpload.tsx    # Document upload
│   │   │   ├── GuarantorDetailsModal.tsx  # Guarantor details
│   │   │   ├── MpesaTransaction.tsx  # M-Pesa integration
│   │   │   ├── ui/                   # Shadcn UI components
│   │   │   │   ├── button.tsx
│   │   │   │   ├── input.tsx
│   │   │   │   ├── dialog.tsx
│   │   │   │   ├── select.tsx
│   │   │   │   ├── table.tsx
│   │   │   │   └── [more UI components...]
│   │   │   └── [more components...]
│   │   │
│   │   ├── contexts/                 # React Context
│   │   │   └── AuthContext.tsx       # Authentication context
│   │   │
│   │   ├── services/                 # API services
│   │   │   └── notificationService.ts # Notification service
│   │   │
│   │   ├── config/                   # Configuration
│   │   │   └── api.ts                # API configuration
│   │   │
│   │   ├── App.tsx                   # Main app component
│   │   ├── main.tsx                  # React entry point
│   │   └── index.css                 # Global styles
│   │
│   ├── public/                       # Static assets
│   │   └── manifest.json             # PWA manifest
│   │
│   ├── android/                      # Android app (Capacitor)
│   │   ├── app/
│   │   │   ├── src/main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   ├── java/             # Android Java code
│   │   │   │   └── res/              # Android resources
│   │   │   │       ├── values/
│   │   │   │       │   └── styles.xml
│   │   │   │       ├── drawable/
│   │   │   │       │   └── ic_launcher_background.xml
│   │   │   │       ├── drawable-v24/
│   │   │   │       │   └── ic_launcher_foreground.xml
│   │   │   │       ├── mipmap-anydpi-v26/
│   │   │   │       │   └── ic_launcher.xml
│   │   │   │       ├── layout/
│   │   │   │       │   └── splash_screen.xml
│   │   │   │       └── [more resources...]
│   │   │   └── build.gradle
│   │   ├── gradle/
│   │   │   └── wrapper/
│   │   │       └── gradle-wrapper.properties
│   │   ├── build.gradle
│   │   └── settings.gradle
│   │
│   ├── dist/                         # Built frontend (production)
│   │   └── index.html
│   │
│   ├── capacitor.config.ts           # Capacitor configuration
│   ├── vite.config.ts                # Vite build configuration
│   ├── tsconfig.json                 # TypeScript configuration
│   ├── package.json                  # NPM dependencies
│   ├── package-lock.json             # NPM lock file
│   ├── build-apk.ps1                 # APK build script (Windows)
│   └── README.md                     # Frontend documentation
│
├── .git/                             # Git repository
├── .kiro/                            # Kiro IDE configuration
│   ├── specs/                        # Kiro specs
│   │   ├── guarantor-approval-workflow/
│   │   ├── profit-loss-report/
│   │   └── loan-workflow-fix/
│   └── settings/
│
├── .vscode/                          # VS Code configuration
├── .gitignore                        # Git ignore rules
├── README.md                         # Project root README
│
└── Documentation Files (Root):
    ├── SYSTEM_OVERVIEW.md            # System overview (current)
    ├── PROJECT_STRUCTURE.md          # This file
    ├── SYSTEM_DESIGN.md              # System design (current)
    ├── USAGE_GUIDE.md                # Usage guide (current)
    ├── PRESENTATION_SUMMARY.md       # Loan officer feature summary
    ├── GUARANTOR_REJECTION_HANDLING.md  # Guarantor rejection workflow
    └── [SQL cleanup scripts]         # Database maintenance scripts
```

---

## Backend Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         REST API Controllers            │  ← HTTP endpoints
├─────────────────────────────────────────┤
│         Business Logic Services         │  ← Core logic
├─────────────────────────────────────────┤
│         Data Access Repositories        │  ← Database queries
├─────────────────────────────────────────┤
│         JPA Entities & Database         │  ← PostgreSQL
└─────────────────────────────────────────┘
```

### Key Components

**Controllers (27 total)**
- Handle HTTP requests/responses
- Validate input parameters
- Call appropriate services
- Return JSON responses

**Services (20+ total)**
- Implement business logic
- Handle transactions
- Validate business rules
- Coordinate between repositories

**Repositories (10+ total)**
- Extend JpaRepository
- Custom query methods
- Database access abstraction

**Entities (10+ total)**
- JPA annotated classes
- Map to database tables
- Define relationships

**DTOs (40+ total)**
- Transfer data between layers
- Separate API contracts from entities
- Validation annotations

---

## Frontend Architecture

### Component Structure

```
App.tsx (Main)
├── ProtectedRoute (Auth wrapper)
│   ├── Staff Portal
│   │   ├── AppSidebar (Navigation)
│   │   ├── Dashboard
│   │   ├── Members
│   │   ├── Loans (with Loan Officer feature)
│   │   ├── Savings
│   │   ├── Reports
│   │   ├── AuditTrail
│   │   └── [more pages...]
│   │
│   └── Member Portal
│       ├── MemberSidebar (Navigation)
│       ├── MemberDashboard
│       ├── MemberLoanApplication
│       ├── MyGuarantees
│       └── [more pages...]
│
└── Public Pages
    ├── Login
    ├── Guide
    └── Index
```

### Technology Stack

**Frontend:**
- React 18+ with TypeScript
- Vite (build tool)
- Tailwind CSS (styling)
- Shadcn/ui (component library)
- Axios (HTTP client)
- React Context (state management)

**Mobile:**
- Capacitor (React wrapper)
- Android native (Gradle)
- Same React codebase

---

## Database Schema

### Core Tables

**users** - Staff accounts
- id, username, email, password_hash, role, enabled, created_at

**members** - Member profiles
- id, member_number, employee_id, first_name, last_name, phone, email, status, created_at

**accounts** - Savings/shares accounts
- id, member_id, type (SAVINGS/SHARES/CONTRIBUTIONS), balance, frozen_savings, created_at

**transactions** - Deposits/withdrawals
- id, account_id, type (DEPOSIT/WITHDRAWAL), amount, status, created_at

**loans** - Loan records
- id, member_id, loan_product_id, amount, interest_rate, term_months, status, created_at

**guarantors** - Guarantor relationships
- id, loan_id, member_id, guarantee_amount, status, created_at

**loan_products** - Loan configuration
- id, name, interest_rate, min_amount, max_amount, min_term_months, max_term_months

**audit_log** - Audit trail
- id, user_id, action, entity_type, entity_id, old_value, new_value, timestamp

**notifications** - System notifications
- id, user_id, message, type, read_at, created_at

---

## File Organization by Feature

### Member Management
- Backend: `MemberService.java`, `MemberController.java`, `MemberRepository.java`
- Frontend: `Members.tsx`
- Database: `members` table, migrations V1-V15

### Loan Management
- Backend: `LoanService.java`, `LoanController.java`, `LoanRepository.java`
- Frontend: `Loans.tsx`, `MemberLoanApplication.tsx`
- Database: `loans`, `guarantors` tables, migrations V1-V73

### Loan Officer Feature ✨ NEW
- Backend: `LoanService.java` (eligibility validation)
- Frontend: `Loans.tsx` (loan officer interface)
- Features: Member selection, live eligibility, guarantor search, total guarantee validation

### Savings Management
- Backend: `AccountService.java`, `AccountController.java`
- Frontend: `Savings.tsx`
- Database: `accounts`, `transactions` tables

### Guarantor Management
- Backend: `GuarantorTrackingService.java`, `GuarantorValidationService.java`
- Frontend: `MyGuarantees.tsx`
- Database: `guarantors` table

### Bulk Processing
- Backend: `BulkProcessingService.java`, `ExcelParserService.java`
- Frontend: `BulkProcessing.tsx`
- Features: Member registration, loan applications, loan repayments

### Reports & Analytics
- Backend: `ReportsController.java`
- Frontend: `Reports.tsx`, `ProfitLossReport.tsx`
- Features: P&L report, member reports, loan reports

### Audit Trail
- Backend: `AuditService.java`, `AuditController.java`
- Frontend: `AuditTrail.tsx`
- Database: `audit_log` table

### Notifications
- Backend: `NotificationService.java`, `NotificationController.java`
- Frontend: `NotificationBell.tsx`
- Database: `notifications` table

### Authentication & Security
- Backend: `AuthService.java`, `JwtTokenProvider.java`, `SecurityConfig.java`
- Frontend: `AuthContext.tsx`, `ProtectedRoute.tsx`
- Features: JWT tokens, role-based access control

---

## Build & Deployment

### Backend Build
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend Build
```bash
cd minetsacco-main
npm install
npm run dev          # Development
npm run build        # Production
```

### Android APK Build
```bash
cd minetsacco-main
npm run build
npx cap add android
npx cap sync
# Then build in Android Studio or:
./build-apk.ps1      # Windows
./build-apk.sh       # Linux/Mac
```

---

## Configuration Files

### Backend Configuration
- `application.properties` - Main configuration
- `application-dev.properties` - Development overrides
- `application-prod.properties` - Production overrides
- `.env` - Environment variables (local)
- `pom.xml` - Maven dependencies

### Frontend Configuration
- `vite.config.ts` - Vite build configuration
- `tsconfig.json` - TypeScript configuration
- `package.json` - NPM dependencies
- `capacitor.config.ts` - Capacitor configuration

### IDE Configuration
- `.vscode/` - VS Code settings
- `.kiro/` - Kiro IDE settings
- `.idea/` - IntelliJ IDEA settings

---

## Development Workflow

### Adding a New Feature

1. **Backend**
   - Create entity in `entity/`
   - Create repository in `repository/`
   - Create service in `service/`
   - Create controller in `controller/`
   - Create DTOs in `dto/`
   - Add database migration in `db/migration/`

2. **Frontend**
   - Create page component in `pages/`
   - Create reusable components in `components/`
   - Add API calls in service layer
   - Update navigation in sidebar

3. **Testing**
   - Add unit tests in `src/test/`
   - Test API endpoints with Swagger
   - Test frontend components

4. **Documentation**
   - Update relevant documentation files
   - Add code comments
   - Update this file if structure changes

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `backend/pom.xml` | Maven dependencies |
| `minetsacco-main/package.json` | NPM dependencies |
| `backend/src/main/resources/application.properties` | Backend config |
| `minetsacco-main/vite.config.ts` | Frontend build config |
| `backend/src/main/resources/db/migration/` | Database migrations |
| `minetsacco-main/src/pages/Loans.tsx` | Loan officer interface |
| `backend/src/main/java/com/minet/sacco/service/LoanService.java` | Loan business logic |
| `minetsacco-main/src/contexts/AuthContext.tsx` | Authentication state |
| `backend/src/main/java/com/minet/sacco/security/JwtTokenProvider.java` | JWT handling |

---

## Documentation Files

| File | Purpose |
|------|---------|
| `SYSTEM_OVERVIEW.md` | Complete system overview |
| `PROJECT_STRUCTURE.md` | This file - project organization |
| `SYSTEM_DESIGN.md` | System architecture and design |
| `USAGE_GUIDE.md` | Step-by-step usage instructions |
| `PRESENTATION_SUMMARY.md` | Loan officer feature summary |
| `GUARANTOR_REJECTION_HANDLING.md` | Guarantor rejection workflow |

---

## Quick Navigation

- **Backend Code**: `backend/src/main/java/com/minet/sacco/`
- **Frontend Code**: `minetsacco-main/src/`
- **Database Migrations**: `backend/src/main/resources/db/migration/`
- **API Documentation**: `http://localhost:8080/swagger-ui/index.html` (when running)
- **Configuration**: `backend/src/main/resources/application.properties`

---

## Notes

- All Java code follows Spring Boot conventions
- All React code uses TypeScript for type safety
- Database migrations are versioned and immutable
- DTOs separate API contracts from internal entities
- Services contain all business logic
- Controllers are thin and delegate to services
- Frontend uses React Context for state management
- Mobile app uses Capacitor for cross-platform compatibility

