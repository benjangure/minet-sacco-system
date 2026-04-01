# Minet SACCO Backend - Quick Start Guide

Get the backend running in 5 minutes!

## Prerequisites Check

Before starting, ensure you have:
- ✅ Java JDK 21 installed (`java -version`)
- ✅ Maven 3.9+ installed (`mvn -version`)
- ✅ MySQL running (via XAMPP or standalone)
- ✅ IntelliJ IDEA or VS Code with Java extensions

## Step 1: Start MySQL

### Using XAMPP:
1. Open XAMPP Control Panel
2. Click "Start" for MySQL
3. Verify it's running on port 3306

### Using Standalone MySQL:
```bash
# Windows
net start MySQL80

# Linux/Mac
sudo systemctl start mysql
```

## Step 2: Configure Database (Optional)

The application will auto-create the database. If you want to verify:

```bash
# Login to MySQL
mysql -u root -p

# Check if database exists (optional)
SHOW DATABASES;

# Exit
exit;
```

## Step 3: Run the Application

### Option A: Using IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. Click "Open" and select the `backend` folder
3. Wait for Maven to download dependencies (check bottom right)
4. Find `MinetSaccoBackendApplication.java`
5. Right-click → "Run 'MinetSaccoBackendApplication'"
6. Wait for "Started MinetSaccoBackendApplication" message

### Option B: Using Command Line

```bash
# Navigate to backend folder
cd backend

# Clean and install dependencies
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run
```

## Step 4: Verify It's Running

Open your browser and visit:
```
http://localhost:8080/swagger-ui/index.html
```

You should see the Swagger API documentation page.

## Step 5: Test the API

### Using Swagger UI (Easiest):

1. Go to `http://localhost:8080/swagger-ui/index.html`
2. Find "auth-controller"
3. Click on `POST /api/auth/login`
4. Click "Try it out"
5. Use these credentials:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
6. Click "Execute"
7. Copy the JWT token from the response

### Using cURL:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Copy the token from response, then:
curl -X GET http://localhost:8080/api/members \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Using Postman:

1. Create new request: `POST http://localhost:8080/api/auth/login`
2. Body → raw → JSON:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
3. Send
4. Copy the token
5. Create new request: `GET http://localhost:8080/api/members`
6. Headers → Add: `Authorization: Bearer YOUR_TOKEN`
7. Send

## Default Test Accounts

The system comes with pre-configured test accounts:

| Username | Password | Role | Email |
|----------|----------|------|-------|
| admin | admin123 | ADMIN | admin@minet.co.ke |
| treasurer | admin123 | TREASURER | treasurer@minet.co.ke |
| loan_officer | admin123 | LOAN_OFFICER | loans@minet.co.ke |

## Sample Test Data

The system includes 5 sample members:
- M001 - John Kamau (IT Department)
- M002 - Jane Wanjiku (Finance Department)
- M003 - Peter Ochieng (Operations Department)
- M004 - Mary Akinyi (HR Department)
- M005 - David Mwangi (Sales Department)

Each member has:
- A SAVINGS account with balance
- A SHARES account with balance

4 Loan Products are pre-configured:
- Emergency Loan (12% interest, up to 100K)
- Development Loan (10% interest, up to 5M)
- School Fees Loan (8% interest, up to 500K)
- Asset Financing (11% interest, up to 2M)

## Common API Endpoints

### Authentication
```
POST /api/auth/login - Login and get JWT token
```

### Members
```
GET    /api/members           - List all members
GET    /api/members/{id}      - Get member by ID
POST   /api/members           - Create new member
PUT    /api/members/{id}      - Update member
DELETE /api/members/{id}      - Delete member
```

### Accounts
```
GET  /api/accounts/member/{memberId}  - Get member accounts
POST /api/accounts/deposit             - Make deposit
POST /api/accounts/withdraw            - Make withdrawal
GET  /api/accounts/balance/{memberId}/{accountType} - Check balance
```

### Loans
```
GET  /api/loans                - List all loans
POST /api/loans/apply          - Apply for loan
POST /api/loans/approve        - Approve/reject loan
POST /api/loans/disburse/{id}  - Disburse loan
POST /api/loans/repay          - Make repayment
```

## Troubleshooting

### Port 8080 already in use
```properties
# Edit src/main/resources/application.properties
server.port=8081
```

### Database connection failed
1. Check MySQL is running
2. Verify credentials in `application.properties`
3. Try connecting manually: `mysql -u root -p`

### Dependencies not downloading
```bash
# Clear Maven cache and retry
mvn clean
mvn dependency:purge-local-repository
mvn install
```

### Application won't start
1. Check Java version: `java -version` (must be 21+)
2. Check Maven version: `mvn -version` (must be 3.9+)
3. Check logs in console for specific error
4. Verify MySQL is running

### Flyway migration failed
```bash
# Reset database (DEVELOPMENT ONLY!)
mysql -u root -p
DROP DATABASE sacco_db;
exit

# Restart application - it will recreate everything
```

## Next Steps

1. ✅ Backend is running
2. 📱 Set up the Angular frontend (see `minetsacco-main` folder)
3. 🔗 Connect frontend to backend
4. 🧪 Test the full system
5. 📊 Explore the API documentation

## Need Help?

- 📖 Full documentation: See `README.md`
- 📊 Implementation status: See `IMPLEMENTATION_STATUS.md`
- 🐛 Found a bug? Contact Benjamin Ngure
- 💡 Feature request? Check the blueprint document

## Production Deployment

⚠️ **WARNING:** This setup is for DEVELOPMENT only!

Before deploying to production:
1. Change JWT secret in `application.properties`
2. Use environment variables for sensitive data
3. Enable HTTPS/TLS
4. Configure proper CORS origins
5. Set up database backups
6. Enable monitoring and logging
7. Review security checklist in `IMPLEMENTATION_STATUS.md`

---

**Happy Coding! 🚀**
