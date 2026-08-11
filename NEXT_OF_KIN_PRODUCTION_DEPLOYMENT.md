# Next of Kin Feature - Production Deployment Guide

## Feature Overview
Multiple Next of Kin with percentage allocation feature allows members to assign beneficiaries with percentage shares (e.g., son 40%, daughter 30%, spouse 30%) that must total 100%.

## Pre-Deployment Checklist

### ✅ Development Complete
- [x] Database migration V1000__Add_multiple_next_of_kin.sql created
- [x] Backend entities, services, controllers implemented
- [x] Frontend NextOfKinManager component created
- [x] Integration with Members.tsx edit dialog
- [x] Validation: percentage must total 100%
- [x] Console errors fixed (notifications, permissions policy, install prompt)
- [x] Data persistence verified (key prop forces refresh)

### ✅ Files Created/Modified

**Backend:**
- `backend/src/main/java/com/minet/sacco/entity/NextOfKin.java`
- `backend/src/main/java/com/minet/sacco/repository/NextOfKinRepository.java`
- `backend/src/main/java/com/minet/sacco/service/NextOfKinService.java`
- `backend/src/main/java/com/minet/sacco/controller/NextOfKinController.java`
- `backend/src/main/java/com/minet/sacco/dto/NextOfKinDTO.java`
- `backend/src/main/resources/db/migration/V1000__Add_multiple_next_of_kin.sql`

**Frontend:**
- `minetsacco-main/src/components/NextOfKinManager.tsx`
- `minetsacco-main/src/pages/Members.tsx` (modified - integrated component)
- `minetsacco-main/src/services/notificationService.ts` (modified - error handling)
- `minetsacco-main/index.html` (modified - permissions policy)
- `minetsacco-main/src/components/InstallPrompt.tsx` (modified - removed spam)

## Database Migration Details

**Table:** `next_of_kin`

```sql
CREATE TABLE next_of_kin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(255),
    id_number VARCHAR(50),
    percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_next_of_kin_member FOREIGN KEY (member_id) 
        REFERENCES members(id) ON DELETE CASCADE
);
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/next-of-kin/member/{memberId}` | Get all next of kin for member |
| POST | `/api/next-of-kin/member/{memberId}/bulk` | Save multiple next of kin |
| POST | `/api/next-of-kin/member/{memberId}` | Create single next of kin |
| PUT | `/api/next-of-kin/{id}` | Update next of kin |
| DELETE | `/api/next-of-kin/{id}` | Delete next of kin |

## Deployment Steps

### Step 1: Build Application
```powershell
# Run the automated deployment script
.\DEPLOY_NEXT_OF_KIN_FEATURE.ps1
```

This script will:
- Build backend JAR with Maven
- Build frontend with npm
- Verify database migration file exists
- Test database connection

### Step 2: Backend Deployment

**Local/Test Server:**
```powershell
# Stop current backend
# (Find process on port 9090)
netstat -ano | findstr :9090
taskkill /PID <PID> /F

# Copy new JAR
Copy-Item backend\target\minet-sacco-backend-*.jar C:\path\to\production\

# Start backend
cd C:\path\to\production\
java -jar minet-sacco-backend-*.jar
```

**Production Server (41.90.64.162):**
```powershell
# Use existing deployment script
.\deploy-backend-to-production.ps1
```

### Step 3: Frontend Deployment

**Local/Test Server:**
```powershell
# Copy built files to web server
Copy-Item -Recurse minetsacco-main\dist\* C:\path\to\webserver\
```

**Production Server:**
```powershell
# Use existing deployment script
.\deploy-frontend-to-server.ps1
```

### Step 4: Verify Migration

Flyway will automatically run V1000 migration on backend startup.

**Check migration status:**
```sql
mysql -u minetsacco -p"0a0b0c0D." minetsacco

-- Verify flyway_schema_history
SELECT * FROM flyway_schema_history WHERE version = '1000';

-- Verify table created
SHOW TABLES LIKE 'next_of_kin';

-- Check table structure
DESCRIBE next_of_kin;
```

**Expected output:**
```
+--------------+--------------+------+-----+-------------------+
| Field        | Type         | Null | Key | Default           |
+--------------+--------------+------+-----+-------------------+
| id           | bigint       | NO   | PRI | NULL              |
| member_id    | bigint       | NO   | MUL | NULL              |
| full_name    | varchar(255) | NO   |     | NULL              |
| relationship | varchar(100) | YES  |     | NULL              |
| phone        | varchar(20)  | YES  |     | NULL              |
| email        | varchar(255) | YES  |     | NULL              |
| id_number    | varchar(50)  | YES  |     | NULL              |
| percentage   | decimal(5,2) | NO   |     | 0.00              |
| is_primary   | tinyint(1)   | YES  |     | 0                 |
| created_at   | timestamp    | YES  |     | CURRENT_TIMESTAMP |
| updated_at   | timestamp    | YES  |     | CURRENT_TIMESTAMP |
+--------------+--------------+------+-----+-------------------+
```

## Post-Deployment Testing

### 1. Access Frontend
```
http://localhost:3000/members (local)
http://41.90.64.162/members (production)
```

### 2. Test Feature Workflow

**A. Add Next of Kin:**
1. Click any member's edit icon
2. Go to "Bank & Next of Kin" tab
3. Click "+ Add Next of Kin"
4. Fill in details (name, relationship, phone, ID, percentage)
5. Verify "Save Next of Kin" button is:
   - **Disabled (gray)** if total ≠ 100%
   - **Enabled (green)** if total = 100%
6. Add more beneficiaries until total = 100%
7. Click "Save Next of Kin"
8. Verify success message

**B. Verify Data Persistence:**
1. Close edit dialog
2. Click same member's edit icon again
3. Go to "Bank & Next of Kin" tab
4. Verify all saved next of kin appear correctly

**C. Test Validation:**
- Try saving with total < 100% (should be disabled)
- Try saving with total > 100% (should show error)
- Verify percentage field only accepts numbers 0-100
- Test removing beneficiaries

**D. Test Edge Cases:**
- Single beneficiary at 100%
- Multiple beneficiaries (e.g., 3 at 33.33%, 33.33%, 33.34%)
- Update existing percentages
- Delete and re-add beneficiaries

### 3. Verify No Console Errors
Open browser DevTools (F12) and check:
- ✅ No notification 400 errors
- ✅ No permissions policy violations
- ✅ No install prompt spam
- ✅ No duplicate useState errors

### 4. Database Verification
```sql
-- Check saved data
SELECT * FROM next_of_kin;

-- Verify percentages total 100% per member
SELECT 
    member_id, 
    SUM(percentage) as total_percentage 
FROM next_of_kin 
GROUP BY member_id;

-- Should return 100.00 for each member
```

## Rollback Plan

If issues occur:

### Backend Rollback:
```powershell
# Stop new backend
# Restore previous JAR
# Start old backend

# If migration ran, rollback:
mysql -u minetsacco -p"0a0b0c0D." minetsacco

-- Drop table
DROP TABLE IF EXISTS next_of_kin;

-- Remove migration record
DELETE FROM flyway_schema_history WHERE version = '1000';
```

### Frontend Rollback:
```powershell
# Restore previous dist folder from backup
Copy-Item -Recurse C:\backup\dist\* C:\webserver\
```

## Known Limitations & User Guide

### For End Users:
1. **Total must equal 100%**: The system requires all beneficiary percentages to add up to exactly 100% before saving
2. **Visual feedback**: 
   - Green checkmark when total = 100%
   - Orange warning when total ≠ 100%
   - Save button disabled until 100%
3. **Decimal precision**: Percentages support up to 2 decimal places (e.g., 33.33%)

### Example Scenarios:
```
Valid:
- Son: 50%, Daughter: 50% = 100% ✓
- Spouse: 40%, Child1: 30%, Child2: 30% = 100% ✓
- Single beneficiary: 100% ✓

Invalid:
- Son: 10% only = 10% ✗ (need 90% more)
- Spouse: 60%, Child: 60% = 120% ✗ (20% too much)
```

## Support & Troubleshooting

### Common Issues:

**1. Button stays grayed out**
- Check total percentage display
- Must equal exactly 100%
- Add more beneficiaries or adjust percentages

**2. Data doesn't appear after save**
- Check browser console for errors
- Verify backend is running on port 9090
- Check database connection

**3. Migration fails**
- Check flyway_schema_history table
- Verify no table name conflicts
- Review backend logs for errors

**4. API 404 errors**
- Verify backend built with new endpoints
- Check backend logs for mapping errors
- Ensure backend restarted after deployment

### Backend Logs:
```powershell
# Check Spring Boot logs for errors
Get-Content backend\logs\spring.log -Tail 50
```

### Database Logs:
```sql
-- Check recent migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
```

## Production Credentials

**Database:**
- Host: localhost
- Port: 3306
- Database: minetsacco
- User: minetsacco
- Password: 0a0b0c0D.

**Production Server:**
- IP: 41.90.64.162
- Backend Port: 9090
- Frontend: Apache/Nginx on port 80/443

## Deployment Checklist

- [ ] Code reviewed and tested locally
- [ ] Backend builds successfully
- [ ] Frontend builds successfully
- [ ] Database migration verified
- [ ] Backup current production (JAR + dist + database)
- [ ] Deploy backend to production
- [ ] Deploy frontend to production
- [ ] Verify migration ran successfully
- [ ] Test feature workflow end-to-end
- [ ] Check browser console for errors
- [ ] Verify data persistence
- [ ] Monitor logs for 10 minutes
- [ ] Notify users of new feature

## User Training Notes

**For Administrators:**
> "When editing member details, you can now add multiple next of kin beneficiaries under the 'Bank & Next of Kin' tab. Each beneficiary can be assigned a percentage share. The total must equal 100% before you can save. The system will show you the current total and disable the save button until you reach exactly 100%."

**Example Instructions:**
```
1. Click Edit on any member
2. Go to "Bank & Next of Kin" tab
3. Click "+ Add Next of Kin"
4. Fill in beneficiary details:
   - Full Name: John Doe
   - Relationship: Son
   - Phone: 0712345678
   - ID Number: 12345678
   - Percentage: 40
5. Add another beneficiary:
   - Full Name: Jane Doe
   - Relationship: Daughter
   - Percentage: 60
6. Verify total shows "100%" with green checkmark
7. Click "Save Next of Kin"
8. Click "Save Changes" to save member
```

---

## Summary

The Next of Kin feature is production-ready with:
- ✅ Full backend implementation (JPA entities, services, REST API)
- ✅ Frontend component with validation
- ✅ Database migration ready
- ✅ Console errors fixed
- ✅ Data persistence verified
- ✅ 100% percentage validation working

**Status: READY FOR PRODUCTION DEPLOYMENT**

Last Updated: 2026-08-07
