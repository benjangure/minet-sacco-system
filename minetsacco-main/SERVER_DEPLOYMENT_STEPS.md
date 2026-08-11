# Server Deployment Steps - Minet SACCO

This guide walks you through deploying the Minet SACCO system to your production server. You'll deploy three components: Database, Backend, and Frontend.

## Prerequisites
- Server IP address, username, and password (you have these)
- SSH client (PuTTY, Windows Terminal, or similar)
- The project files ready to deploy

---

## PHASE 1: DATABASE SETUP

### Step 1.1: Connect to Server via SSH
```bash
ssh username@server_ip_address
# Enter password when prompted
```

### Step 1.2: Install MySQL (if not already installed)
```bash
# For Ubuntu/Debian
sudo apt-get update
sudo apt-get install mysql-server -y

# For CentOS/RHEL
sudo yum install mysql-server -y

# Start MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql  # Auto-start on reboot
```

### Step 1.3: Create Database and User
```bash
# Connect to MySQL
mysql -u root -p
# Enter root password

# Inside MySQL prompt, run:
CREATE DATABASE minetsacco;
CREATE USER 'minetsacco_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON minetsacco.* TO 'minetsacco_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Step 1.4: Verify Database Creation
```bash
mysql -u minetsacco_user -p minetsacco
# Enter password - should connect successfully
SHOW TABLES;  # Should be empty (migrations will create tables)
EXIT;
```

---

## PHASE 2: BACKEND DEPLOYMENT

### Step 2.1: Install Java (if not already installed)
```bash
# Check if Java is installed
java -version

# If not installed, install Java 11 or higher
sudo apt-get install openjdk-11-jdk -y  # Ubuntu/Debian
# OR
sudo yum install java-11-openjdk -y  # CentOS/RHEL
```

### Step 2.2: Upload Backend Files to Server
On your local machine, copy the backend folder to the server:
```bash
# From your Windows machine (PowerShell)
scp -r "C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main\backend" username@server_ip_address:/home/username/
```

### Step 2.3: Configure Backend Environment
SSH into server and navigate to backend:
```bash
ssh username@server_ip_address
cd ~/backend
```

### Step 2.4: Update Backend Configuration
Edit `application.properties` to point to your server database:
```bash
nano src/main/resources/application.properties
```

Update these values:
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco
spring.datasource.username=minetsacco_user
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Server Port
server.port=9090

# Flyway (auto-migration)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Save and exit (Ctrl+X, then Y, then Enter)

### Step 2.5: Build Backend
```bash
# Make sure you're in the backend directory
cd ~/backend

# Build the project (this creates a JAR file)
mvn clean package -DskipTests

# This will take 5-10 minutes. Wait for "BUILD SUCCESS"
```

### Step 2.6: Run Backend
```bash
# Navigate to target directory where JAR is created
cd target

# Run the JAR file
java -jar sacco-0.0.1-SNAPSHOT.jar

# You should see logs ending with:
# "Started SaccoApplication in X seconds"
```

**Keep this terminal open** - the backend is now running on port 9090

### Step 2.7: Verify Backend is Running
Open a new SSH terminal and test:
```bash
curl http://localhost:9090/api/auth/health
# Should return: {"status":"UP"}
```

---

## PHASE 3: FRONTEND DEPLOYMENT

### Step 3.1: Install Node.js (if not already installed)
```bash
# Check if Node is installed
node -v

# If not, install Node.js
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify installation
node -v
npm -v
```

### Step 3.2: Upload Frontend Files to Server
On your local machine:
```bash
# From Windows PowerShell
scp -r "C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main\minetsacco-main" username@server_ip_address:/home/username/
```

### Step 3.3: Install Frontend Dependencies
SSH into server:
```bash
ssh username@server_ip_address
cd ~/minetsacco-main

# Install dependencies
npm install

# This will take 5-10 minutes
```

### Step 3.4: Update Frontend API Configuration
Edit the API configuration file:
```bash
nano src/config/api.ts
```

Update the backend URL to point to your server:
```typescript
// Change from localhost to your server IP
const API_BASE_URL = 'http://your_server_ip:9090/api';
// OR if you have a domain
const API_BASE_URL = 'https://api.yourdomain.com/api';
```

Save and exit

### Step 3.5: Build Frontend
```bash
# Build the production version
npm run build

# This creates a 'dist' folder with optimized files
# Wait for "build complete"
```

### Step 3.6: Install and Run Web Server (Nginx)
```bash
# Install Nginx
sudo apt-get install nginx -y

# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx  # Auto-start on reboot
```

### Step 3.7: Configure Nginx to Serve Frontend
```bash
# Edit Nginx configuration
sudo nano /etc/nginx/sites-available/default
```

Replace the entire file with:
```nginx
server {
    listen 80 default_server;
    listen [::]:80 default_server;

    server_name _;

    root /home/username/minetsacco-main/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:9090/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Save and exit

### Step 3.8: Test Nginx Configuration
```bash
sudo nginx -t
# Should output: "test is successful"
```

### Step 3.9: Reload Nginx
```bash
sudo systemctl reload nginx
```

---

## PHASE 4: VERIFICATION & TESTING

### Step 4.1: Verify All Services Running
```bash
# Check if backend is running
curl http://localhost:9090/api/auth/health

# Check if Nginx is running
sudo systemctl status nginx

# Check if MySQL is running
sudo systemctl status mysql
```

### Step 4.2: Test Frontend Access
Open a browser and go to:
```
http://your_server_ip
```

You should see the Minet SACCO login page.

### Step 4.3: Test Member Login
Use test credentials:
- **Username**: 0722123456 (or any member phone number from database)
- **Password**: Member's National ID

### Step 4.4: Test Admin Login
- **Username**: admin
- **Password**: admin123

---

## PHASE 5: PRODUCTION HARDENING (Optional but Recommended)

### Step 5.1: Enable HTTPS (SSL Certificate)
```bash
# Install Certbot for Let's Encrypt
sudo apt-get install certbot python3-certbot-nginx -y

# Get free SSL certificate
sudo certbot --nginx -d yourdomain.com

# Auto-renewal
sudo systemctl enable certbot.timer
```

### Step 5.2: Set Up Firewall
```bash
# Enable UFW firewall
sudo ufw enable

# Allow SSH, HTTP, HTTPS
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Verify rules
sudo ufw status
```

### Step 5.3: Set Up Automatic Backups
```bash
# Create backup script
sudo nano /usr/local/bin/backup-minetsacco.sh
```

Add:
```bash
#!/bin/bash
BACKUP_DIR="/backups/minetsacco"
mkdir -p $BACKUP_DIR
mysqldump -u minetsacco_user -p'your_password' minetsacco > $BACKUP_DIR/db_$(date +%Y%m%d_%H%M%S).sql
```

Make executable and schedule with cron:
```bash
sudo chmod +x /usr/local/bin/backup-minetsacco.sh
sudo crontab -e
# Add: 0 2 * * * /usr/local/bin/backup-minetsacco.sh  (runs daily at 2 AM)
```

---

## TROUBLESHOOTING

### Backend won't start
```bash
# Check logs
tail -f ~/backend/target/logs/spring.log

# Common issues:
# - Database not running: sudo systemctl start mysql
# - Port 9090 in use: sudo lsof -i :9090
# - Wrong database credentials: Check application.properties
```

### Frontend shows blank page
```bash
# Check browser console (F12) for errors
# Common issues:
# - API URL wrong: Check src/config/api.ts
# - Backend not running: Check backend service
# - Nginx not serving files: Check /etc/nginx/sites-available/default
```

### Can't connect to server
```bash
# Check SSH connection
ssh -v username@server_ip_address

# Check firewall
sudo ufw status

# Check if services are running
sudo systemctl status nginx
sudo systemctl status mysql
```

---

## QUICK REFERENCE - RESTART SERVICES

If you need to restart services after deployment:

```bash
# Restart Backend (stop old process first)
pkill -f "java -jar"
cd ~/backend/target
java -jar sacco-0.0.1-SNAPSHOT.jar &

# Restart Frontend
sudo systemctl restart nginx

# Restart Database
sudo systemctl restart mysql
```

---

## NEXT STEPS AFTER DEPLOYMENT

1. **Test all features** - Login, create loans, process payments
2. **Monitor logs** - Check for errors in production
3. **Set up monitoring** - Use tools like Prometheus or New Relic
4. **Regular backups** - Ensure database backups run daily
5. **Security updates** - Keep OS and packages updated

---

## SUPPORT

If you encounter issues:
1. Check the troubleshooting section above
2. Review service logs (see commands above)
3. Verify all three components are running
4. Check network connectivity between components

Good luck with your deployment!
