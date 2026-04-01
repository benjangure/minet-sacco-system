# Member App Deployment Guide

## Overview
The member app is a web-based application that members can access through their browser. No APK or app store deployment needed.

## What's Built

### Backend Endpoints (Already Implemented)
- `POST /api/auth/member/login` - Member login with phone/employee ID + national ID
- `GET /api/member/dashboard` - Dashboard summary (savings, shares, loans)
- `GET /api/member/profile` - Member profile information
- `GET /api/member/accounts` - Account balances
- `GET /api/member/transactions` - Transaction history
- `GET /api/member/loans` - All member loans
- `GET /api/member/loans/{id}` - Specific loan details

### Frontend Pages (Already Implemented)
- `/member` - Member login page with APK download link
- `/member/dashboard` - Member dashboard with:
  - Account summary (Savings, Shares, Total)
  - Loan summary (Active loans, Outstanding, Pending)
  - Recent transactions
  - Quick action buttons

## Deployment Steps

### 1. Build the Frontend
```bash
cd minetsacco-main
npm run build
```
This creates the `dist/` folder with all static files.

### 2. Deploy to Your Server

**Option A: Using Node.js**
```bash
# Install serve globally
npm install -g serve

# Serve the dist folder
serve -s dist -l 3000
```

**Option B: Using Nginx**
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        root /path/to/minetsacco-main/dist;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
    }
}
```

**Option C: Using Apache**
```apache
<Directory /path/to/minetsacco-main/dist>
    RewriteEngine On
    RewriteBase /
    RewriteRule ^index\.html$ - [L]
    RewriteCond %{REQUEST_FILENAME} !-f
    RewriteCond %{REQUEST_FILENAME} !-d
    RewriteRule . /index.html [L]
</Directory>

ProxyPass /api http://localhost:8080/api
ProxyPassReverse /api http://localhost:8080/api
```

### 3. Configure Backend API URL

Update the API URL in `minetsacco-main/src/pages/MemberLogin.tsx` and `MemberDashboard.tsx`:

```typescript
const API_BASE_URL = 'https://your-server.com/api';
```

Then rebuild:
```bash
npm run build
```

### 4. Share Member Login Link

Send members this link:
```
https://your-server.com/member
```

Members login with:
- **Username**: Phone number or Employee ID
- **Password**: National ID (initial password)

## Member Access Flow

1. Member receives login link via SMS/email
2. Opens link in browser (works on any device - phone, tablet, desktop)
3. Logs in with phone/employee ID + national ID
4. Accesses dashboard with:
   - Savings account balance
   - Shares balance
   - Active loans
   - Transaction history
   - Loan application status

## Security Features

- JWT token-based authentication
- Role-based access control (MEMBER role only)
- Session validation on each request
- Automatic logout on token expiration
- Secure password handling (national ID as initial password)

## Testing

### Local Testing
```bash
cd minetsacco-main
npm run dev
```
Then visit: `http://localhost:5173/member`

### Production Testing
1. Deploy to your server
2. Test member login with test credentials
3. Verify all dashboard data loads correctly
4. Test on mobile browser (Chrome, Safari)

## Troubleshooting

**Members can't login:**
- Verify backend is running on port 8080
- Check API URL is correct in frontend code
- Verify member exists in database with correct phone/employee ID

**Dashboard shows no data:**
- Check member has accounts/loans in database
- Verify API endpoints are returning data
- Check browser console for errors

**CORS errors:**
- Ensure backend CORS is configured for your domain
- Check `CorsConfig.java` in backend

## Future Enhancements

- Add payment integration (M-Pesa, banks)
- Add loan application form
- Add repayment schedule view
- Add notifications
- Add profile update functionality
