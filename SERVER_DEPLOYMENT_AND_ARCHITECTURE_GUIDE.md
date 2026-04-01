# Server Deployment & Architecture Guide

## 1. How Servers Work (Brief Explanation)

### What is a Server?

A server is a powerful computer that:
- Runs 24/7 (always on)
- Stores your application code and data
- Responds to requests from users
- Has a public IP address (like a phone number for the internet)
- Is connected to the internet permanently

### Server vs Your Local Computer

```
Your Local Computer (Development)
├── Runs when you turn it on
├── Private IP (only accessible on your network)
├── Used for testing and development
└── Limited resources

Production Server (Live)
├── Runs 24/7
├── Public IP address (accessible from anywhere)
├── Used by real users
└── High resources (CPU, RAM, Storage)
```

### How Requests Work

```
User's Phone/Browser
    ↓
Internet
    ↓
Server (Your Application)
    ↓
Database (MySQL)
    ↓
Response sent back to user
```

### Server Components

```
Production Server
├── Operating System (Linux, Windows)
├── Java Runtime (JVM)
├── Spring Boot Application (Your Backend)
├── MySQL Database
├── Web Server (Nginx/Apache) - Optional
├── SSL Certificate (HTTPS)
└── Firewall & Security
```

---

## 2. Your Current Setup (Development)

### Local Development Environment

```
Your Computer
├── Backend (Spring Boot)
│   └── Running on localhost:8080
├── Frontend (React)
│   └── Running on localhost:3000
└── Database (MySQL)
    └── Running on localhost:3306
```

**Access:**
- Only you can access it
- Only on your computer or local network
- Not accessible from the internet

---

## 3. Production Server Setup

### What You'll Get

The server they provide will have:
- **Operating System**: Linux (Ubuntu/CentOS) or Windows Server
- **Java**: Java 21 JDK installed
- **MySQL**: Database server installed
- **Public IP Address**: e.g., `203.45.67.89`
- **Domain Name**: e.g., `sacco.example.com` (optional)
- **Storage**: Hard drive space for data
- **RAM**: Memory for running applications
- **CPU**: Processing power

### Server Architecture

```
Production Server (e.g., 203.45.67.89)
├── Port 80 (HTTP)
├── Port 443 (HTTPS/SSL)
├── Port 8080 (Backend API)
├── Port 3000 (Frontend - optional)
└── Port 3306 (MySQL - internal only)

Users Access:
├── Web: https://sacco.example.com (Port 443)
├── API: https://sacco.example.com/api (Port 443)
└── Mobile: https://sacco.example.com/api (Port 443)
```

---

## 4. Deployment Process

### Step 1: Prepare Your Code

**Backend:**
```bash
# Build the JAR file
cd backend
mvn clean package

# Output: target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

**Frontend:**
```bash
# Build the React app
cd minetsacco-main
npm run build

# Output: dist/ folder with static files
```

### Step 2: Connect to Server

**Using SSH (Secure Shell):**
```bash
# Connect to server
ssh username@203.45.67.89

# Or with domain name
ssh username@sacco.example.com

# You'll be prompted for password
# Then you're inside the server terminal
```

**What SSH is:**
- Secure way to access server remotely
- Like remote desktop but command-line
- Encrypted connection
- You get a terminal to run commands

### Step 3: Upload Files to Server

**Option A: Using SCP (Secure Copy)**
```bash
# Copy backend JAR to server
scp backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar \
    username@203.45.67.89:/home/username/app/

# Copy frontend files to server
scp -r minetsacco-main/dist/* \
    username@203.45.67.89:/var/www/html/
```

**Option B: Using Git**
```bash
# On server, clone your repository
ssh username@203.45.67.89
git clone https://github.com/yourname/minet-sacco.git
cd minet-sacco
```

**Option C: Using FTP/SFTP**
- Use FileZilla or WinSCP
- Drag and drop files
- More user-friendly

### Step 4: Set Up Database

**On the server:**
```bash
# Connect to MySQL
mysql -u root -p

# Create database
CREATE DATABASE minet_sacco;

# Create user
CREATE USER 'sacco_user'@'localhost' IDENTIFIED BY 'strong_password';

# Grant permissions
GRANT ALL PRIVILEGES ON minet_sacco.* TO 'sacco_user'@'localhost';
FLUSH PRIVILEGES;
```

**Flyway will automatically run migrations:**
- When backend starts, Flyway runs all SQL migrations
- Creates tables automatically
- No manual SQL needed

### Step 5: Configure Application

**Create application.properties on server:**
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/minet_sacco
spring.datasource.username=sacco_user
spring.datasource.password=strong_password

# Server Configuration
server.port=8080
server.servlet.context-path=/api

# JWT Configuration
jwt.secret=your-secret-key-here
jwt.expiration=86400000

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# File Upload
file.upload.dir=/var/uploads/

# Logging
logging.level.root=INFO
logging.level.com.minet.sacco=DEBUG
```

### Step 6: Start Backend

**On the server:**
```bash
# Navigate to app directory
cd /home/username/app/

# Run the JAR file
java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar

# Or run in background
nohup java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Check if running
curl http://localhost:8080/api/health
```

### Step 7: Deploy Frontend

**Option A: Using Nginx (Recommended)**
```bash
# Install Nginx
sudo apt-get install nginx

# Copy frontend files
sudo cp -r dist/* /var/www/html/

# Configure Nginx to proxy API requests
# Edit /etc/nginx/sites-available/default
```

**Nginx Configuration:**
```nginx
server {
    listen 80;
    server_name sacco.example.com;

    # Serve frontend
    location / {
        root /var/www/html;
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to backend
    location /api {
        proxy_pass http://localhost:8080/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**Option B: Using Apache**
```bash
# Install Apache
sudo apt-get install apache2

# Enable modules
sudo a2enmod proxy
sudo a2enmod rewrite

# Copy frontend files
sudo cp -r dist/* /var/www/html/
```

### Step 8: Set Up HTTPS (SSL Certificate)

**Using Let's Encrypt (Free):**
```bash
# Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# Get certificate
sudo certbot certonly --nginx -d sacco.example.com

# Auto-renew
sudo systemctl enable certbot.timer
```

**Nginx with HTTPS:**
```nginx
server {
    listen 443 ssl;
    server_name sacco.example.com;

    ssl_certificate /etc/letsencrypt/live/sacco.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/sacco.example.com/privkey.pem;

    # Rest of configuration...
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name sacco.example.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 5. Complete Deployment Checklist

### Pre-Deployment
- [ ] Build backend JAR: `mvn clean package`
- [ ] Build frontend: `npm run build`
- [ ] Test locally: Backend on 8080, Frontend on 3000
- [ ] Get server credentials (IP, username, password)
- [ ] Get domain name (if available)

### On Server Setup
- [ ] SSH into server
- [ ] Install Java 21: `sudo apt-get install openjdk-21-jdk`
- [ ] Install MySQL: `sudo apt-get install mysql-server`
- [ ] Install Nginx: `sudo apt-get install nginx`
- [ ] Create application directory: `mkdir -p /home/username/app`
- [ ] Create database and user

### Deployment
- [ ] Upload backend JAR to server
- [ ] Upload frontend files to server
- [ ] Create application.properties file
- [ ] Start backend: `java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar`
- [ ] Configure Nginx
- [ ] Set up SSL certificate
- [ ] Test API: `curl https://sacco.example.com/api/health`
- [ ] Test frontend: Open browser to `https://sacco.example.com`

### Post-Deployment
- [ ] Monitor logs: `tail -f app.log`
- [ ] Check database: `mysql -u sacco_user -p minet_sacco`
- [ ] Test login functionality
- [ ] Test member operations
- [ ] Set up backups
- [ ] Set up monitoring/alerts

---

## 6. Server Architecture Diagram

```
Internet Users
    ↓
Domain Name (sacco.example.com)
    ↓
Nginx Web Server (Port 443 - HTTPS)
    ├── Serves Frontend (React)
    └── Proxies API requests to Backend
    ↓
Spring Boot Backend (Port 8080)
    ├── Handles API requests
    ├── Business logic
    └── Authentication
    ↓
MySQL Database (Port 3306)
    └── Stores all data
```

---

## 7. Connecting Your System to Production Server

### Step-by-Step Connection

**1. Get Server Details**
```
Server IP: 203.45.67.89 (example)
Username: admin
Password: ****
Domain: sacco.example.com (optional)
```

**2. SSH Connection**
```bash
ssh admin@203.45.67.89
# Enter password
# You're now on the server
```

**3. Check Server Status**
```bash
# Check Java
java -version

# Check MySQL
mysql --version

# Check disk space
df -h

# Check memory
free -h
```

**4. Create Application Directory**
```bash
mkdir -p /home/admin/minet-sacco
cd /home/admin/minet-sacco
```

**5. Upload Your Code**
```bash
# From your local computer
scp -r backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar \
    admin@203.45.67.89:/home/admin/minet-sacco/

scp -r minetsacco-main/dist/* \
    admin@203.45.67.89:/var/www/html/
```

**6. Configure Database**
```bash
# On server
mysql -u root -p
CREATE DATABASE minet_sacco;
CREATE USER 'sacco'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON minet_sacco.* TO 'sacco'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**7. Create Configuration File**
```bash
# On server
cat > /home/admin/minet-sacco/application.properties << EOF
spring.datasource.url=jdbc:mysql://localhost:3306/minet_sacco
spring.datasource.username=sacco
spring.datasource.password=password123
server.port=8080
EOF
```

**8. Start Backend**
```bash
cd /home/admin/minet-sacco
java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

**9. Configure Nginx**
```bash
# Edit Nginx config
sudo nano /etc/nginx/sites-available/default

# Add configuration (see section 4)
# Test: sudo nginx -t
# Restart: sudo systemctl restart nginx
```

**10. Access Your System**
```
Web: https://sacco.example.com
API: https://sacco.example.com/api
```

---

## 8. Monitoring & Maintenance

### Check Backend Status
```bash
# Check if running
ps aux | grep java

# View logs
tail -f /home/admin/minet-sacco/app.log

# Check API health
curl https://sacco.example.com/api/health
```

### Database Backup
```bash
# Backup database
mysqldump -u sacco -p minet_sacco > backup.sql

# Restore database
mysql -u sacco -p minet_sacco < backup.sql
```

### Update Application
```bash
# Build new version locally
mvn clean package

# Upload new JAR
scp backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar \
    admin@203.45.67.89:/home/admin/minet-sacco/

# On server, restart
pkill -f "java -jar"
java -jar /home/admin/minet-sacco/minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

---

## 9. Common Issues & Solutions

### Issue: Backend won't start
```bash
# Check Java is installed
java -version

# Check port 8080 is available
netstat -tuln | grep 8080

# Check logs
tail -f app.log
```

### Issue: Can't connect to database
```bash
# Check MySQL is running
sudo systemctl status mysql

# Check credentials
mysql -u sacco -p -h localhost

# Check database exists
mysql -u sacco -p -e "SHOW DATABASES;"
```

### Issue: Frontend not loading
```bash
# Check Nginx is running
sudo systemctl status nginx

# Check files are in right place
ls -la /var/www/html/

# Check Nginx config
sudo nginx -t
```

### Issue: SSL certificate error
```bash
# Check certificate
sudo certbot certificates

# Renew certificate
sudo certbot renew

# Check Nginx SSL config
sudo nginx -t
```

---

## 10. Security Best Practices

### Firewall Configuration
```bash
# Allow SSH
sudo ufw allow 22

# Allow HTTP
sudo ufw allow 80

# Allow HTTPS
sudo ufw allow 443

# Enable firewall
sudo ufw enable
```

### Database Security
```bash
# Change root password
mysql -u root -p

# Remove anonymous users
DELETE FROM mysql.user WHERE User='';

# Remove remote root login
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');

# Flush privileges
FLUSH PRIVILEGES;
```

### Application Security
- Use strong passwords
- Enable HTTPS only
- Keep Java updated
- Keep MySQL updated
- Regular backups
- Monitor logs for suspicious activity

---

## 11. Performance Optimization

### Database Optimization
```bash
# Add indexes
mysql> CREATE INDEX idx_member_status ON members(status);
mysql> CREATE INDEX idx_loan_member ON loans(member_id);
```

### Backend Optimization
```properties
# Increase heap memory
-Xmx2g -Xms1g

# Enable caching
spring.cache.type=caffeine
```

### Frontend Optimization
- Minify CSS/JS
- Compress images
- Enable gzip compression in Nginx
- Use CDN for static files

---

## 12. Scaling for Growth

### As Users Grow

**Week 1-4:**
- Single server setup (what we described)
- Works for 100-500 users

**Month 2-3:**
- Add load balancer
- Multiple backend instances
- Separate database server

**Month 4+:**
- Database replication
- Caching layer (Redis)
- CDN for frontend
- Monitoring and alerting

---

## Summary

**How servers work:**
- Always-on computers with public IP addresses
- Run your application 24/7
- Respond to user requests
- Store data in databases

**How to connect your system:**
1. Get server credentials
2. SSH into server
3. Install Java, MySQL, Nginx
4. Upload your code
5. Configure database
6. Start backend
7. Configure Nginx
8. Set up HTTPS
9. Access via domain name

**Key difference from local:**
- Local: Only you can access, runs when you want
- Server: Everyone can access, runs 24/7

Ready to deploy when you get the server!
