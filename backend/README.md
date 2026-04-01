# Minet SACCO Backend

Spring Boot REST API for the Minet SACCO Management System.

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **MySQL 8.x**
- **Spring Security + JWT**
- **Flyway** (Database Migrations)
- **Maven** (Build Tool)

## Prerequisites

1. Java JDK 21 or higher
2. Maven 3.9+
3. MySQL 8.x (via XAMPP or standalone)
4. IntelliJ IDEA or any Java IDE

## Setup Instructions

### 1. Database Setup

Start MySQL via XAMPP or standalone installation. The application will automatically create the `sacco_db` database on first run.

### 2. Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sacco_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

### 3. Build and Run

```bash
# Clean and install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### 4. API Documentation

Once running, access Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

## Project Structure

```
backend/
├── src/main/java/com/minet/sacco/
│   ├── controller/       # REST API endpoints
│   ├── service/          # Business logic
│   ├── repository/       # Data access layer
│   ├── entity/           # JPA entities
│   ├── dto/              # Data transfer objects
│   ├── security/         # JWT & Spring Security
│   ├── config/           # Configuration classes
│   └── exception/        # Exception handlers
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/     # Flyway SQL scripts
└── pom.xml
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login (returns JWT token)

### Members
- `GET /api/members` - Get all members
- `GET /api/members/{id}` - Get member by ID
- `POST /api/members` - Create new member
- `PUT /api/members/{id}` - Update member
- `DELETE /api/members/{id}` - Delete member

### Accounts
- `GET /api/accounts` - Get all accounts
- `GET /api/accounts/member/{memberId}` - Get member accounts
- `POST /api/accounts/deposit` - Process deposit
- `POST /api/accounts/withdraw` - Process withdrawal
- `GET /api/accounts/balance/{memberId}/{accountType}` - Get balance

### Loans
- `GET /api/loans` - Get all loans
- `GET /api/loans/{id}` - Get loan by ID
- `POST /api/loans/apply` - Apply for loan
- `POST /api/loans/approve` - Approve/reject loan
- `POST /api/loans/disburse/{loanId}` - Disburse loan
- `POST /api/loans/repay` - Make loan repayment
- `GET /api/loans/{loanId}/outstanding` - Get outstanding balance

### Loan Products
- `GET /api/loan-products` - Get all loan products
- `POST /api/loan-products` - Create loan product (Admin only)

### Users
- `GET /api/users` - Get all users (Admin only)
- `POST /api/users` - Create user (Admin only)
- `PUT /api/users/{id}` - Update user (Admin only)

## User Roles & Permissions

The system supports the following roles:

1. **ADMIN** - Full system access
2. **TREASURER** - Financial operations, deposits, withdrawals
3. **LOAN_OFFICER** - Loan processing and management
4. **CREDIT_COMMITTEE** - Loan approval/rejection
5. **AUDITOR** - Read-only access for auditing
6. **TELLER** - Member registration, deposits
7. **CUSTOMER_SUPPORT** - Member queries and support

## Security

- All endpoints (except `/api/auth/login`) require JWT authentication
- JWT tokens expire after 30 minutes (configurable)
- Passwords are encrypted using BCrypt
- Role-based access control (RBAC) enforced at method level
- Audit logging for all critical operations

## Database Schema

The application uses Flyway for database migrations. The initial schema includes:

- `users` - Staff user accounts
- `members` - SACCO members
- `accounts` - Member savings/shares accounts
- `transactions` - All financial transactions
- `loans` - Loan applications and records
- `loan_products` - Loan product definitions
- `loan_repayments` - Loan repayment records
- `guarantors` - Loan guarantor records
- `audit_logs` - System audit trail
- `notifications` - SMS/Email notifications

## Testing the API

### 1. Login to get JWT token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

### 2. Use the token in subsequent requests

```bash
curl -X GET http://localhost:8080/api/members \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Development Notes

- The JWT secret in `application.properties` should be changed in production
- Database credentials should be externalized using environment variables
- Enable HTTPS/TLS in production
- Configure proper CORS origins for production
- Set up proper logging and monitoring

## Troubleshooting

### Port 8080 already in use
Change the port in `application.properties`:
```properties
server.port=8081
```

### Database connection failed
- Ensure MySQL is running
- Verify database credentials
- Check firewall settings

### Flyway migration failed
```bash
# Reset database (development only)
mvn flyway:clean
mvn flyway:migrate
```

## Production Deployment

For production deployment:

1. Update `application.properties` with production database
2. Change JWT secret to a strong random value
3. Enable HTTPS
4. Configure proper CORS origins
5. Set up database backups
6. Enable application monitoring
7. Use environment variables for sensitive data

## Support

For issues or questions, contact the Minet Technology Team.
