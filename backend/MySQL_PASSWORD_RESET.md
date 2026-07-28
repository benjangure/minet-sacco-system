# MySQL Password Issue - Quick Fix Guide

The password "0a0b0ccD." is not working with MySQL. Here are your options:

---

## ✅ Option 1: Check Password with MySQL Workbench (Easiest)

1. Open **MySQL Workbench** (if installed)
2. Try connecting with the password "0a0b0ccD."
3. If it works, we need to figure out why command line doesn't work
4. If it doesn't work, you need to reset the password

---

## ✅ Option 2: Reset MySQL Root Password (Recommended)

### Step-by-Step Instructions

**1. Open PowerShell as Administrator**
   - Right-click PowerShell
   - Select "Run as Administrator"

**2. Stop MySQL Service**
```powershell
net stop MySQL80
```

**3. Start MySQL in Safe Mode**
```powershell
# Run this command and LEAVE THE WINDOW OPEN
mysqld --console --skip-grant-tables --skip-networking
```

**4. Open a NEW PowerShell Window (as Administrator)**
```powershell
# Connect to MySQL
mysql -u root

# You should now be in MySQL prompt (mysql>)
# Run these commands one by one:
```

```sql
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'admin123';
FLUSH PRIVILEGES;
EXIT;
```

**5. Stop the Safe Mode MySQL**
   - Go back to the first PowerShell window
   - Press `Ctrl + C` to stop it

**6. Start MySQL Normally**
```powershell
net start MySQL80
```

**7. Test New Password**
```powershell
mysql -u root -p
# Enter password: admin123
```

**8. Update application.properties**
   - Change password to: `admin123`
   - File: `backend/src/main/resources/application.properties`

---

## ✅ Option 3: Try Empty Password (If Using XAMPP)

If you're using XAMPP, try this:

**Update application.properties:**
```properties
spring.datasource.password=
```

Then test:
```powershell
mysql -u root -e "SELECT 1;"
```

---

## ✅ Option 4: Create New MySQL User (Alternative)

If you can't reset root password, create a new user:

**1. If you can access MySQL somehow, run:**
```sql
CREATE USER 'saccodev'@'localhost' IDENTIFIED BY 'admin123';
GRANT ALL PRIVILEGES ON *.* TO 'saccodev'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

**2. Update application.properties:**
```properties
spring.datasource.username=saccodev
spring.datasource.password=admin123
```

---

## 🎯 Recommended Password for Local Development

Use: **`admin123`**

This is:
- Easy to remember
- Simple to type
- Suitable for local development (NOT for production!)

---

## After You Reset the Password

Run these commands:

```powershell
# Navigate to backend folder
cd backend

# Test MySQL connection
mysql -u root -padmin123 -e "SELECT VERSION();"

# Create database
mysql -u root -padmin123 -e "CREATE DATABASE IF NOT EXISTS sacco_db;"

# Start backend
.\mvnw.cmd spring-boot:run
```

---

## 🆘 Still Having Issues?

**Check if MySQL is running:**
```powershell
Get-Service MySQL80
```

**Check MySQL error log:**
```powershell
# Usually located at:
Get-Content "C:\ProgramData\MySQL\MySQL Server 8.0\Data\*.err" -Tail 50
```

**Try connecting via socket:**
```powershell
mysql -u root -p --protocol=TCP
```

---

## Next Steps After Password Reset

1. ✅ Password reset to `admin123`
2. ✅ Update `application.properties` with new password
3. ✅ Create database: `sacco_db`
4. ✅ Run backend: `.\mvnw.cmd spring-boot:run`
5. ✅ System will auto-create all tables via Flyway migrations

