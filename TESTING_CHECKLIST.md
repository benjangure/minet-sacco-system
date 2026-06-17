# Member Credentials Implementation - Testing Checklist

## Pre-Testing Setup
- [ ] Backend is running on `http://localhost:8080`
- [ ] Frontend is running on `http://localhost:3000` (or configured port)
- [ ] Database migrations have completed (V125 and V126)
- [ ] User is logged in as ADMIN, TREASURER, or CUSTOMER_SUPPORT

## Database Verification
- [ ] Password column exists in `member_credentials` table
  ```sql
  DESCRIBE member_credentials;
  ```
- [ ] Column is nullable VARCHAR(255)
- [ ] Index `idx_member_credentials_password` exists

## Backend API Testing

### 1. Get All Credentials
```
GET http://localhost:8080/api/member-credentials
Headers: Authorization: Bearer {token}
Expected: 200 OK with list of credentials
```
- [ ] Status: 200
- [ ] Contains member data
- [ ] Includes password field (if not changed)

### 2. Get Single Credential
```
GET http://localhost:8080/api/member-credentials/1
Headers: Authorization: Bearer {token}
Expected: 200 OK with credential object
```
- [ ] Status: 200
- [ ] Contains username, memberName, email
- [ ] Contains password if not changed

### 3. Get Password Endpoint
```
GET http://localhost:8080/api/member-credentials/1/password
Headers: Authorization: Bearer {token}
Expected: 200 OK with password object
```
- [ ] Status: 200
- [ ] Returns: { password: "..." }
- [ ] Or returns: { password: null } if changed

### 4. Search Credentials
```
GET http://localhost:8080/api/member-credentials/search?query=John
Headers: Authorization: Bearer {token}
Expected: 200 OK with filtered results
```
- [ ] Status: 200
- [ ] Filters by memberName or username
- [ ] Results contain matching credentials

### 5. Role-Based Access Control
```
GET http://localhost:8080/api/member-credentials
Headers: Bearer {member-token}
Expected: 403 Forbidden
```
- [ ] MEMBER role gets 403
- [ ] ADMIN/TREASURER/CUSTOMER_SUPPORT get 200

## Frontend Testing

### 1. Navigation
- [ ] Can navigate to Member Credentials page from sidebar
- [ ] Page title shows "Member Credentials Dashboard"
- [ ] Info alert displays about credentials

### 2. Initial Load
- [ ] Page loads credentials from API
- [ ] Shows loading spinner while fetching
- [ ] Displays table with columns:
  - [ ] Member Name
  - [ ] Username
  - [ ] Email
  - [ ] Password Type (National ID / Generated)
  - [ ] Status (Pending Delivery / Email Sent / Password Changed)
  - [ ] Created
  - [ ] Actions

### 3. Search Functionality
- [ ] Type member name in search box
- [ ] Click Search button
- [ ] Results filter correctly
- [ ] Can search by username
- [ ] Can search by email

### 4. View Credentials Modal
- [ ] Click eye icon on table row
- [ ] Modal opens with credential details
- [ ] Shows Member Name
- [ ] Shows Email
- [ ] Shows Username input field
- [ ] Shows Password field (if not changed by member)
- [ ] Shows Status info

### 5. Copy to Clipboard
- [ ] Click Copy icon next to Username
- [ ] Toast shows "Username copied to clipboard"
- [ ] Icon changes to checkmark temporarily
- [ ] Actual clipboard contains username

### 6. Copy Password
- [ ] Click Copy icon next to Password
- [ ] Toast shows "Password copied to clipboard"
- [ ] Icon changes to checkmark temporarily
- [ ] Actual clipboard contains password

### 7. Show/Hide Password
- [ ] Password field shows masked dots (••••••)
- [ ] Click eye icon to show password
- [ ] Password becomes visible
- [ ] Click eye-off icon to hide
- [ ] Password returns to masked view

### 8. Password Changed Status
- [ ] For members who changed password
- [ ] Modal shows: "Password has been changed by the member..."
- [ ] Password field is empty/null
- [ ] No copy button visible for password
- [ ] Explanation alert shows in red

### 9. Error Handling
- [ ] If session expires, shows "Your session has expired"
- [ ] If 401 error, redirects to login
- [ ] If network error, shows error toast
- [ ] Can retry after error

## Integration Testing

### 1. Individual Member Registration Flow
- [ ] Create member WITHOUT National ID
  - [ ] Navigate to Members page
  - [ ] Click Add Member
  - [ ] Fill form (leave National ID empty)
  - [ ] Save member
  - [ ] Modal shows credentials with temp password
  - [ ] Go to Credentials Dashboard
  - [ ] Find new member in list
  - [ ] View password in modal
  - [ ] [ ] Password is visible and matches modal

### 2. Bulk Upload Flow
- [ ] Upload Excel file with members
- [ ] Bulk processing completes
- [ ] Go to Credentials Dashboard
- [ ] All members from bulk upload appear
- [ ] Can view passwords for each
- [ ] Passwords match generated values

### 3. National ID Password Flow
- [ ] Create member WITH National ID (e.g., "12345678")
- [ ] View in Credentials Dashboard
- [ ] Click to view credentials
- [ ] Password Type shows "National ID"
- [ ] Password field shows the National ID value

### 4. End-to-End Member Flow
- [ ] Create member via UI
- [ ] Admin views credentials in dashboard
- [ ] Admin copies password
- [ ] Member logs in with distributed credentials
- [ ] Member changes password
- [ ] Go back to dashboard
- [ ] Password no longer visible for that member
- [ ] Shows "Password has been changed" message

## Performance Testing

### 1. Dashboard Load Time
- [ ] Dashboard loads within 2 seconds
- [ ] Handles 100+ credentials
- [ ] Search remains responsive

### 2. Large List Handling
- [ ] Scrolling is smooth
- [ ] No lag when filtering
- [ ] Modal opens quickly

## Security Testing

### 1. Token Validation
- [ ] With invalid token: 401 Unauthorized
- [ ] With expired token: 401 Unauthorized
- [ ] With missing token: 401 Unauthorized

### 2. Role-Based Access
- [ ] MEMBER role: Cannot access credentials
- [ ] LOAN_OFFICER role: Cannot access credentials
- [ ] ADMIN role: Can access all
- [ ] TREASURER role: Can access all
- [ ] CUSTOMER_SUPPORT role: Can access all

### 3. Password Visibility
- [ ] Passwords only shown if not changed
- [ ] Cannot retrieve new password after change
- [ ] Cannot access password via direct API call if changed

## Browser Compatibility
- [ ] Chrome
- [ ] Firefox
- [ ] Safari
- [ ] Edge

## Responsive Design
- [ ] Desktop (1920x1080)
- [ ] Tablet (768x1024)
- [ ] Mobile (375x667)
- [ ] Table scrolls horizontally on mobile
- [ ] Modal is readable on all sizes

## Final Sign-Off
- [ ] All tests passed
- [ ] No console errors
- [ ] No unhandled promise rejections
- [ ] Database queries are optimized
- [ ] No memory leaks observed
- [ ] Ready for production deployment

## Known Issues / Notes
```
(Add any known issues or notes here)
```

## Deployment Checklist
- [ ] Backend compiled without errors
- [ ] Frontend builds successfully
- [ ] All migrations included in build
- [ ] Environment variables configured
- [ ] Database backups created
- [ ] Deployment ready
