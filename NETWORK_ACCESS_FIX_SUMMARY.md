# Network Access Fix Summary

## Problem
When accessing the application from network IP `http://10.39.60.15:8090`, the frontend was calling `http://localhost:9090/api/*` instead of `http://10.39.60.15:9090/api/*`, causing CORS errors.

## Root Cause
**23 files** had hardcoded API URLs:
```typescript
const API_BASE_URL = "http://localhost:9090/api";
```

This meant the API endpoint was hardcoded at build time instead of being dynamically determined based on where the user accesses the application from.

## Solution
Replaced all hardcoded URLs with dynamic calls to `getApiBaseUrl()` from the centralized `api.ts` configuration file:

```typescript
import { getApiBaseUrl } from '@/config/api';
const API_BASE_URL = getApiBaseUrl();
```

The `getApiBaseUrl()` function dynamically returns:
- `http://localhost:9090/api` when accessed from `http://localhost:8090`
- `http://10.39.60.15:9090/api` when accessed from `http://10.39.60.15:8090`

## Files Fixed (23 total)

### Components (2)
1. `src/components/DocumentUpload.tsx`
2. `src/components/KycDocumentUpload.tsx`

### Pages (21)
3. `src/pages/AuditReports.tsx`
4. `src/pages/BulkProcessing.tsx`
5. `src/pages/CustomerSupportPortal.tsx`
6. `src/pages/FundConfiguration.tsx`
7. `src/pages/GuarantorApprovals.tsx`
8. `src/pages/Index.tsx` (Dashboard)
9. `src/pages/KycApproval.tsx`
10. `src/pages/KycDocumentUpload.tsx`
11. `src/pages/KycUploadTracking.tsx`
12. `src/pages/LoanEligibilityRules.tsx`
13. `src/pages/LoanProducts.tsx`
14. `src/pages/LoanRepaymentRecording.tsx`
15. `src/pages/Members.tsx`
16. `src/pages/MemberTransactionHistory.tsx`
17. `src/pages/ProfitLossReport.tsx`
18. `src/pages/Reports.tsx`
19. `src/pages/Savings.tsx`
20. `src/pages/Settings.tsx`
21. `src/pages/TellerMemberContext.tsx`
22. `src/pages/UserManagement.tsx`
23. `src/pages/ViewMemberDocuments.tsx`

## Deployment Steps

### 1. On Local Machine (Already Completed)
```powershell
# Fix all hardcoded URLs
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system
.\fix-all-api-urls.ps1

# Rebuild frontend
cd minetsacco-main
npm run build
```

### 2. Deploy to Server
Run this command on the **server** (10.39.60.15):

```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system
.\deploy-frontend-to-server.ps1
```

This script will:
- Stop IIS
- Clear old files from `C:\inetpub\minetsacco`
- Copy new build from local `dist` folder
- Start IIS
- Verify deployment

### 3. Test from Remote Device
On your device (not the server):

1. **Clear browser cache:**
   - Press `Ctrl+Shift+Del`
   - Select "Cached images and files"
   - Click "Clear data"

2. **Navigate to:** `http://10.39.60.15:8090`

3. **Open DevTools (F12)** and verify in Console:
   - ✅ API calls go to: `http://10.39.60.15:9090/api/*`
   - ❌ NOT `http://localhost:9090/api/*`

4. **Test login** with treasurer credentials

5. **Navigate to Loans page** and verify Edit/Delete buttons work

## Verification Checklist

- [ ] Frontend deployed to IIS (`C:\inetpub\minetsacco`)
- [ ] Backend running on port 9090 with correct database credentials
- [ ] Browser cache cleared on test device
- [ ] Can access from network IP: `http://10.39.60.15:8090`
- [ ] API calls use network IP, not localhost
- [ ] Login successful from remote device
- [ ] Dashboard loads member/loan data
- [ ] All pages accessible (Members, Loans, Savings, Reports, etc.)
- [ ] Treasurer can edit/delete loans

## Backend Database Configuration

The backend `application.properties` was also updated:

**Before:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sacco_db?...
spring.datasource.username=root
spring.datasource.password=0a0b0c0D.
```

**After:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco?...
spring.datasource.username=minetsacco
spring.datasource.password=0a0b0c0D.
```

## Network Configuration Summary

| Component | Port | Access Points |
|-----------|------|---------------|
| Frontend (IIS) | 8090 | `http://localhost:8090`<br>`http://10.39.60.15:8090` |
| Backend (Spring Boot) | 9090 | `http://localhost:9090`<br>`http://10.39.60.15:9090` |
| MySQL Database | 3306 | `localhost:3306` |

## CORS Configuration

Backend allows these origins:
- `http://localhost:8090`
- `http://10.39.60.15:8090`
- `http://10.39.60.*:*` (entire network range)

## Scripts Created

1. **`fix-all-api-urls.ps1`** - Automatically fixes hardcoded API URLs in source files
2. **`deploy-frontend-to-server.ps1`** - Deploys built frontend to IIS
3. **`clear-storage.html`** - Web page to clear browser localStorage (if needed)

## Troubleshooting

### Issue: Still seeing localhost:9090 in browser
**Solution:** 
- Clear browser cache (Ctrl+Shift+Del)
- Use Incognito/Private mode
- Hard refresh (Ctrl+Shift+R)

### Issue: CORS errors persist
**Solution:**
- Verify backend is running with updated CORS config
- Check `CorsConfig.java` allows `10.39.60.*:*`
- Restart backend if needed

### Issue: Database connection errors
**Solution:**
- Verify backend is using `minetsacco` user, not `root`
- Check MySQL user exists: `SELECT user FROM mysql.user WHERE user='minetsacco';`
- Verify backend started with correct credentials

## Build Information

**Last Build:** January 23, 2025
**New JavaScript Hash:** `index-kyVNqnLB.js`
**Previous Hash:** `index-BS7C9sis.js` (contained hardcoded localhost)

## Next Steps

After successful deployment:
1. Test all major features from network access
2. Verify treasurer loan edit/delete functionality
3. Test from multiple devices on the network
4. Document any additional issues

## Support

If issues persist:
1. Check browser DevTools Console for errors
2. Check backend logs for CORS/authentication errors
3. Verify IIS is serving the latest build (check file timestamps)
4. Ensure backend is running on correct port (9090)
