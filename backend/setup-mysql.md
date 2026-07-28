# MySQL Setup Guide for Minet SACCO System

## Quick Setup for Local Development

### Method 1: Reset MySQL Root Password (If you don't know it)

#### Step 1: Stop MySQL Service
```powershell
# Run as Administrator
net stop MySQL80
```

#### Step 2: Start MySQL in Safe Mode
Open PowerShell as Administrator and run:
```powershell
mysqld --skip-grant-tables --skip-networking
```

**Leave this window open!**

#### Step 3: Reset Password (Open NEW PowerShell window)
```powershell
# Connect to MySQL
mysql -u root

# Run these commands in MySQL:
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'admin123';
FLUSH PRIVILEGES;
EXIT;
```

#### Step 4: Restart MySQL Normally
Close the safe mode window (Ctrl+C), then:
```powershell
# Run as Administrator
net start MySQL80
```

#### Step 5: Test Connection
```powershell
mysql -u root -p
# Enter password: admin123
```

---

### Method 2: If You Already Know Your Password

Just update the `application.properties` file:

**File:** `backend/src/main/resources/application.properties`

```properties
spring.datasource.password=YOUR_PASSWORD_HERE
```

---

### Method 3: Create Database Manually (If MySQL Connection Works)

```sql
-- Connect to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE IF NOT EXISTS sacco_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verify
SHOW DATABASES;

-- Exit
EXIT;
```

---

## What Password Should I Use?

For local development, I recommend using **`admin123`** as it's easy to remember.

The application.properties file should have:
```properties
spring.datasource.username=root
spring.datasource.password=admin123
```

---

## Testing the Setup

After setting up MySQL, test the connection:

```powershell
# Test connection
mysql -u root -padmin123 -e "SELECT VERSION();"

# If successful, create the database
mysql -u root -padmin123 -e "CREATE DATABASE IF NOT EXISTS sacco_db;"

# Verify database exists
mysql -u root -padmin123 -e "SHOW DATABASES;" | findstr sacco_db
```

---

## Next Steps

1. ✅ Set MySQL password
2. ✅ Update `application.properties` with the password
3. ✅ Create `sacco_db` database (or let Flyway do it automatically)
4. ✅ Run the backend: `.\mvnw.cmd spring-boot:run`
5. ✅ Backend will automatically run Flyway migrations to create all tables

---

## Common Issues

### "Access Denied" Error
- Double-check password in `application.properties`
- Make sure MySQL service is running: `Get-Service MySQL80`

### "Unknown Database" Error
- Create database manually: `CREATE DATABASE sacco_db;`
- Or ensure `createDatabaseIfNotExist=true` is in the connection URL

### Port 3306 Already in Use
- Stop other MySQL instances
- Or change port in `application.properties`

---

## Current Configuration

**Database URL:** `jdbc:mysql://localhost:3306/sacco_db`
**Username:** `root`
**Password:** (needs to be set in application.properties)

