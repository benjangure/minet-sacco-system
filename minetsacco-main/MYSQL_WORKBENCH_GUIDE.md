# MySQL Workbench Guide - Minet SACCO Setup

MySQL Workbench is a visual tool for managing MySQL databases. This guide shows you how to use it to set up the Minet SACCO database.

---

## Step 1: Open MySQL Workbench

1. On the server, click **Start Menu**
2. Search for **MySQL Workbench**
3. Click to open it
4. Wait for it to load (first time may take a moment)

---

## Step 2: Create a Connection to MySQL

### If you see the Welcome screen:

1. Click **MySQL Connections** section
2. Click the **+** icon to add a new connection
3. Fill in the connection details:
   - **Connection Name**: `Local MySQL`
   - **Connection Method**: `Standard (TCP/IP)`
   - **Hostname**: `localhost`
   - **Port**: `3306`
   - **Username**: `root`
   - **Password**: Leave blank for now (we'll enter it when connecting)
4. Click **Test Connection**
5. Enter your MySQL root password when prompted
6. Click **OK** if connection is successful
7. Click **OK** to save the connection

### If connection already exists:

1. Look for a box labeled **Local instance MySQL80** or similar
2. Click it to connect
3. Enter your root password if prompted

---

## Step 3: Connect to MySQL

1. Double-click the connection you just created (or the existing one)
2. Enter your MySQL root password
3. Click **OK**
4. You should now see the MySQL Workbench interface with databases listed on the left

---

## Step 4: Create the Database

### Method 1: Using the GUI (Easiest)

1. In the left panel, right-click on **Schemas** (or the database list area)
2. Select **Create Schema**
3. In the dialog that appears:
   - **Name**: `minetsacco`
   - **Charset**: `utf8mb4`
   - **Collation**: `utf8mb4_unicode_ci`
4. Click **Apply**
5. Click **Apply** again on the next screen
6. Click **Finish**
7. You should now see `minetsacco` in the Schemas list on the left

### Method 2: Using SQL (Alternative)

1. At the top, click **File** → **New Query Tab** (or press `Ctrl+T`)
2. A blank SQL editor will open
3. Copy and paste this SQL:
```sql
CREATE DATABASE minetsacco
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```
4. Click the **Execute** button (lightning bolt icon) or press `Ctrl+Enter`
5. You should see "Query executed successfully" at the bottom
6. The database `minetsacco` will appear in the Schemas list

---

## Step 5: Create the Database User

1. Click **File** → **New Query Tab** (or press `Ctrl+T`)
2. Copy and paste this SQL:
```sql
CREATE USER 'sacco_user'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON minetsacco.* TO 'sacco_user'@'localhost';
FLUSH PRIVILEGES;
```

**Important:** Replace `YourStrongPassword123!` with your strong password. Save this password securely.

3. Click the **Execute** button (lightning bolt icon) or press `Ctrl+Enter`
4. You should see "Query executed successfully" at the bottom

---

## Step 6: Verify the Setup

### Check if database exists:

1. Click **File** → **New Query Tab**
2. Copy and paste:
```sql
SHOW DATABASES;
```
3. Click **Execute**
4. You should see `minetsacco` in the results

### Check if user exists:

1. Click **File** → **New Query Tab**
2. Copy and paste:
```sql
SELECT User, Host FROM mysql.user WHERE User='sacco_user';
```
3. Click **Execute**
4. You should see `sacco_user` and `localhost` in the results

### Check user permissions:

1. Click **File** → **New Query Tab**
2. Copy and paste:
```sql
SHOW GRANTS FOR 'sacco_user'@'localhost';
```
3. Click **Execute**
4. You should see the permissions granted to the user

---

## Step 7: Test the New User Connection

1. In the left panel, right-click on **MySQL Connections**
2. Select **New Connection**
3. Fill in:
   - **Connection Name**: `Sacco User Connection`
   - **Connection Method**: `Standard (TCP/IP)`
   - **Hostname**: `localhost`
   - **Port**: `3306`
   - **Username**: `sacco_user`
   - **Password**: Leave blank (we'll enter it when connecting)
4. Click **Test Connection**
5. Enter the password you created for `sacco_user`
6. Click **OK**
7. If successful, you'll see "Connection test successful"
8. Click **OK** to save

---

## Step 8: View Your Database

1. Double-click the `sacco_user` connection you just created
2. Enter the password when prompted
3. In the left panel, expand **Schemas**
4. You should see `minetsacco` database
5. Expand `minetsacco` to see its tables (currently empty, will be populated when backend runs)

---

## Common Tasks in MySQL Workbench

### View all tables in a database:
1. Expand the database name in the left panel
2. Expand **Tables**
3. You'll see all tables listed

### View data in a table:
1. Right-click on a table name
2. Select **Select Rows - Limit 1000**
3. The data will appear in the bottom panel

### Run a custom SQL query:
1. Click **File** → **New Query Tab**
2. Type your SQL query
3. Click **Execute** (lightning bolt icon) or press `Ctrl+Enter`
4. Results appear below

### Backup the database:
1. Right-click on the database name (`minetsacco`)
2. Select **Dump SQL File**
3. Choose where to save the backup file
4. Click **Save**

### Restore from backup:
1. Click **File** → **Open SQL Script**
2. Select your backup file
3. Click **Open**
4. Click **Execute** to restore

---

## Troubleshooting

### "Access denied for user 'root'@'localhost'"
- You entered the wrong password
- Click **Cancel** and try again with the correct password

### "Can't connect to MySQL server"
- MySQL service is not running
- Open **Services** (services.msc) and start **MySQL80**
- Try connecting again

### "Database already exists"
- The database was already created
- This is fine, you can proceed to the next step

### "User already exists"
- The user was already created
- This is fine, you can proceed to the next step

---

## Next Steps

Once you've completed this setup:

1. Your database `minetsacco` is ready
2. Your user `sacco_user` has full access to the database
3. You can now proceed with building and deploying the backend
4. When the backend runs, it will automatically create all tables using Flyway migrations

---

## Quick Reference

**Connection Details:**
- Hostname: `localhost`
- Port: `3306`
- Database: `minetsacco`
- Username: `sacco_user`
- Password: (the one you created)

**Useful SQL Commands:**
```sql
-- Show all databases
SHOW DATABASES;

-- Show all users
SELECT User, Host FROM mysql.user;

-- Show user permissions
SHOW GRANTS FOR 'sacco_user'@'localhost';

-- Show all tables in database
USE minetsacco;
SHOW TABLES;

-- Count rows in a table
SELECT COUNT(*) FROM table_name;

-- Backup database
mysqldump -u sacco_user -p minetsacco > backup.sql

-- Restore database
mysql -u sacco_user -p minetsacco < backup.sql
```

---

## Summary

You've successfully:
- ✓ Opened MySQL Workbench
- ✓ Created a connection to MySQL
- ✓ Created the `minetsacco` database
- ✓ Created the `sacco_user` user with proper permissions
- ✓ Verified the setup

**Your database is now ready for the backend deployment!**
