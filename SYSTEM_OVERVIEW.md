# Minet SACCO Management System - System Overview

## 1. What is the System?

Minet SACCO Management System is a comprehensive digital platform designed for managing Savings and Credit Cooperative Organization (SACCO) operations. It's an in-house SACCO system built specifically for Minet Insurance employees, providing both web-based staff portal and mobile member portal interfaces.

The system handles member registration, savings management, loan applications, approvals, disbursements, and comprehensive financial reporting with role-based access control.

## 2. Problems the System Solves

- **Manual Member Management** → Automated member registration with KYC verification
- **Loan Processing Delays** → Streamlined loan workflow with multi-level approvals
- **Lack of Financial Transparency** → Real-time dashboards and comprehensive reports
- **Disconnected Channels** → Unified platform for staff and members
- **Data Security Concerns** → JWT-based authentication with role-based permissions
- **Audit Trail Gaps** → Complete audit logging of all transactions for SASRA compliance
- **Mobile Accessibility** → Native Android APK for member access on-the-go
- **Bulk Operations** → Excel-based bulk processing for monthly contributions and member registration
- **Incorrect Loan Eligibility** → Formula-based calculation accounting for active loans
- **Incomplete Repayment Process** → Transaction recording, conditional savings debit, guarantor pledge release
- **Improper Account Restrictions** → Shares account properly restricted from deposits

## 3. System Architecture

### Technology Stack

**Backend:**
- Java 17 with Spring Boot 3.x
- Spring Security with JWT authentication
- Spring Data JPA with Hibernate ORM
- PostgreSQL database
- Maven for dependency management

**Frontend (Web):**
- React 18 with TypeScript
- Vite as build tool
- Tailwind CSS for styling
- Shadcn/ui component library
- Axios for HTTP requests

**Mobile:**
- Capacitor for cross-platform mobile development
- React Native bridge to Android
- Gradle for Android build management

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  (React Web Portal + Android Mobile App)                │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    API Layer                             │
│  (Spring Boot REST Controllers)                         │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  Business Logic Layer                    │
│  (Services: LoanService, AccountService, etc.)         │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  Data Access Layer                       │
│  (JPA Repositories, Hibernate ORM)                      │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                   Database Layer                         │
│  (PostgreSQL)                                           │
└─────────────────────────────────────────────────────────┘
```

### Key Components

1. **Authentication & Authorization**
   - JWT token-based authentication
   - Role-based access control (RBAC)
   - 7 user roles: ADMIN, TREASURER, LOAN_OFFICER, CREDIT_COMMITTEE, AUDITOR, TELLER, MEMBER

2. **Member Management**
   - Member registration with KYC verification
   - Document upload and tracking
   - Member status management

3. **Account Management**
   - Multiple account types: Savings, Shares, Contributions, Benevolent Fund, etc.
   - Balance tracking and transaction history
   - Shares account dormant (no deposits allowed)

4. **Loan Management**
   - Loan product configuration
   - Eligibility rules engine
   - Multi-stage approval workflow
   - Guarantor validation
   - Loan repayment tracking

5. **Bulk Processing**
   - Excel-based member registration
   - Monthly contributions processing
   - Loan disbursements
   - Data validation and error reporting

6. **Reporting & Analytics**
   - Member reports
   - Loan portfolio analysis
   - Savings summary
   - Profit & loss statements
   - Transaction history

7. **Audit & Compliance**
   - Complete audit trail logging
   - User action tracking
   - SASRA compliance features
   - Data integrity checks

## 4. Code Quality Measures

### Clean Code Practices

1. **Separation of Concerns**
   - Controllers handle HTTP requests only
   - Services contain business logic
   - Repositories handle data access
   - DTOs for data transfer

2. **SOLID Principles**
   - Single Responsibility: Each class has one reason to change
   - Open/Closed: Open for extension, closed for modification
   - Liskov Substitution: Proper interface implementation
   - Interface Segregation: Focused interfaces
   - Dependency Inversion: Depend on abstractions, not concretions

3. **Design Patterns**
   - Repository Pattern for data access
   - Service Layer Pattern for business logic
   - DTO Pattern for data transfer
   - Factory Pattern for object creation
   - Strategy Pattern for different validation rules

4. **Code Organization**
   - Logical package structure (controller, service, entity, dto, repository)
   - Consistent naming conventions
   - Proper exception handling
   - Comprehensive logging

5. **Testing & Validation**
   - Input validation at controller and service levels
   - Custom validators for business rules
   - Transaction management with @Transactional
   - Error handling with meaningful messages

6. **Documentation**
   - JavaDoc comments on public methods
   - Clear variable and method names
   - Inline comments for complex logic
   - README files for setup and usage

## 5. API Endpoints

### Authentication Endpoints
- `POST /api/auth/login` - Staff login
- `POST /api/auth/member/login` - Member login

### Member Management (Staff)
- `GET /api/members` - List all members
- `GET /api/members/{id}` - Get member details
- `POST /api/members` - Create member
- `PUT /api/members/{id}` - Update member
- `GET /api/members/status/{status}` - Filter by status
- `POST /api/members/{id}/approve` - Approve member

### Account Management
- `GET /api/accounts` - List all accounts
- `GET /api/accounts/{id}` - Get account details
- `POST /api/accounts/deposit` - Process deposit
- `POST /api/accounts/withdraw` - Process withdrawal
- `GET /api/member/accounts` - Member's accounts

### Loan Management
- `GET /api/loans` - List loans
- `POST /api/loans/apply` - Apply for loan
- `GET /api/loans/{id}` - Get loan details
- `POST /api/loans/{id}/approve` - Approve loan
- `POST /api/loans/{id}/disburse` - Disburse loan
- `POST /api/loans/{id}/repay` - Repay loan
- `GET /api/loan-products` - List loan products
- `POST /api/loan-products` - Create loan product

### Bulk Processing
- `POST /api/bulk/upload` - Upload bulk file
- `GET /api/bulk/batches` - List batches
- `POST /api/bulk/batches/{id}/approve` - Approve batch
- `POST /api/bulk/batches/{id}/reject` - Reject batch

### Reports
- `GET /api/reports/members` - Member report
- `GET /api/reports/loans` - Loan portfolio report
- `GET /api/reports/savings` - Savings summary
- `GET /api/reports/profit-loss` - P&L statement
- `GET /api/reports/transactions` - Transaction history

### Audit & Compliance
- `GET /api/audit-trail` - Audit log
- `GET /api/audit-trail/user/{userId}` - User actions
- `GET /api/sasra/compliance` - SASRA compliance data

### Notifications
- `GET /api/notifications` - Get notifications
- `PUT /api/notifications/{id}/read` - Mark as read
- `DELETE /api/notifications/{id}` - Delete notification

### Member Portal
- `GET /api/member/dashboard` - Member dashboard
- `GET /api/member/account-statement` - Account statement
- `GET /api/member/loan-balances` - Loan balances
- `POST /api/member/deposit-requests` - Submit deposit request
- `GET /api/member/guarantor-requests` - Guarantor requests
- `POST /api/member/guarantor-requests/{id}/approve` - Approve guarantee

**Total: 50+ REST endpoints**

## 6. Spring Boot Dependencies

### Core Dependencies (from Spring Web Initializr)
- `spring-boot-starter-web` - Web MVC framework
- `spring-boot-starter-data-jpa` - JPA/Hibernate ORM
- `spring-boot-starter-security` - Security framework
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-mail` - Email support
- `spring-boot-starter-actuator` - Application monitoring

### Additional Dependencies Added
- `jjwt` - JWT token generation and validation
- `postgresql` - PostgreSQL JDBC driver
- `apache-poi` - Excel file processing
- `commons-io` - File I/O utilities
- `lombok` - Code generation (getters, setters, constructors)
- `jackson-databind` - JSON processing
- `spring-boot-starter-test` - Testing framework

## 7. Commonly Used Annotations

### Spring Framework
- `@SpringBootApplication` - Main application class
- `@RestController` - REST API controller
- `@RequestMapping` - URL mapping
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` - HTTP method mapping
- `@RequestBody` - Request body binding
- `@PathVariable` - URL path variable
- `@RequestParam` - Query parameter
- `@Service` - Service layer component
- `@Repository` - Data access component
- `@Component` - Generic component
- `@Autowired` - Dependency injection
- `@Configuration` - Configuration class
- `@Bean` - Bean definition

### Security
- `@EnableWebSecurity` - Enable Spring Security
- `@PreAuthorize` - Method-level authorization
- `@Secured` - Role-based security
- `@CrossOrigin` - CORS configuration

### Data & Persistence
- `@Entity` - JPA entity
- `@Table` - Database table mapping
- `@Id` - Primary key
- `@GeneratedValue` - Auto-generated ID
- `@Column` - Column mapping
- `@ManyToOne`, `@OneToMany`, `@ManyToMany` - Relationships
- `@JoinColumn` - Foreign key
- `@Transactional` - Transaction management
- `@Query` - Custom JPQL queries

### Validation
- `@NotNull`, `@NotBlank`, `@NotEmpty` - Null/empty validation
- `@Size` - Size validation
- `@Min`, `@Max` - Range validation
- `@Email` - Email format validation
- `@Pattern` - Regex pattern validation

### Lombok
- `@Data` - Generates getters, setters, equals, hashCode, toString
- `@Getter`, `@Setter` - Individual getters/setters
- `@NoArgsConstructor`, `@AllArgsConstructor` - Constructors
- `@Builder` - Builder pattern

## 8. Programming Languages & Technologies

### Backend
- **Java 17** - Primary backend language
- **SQL** - Database queries and migrations
- **Groovy** - Gradle build scripts

### Frontend
- **TypeScript** - Type-safe JavaScript
- **JavaScript (ES6+)** - React components
- **CSS/Tailwind** - Styling
- **HTML** - Markup

### Mobile
- **Kotlin** - Android native code (Capacitor bridge)
- **XML** - Android layouts and resources

### Configuration & Build
- **Maven** - Backend dependency management
- **Gradle** - Android build system
- **YAML** - Application configuration
- **JSON** - Data interchange format

### Database
- **PostgreSQL** - Primary database
- **Flyway/Liquibase** - Database migrations (via Spring)

## 9. Key Features Summary

✓ Multi-role user management with JWT authentication (5 staff roles + member role)
✓ Comprehensive member lifecycle management
✓ Flexible loan product configuration with eligibility rules
✓ Multi-stage loan approval workflow (LOAN_OFFICER → CREDIT_COMMITTEE → TREASURER)
✓ Guarantor validation and capacity tracking with pledge freezing
✓ Real-time account balance management
✓ Bulk Excel processing for members and contributions
✓ Complete audit trail logging for SASRA compliance
✓ Mobile-first member portal via Android APK with splash screen and logo
✓ Responsive web dashboard for staff with role-based access
✓ Comprehensive financial reporting (statements, P&L, cashbook)
✓ SASRA compliance features and audit trail
✓ Notification system for approvals and updates
✓ Document management and KYC tracking
✓ Profit & loss reporting
✓ Transaction history and reconciliation
✓ Loan eligibility calculation accounting for active loans
✓ Conditional savings debit on repayment (payment method dependent)
✓ Automatic guarantor pledge release on full loan repayment
✓ Shares account properly restricted from deposits

## 10. Performance & Scalability

- JWT-based stateless authentication (no session storage)
- Database indexing on frequently queried columns
- Pagination for large data sets
- Lazy loading for related entities
- Connection pooling for database efficiency
- Caching strategies for frequently accessed data
- Bulk operations for high-volume processing
