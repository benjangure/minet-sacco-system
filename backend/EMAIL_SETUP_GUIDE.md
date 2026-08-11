# Email Notification System - Setup Guide

## ✅ Configuration Complete

The Minet SACCO email notification system is now configured with:

### SMTP Settings (Office365)
- **Host:** smtp.office365.com
- **Port:** 587
- **Username:** no_reply@minet.co.ke
- **Security:** TLS
- **From Name:** Minet SACCO

### Features

1. **Beautiful Red-Themed Templates**
   - Professional HTML emails with Minet SACCO branding
   - Responsive design that works on all devices
   - Red gradient theme matching company colors
   - Dynamic icons based on notification type

2. **Automatic Email Notifications**
   - Every in-app notification automatically sends an email
   - Emails sent asynchronously (won't slow down the application)
   - Smart subject lines based on notification type

3. **Notification Types Supported**
   - 🏦 Loan approvals and updates
   - 💰 Deposit notifications
   - 💳 Repayment confirmations
   - 👥 Guarantor requests
   - 📊 Bulk processing updates
   - 👤 Member updates
   - ℹ️ General system notifications

## Testing the Email System

### Method 1: Using the Test Page (Easiest)

1. **Start the backend:**
   ```bash
   cd backend
   .\mvnw spring-boot:run
   ```

2. **Open the test page:**
   - Open `backend/test-email.html` in your browser
   - Click "Send Test Email to victorgathecha@gmail.com"
   - Check your inbox!

### Method 2: Using API Directly

**Send Test Email:**
```bash
curl -X POST http://localhost:9090/api/email/test \
  -H "Content-Type: application/json" \
  -d '{"email": "victorgathecha@gmail.com"}'
```

**Send Custom Notification:**
```bash
curl -X POST http://localhost:9090/api/email/notify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "victorgathecha@gmail.com",
    "subject": "Loan Approved!",
    "message": "Your loan application for KES 50,000 has been approved.",
    "type": "LOAN_APPROVAL"
  }'
```

### Method 3: PowerShell

```powershell
# Test Email
Invoke-RestMethod -Uri "http://localhost:9090/api/email/test" -Method Post `
  -ContentType "application/json" `
  -Body '{"email": "victorgathecha@gmail.com"}'

# Custom Email
$body = @{
    email = "victorgathecha@gmail.com"
    subject = "Test Notification"
    message = "This is a test from PowerShell"
    type = "LOAN_APPROVAL"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:9090/api/email/notify" -Method Post `
  -ContentType "application/json" -Body $body
```

## How It Works

### Automatic Integration

The email system is automatically integrated with the notification system:

```java
// When you create a notification, email is sent automatically!
notificationService.notifyUser(
    userId,
    "Your loan has been approved!",
    "LOAN_APPROVAL"
);
// ✅ In-app notification created
// ✅ Email sent to user automatically
```

### Email Template Preview

Emails include:
- **Professional Header:** Minet SACCO logo and tagline
- **Notification Icon:** Dynamic icon based on type
- **Message Content:** Your notification message in a styled box
- **Call-to-Action:** "View in Dashboard" button
- **Information Box:** Notification type and timestamp
- **Footer:** Company information and links

### Files Created

1. **EmailNotificationService.java** - Core email service with templates
2. **EmailTestController.java** - REST API for testing
3. **AsyncConfig.java** - Async email sending configuration
4. **test-email.html** - Interactive test page
5. **application.properties** - Updated with SMTP settings

### Email Subject Lines

The system automatically generates appropriate subject lines:

| Notification Type | Email Subject |
|-------------------|---------------|
| LOAN_APPROVAL | Loan Application Update - Minet SACCO |
| LOAN | Loan Notification - Minet SACCO |
| GUARANTOR | Guarantor Request - Minet SACCO |
| DEPOSIT | Deposit Notification - Minet SACCO |
| REPAYMENT | Repayment Notification - Minet SACCO |
| DEPOSIT_REQUEST | Deposit Request - Minet SACCO |
| DEPOSIT_APPROVED | Deposit Approved - Minet SACCO |
| BULK_PROCESSING | Bulk Processing Update - Minet SACCO |
| Default | Notification from Minet SACCO |

## Troubleshooting

### Emails Not Sending?

1. **Check Backend Logs:**
   Look for "Email sent successfully" or error messages

2. **Verify SMTP Settings:**
   - Username: no_reply@minet.co.ke
   - Password: fhcyvypyydghmyfp
   - Port: 587 (TLS)

3. **Check User Email:**
   Users must have valid email addresses in the database

4. **Test Connection:**
   Use the test-email.html page to send a test email

### Common Issues

**Issue:** "Authentication failed"
- **Solution:** Verify the SMTP username and password are correct

**Issue:** "Connection timeout"
- **Solution:** Check firewall settings, port 587 must be open

**Issue:** Emails go to spam
- **Solution:** This is normal for no-reply addresses. Users should whitelist no_reply@minet.co.ke

## Production Deployment

For production, update these settings in `application-prod.properties`:

```properties
# Update dashboard URL
# Find and replace http://localhost:3000 with your production URL in EmailNotificationService.java

# Consider adding:
spring.mail.properties.mail.smtp.ssl.trust=smtp.office365.com
```

## Support

If you encounter any issues:
1. Check the backend logs for error messages
2. Use the test-email.html page to verify SMTP configuration
3. Ensure the email account no_reply@minet.co.ke is active and accessible

---

**System Status:** ✅ Configured and Ready
**Test Email:** Ready to send to victorgathecha@gmail.com
**Integration:** Automatic with all notifications
