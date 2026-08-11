# Quick Diagnostic Steps

Your dashboard is still slow. Let's find out why:

## Step 1: Check Database Indexes (MOST IMPORTANT)

1. Open **MySQL Workbench**
2. Run the script: `backend/CHECK_INDEXES.sql`
3. **Expected result**: You should see 40-50 indexes listed
4. **If you see 0-5 indexes**: The indexes weren't created - run `APPLY_INDEXES_SIMPLE.sql` again

## Step 2: Check Which API Calls Are Slow

1. Open the dashboard that's loading slowly
2. Press **F12** (open DevTools)
3. Click **Network** tab
4. Refresh the page
5. Look for API calls (starting with `/api/`)
6. **Which ones are taking 5+ seconds?** (they'll be red or orange)
7. **Common slow endpoints**:
   - `/api/members` - Should take <2s with indexes
   - `/api/loans` - Should take <3s with indexes
   - `/api/accounts` - Should take <1s with indexes
   - `/api/loans/{id}/repayments` - If many calls, could be N+1 problem

## Step 3: Check Backend Query Logs

Look at your backend console output. You should see SQL queries like:
```
Hibernate: SELECT ... FROM loans WHERE member_id = ?
Hibernate: SELECT ... FROM transactions WHERE account_id = ?
```

**Look for**:
- Queries taking > 1 second (these will be logged)
- Many similar queries running repeatedly (N+1 problem)

## Step 4: Rebuild Frontend (If skeleton loaders not showing)

If you still see white screens instead of skeleton loaders:

```powershell
cd minetsacco-main
npm run build
# Or if running dev server:
npm run dev
```

## Common Issues & Fixes

### Issue 1: Indexes Not Created
**Symptom**: CHECK_INDEXES.sql shows 0-10 indexes
**Fix**: Run `APPLY_INDEXES_SIMPLE.sql` again in MySQL Workbench

### Issue 2: Too Much Data
**Symptom**: `/api/loans` or `/api/members` taking 10+ seconds
**Fix**: Add `?paginated=true&page=0&size=50` to API URL

### Issue 3: N+1 Query Problem
**Symptom**: Backend logs show 100+ similar queries
**Fix**: Already fixed in code, but may need more optimization

### Issue 4: Database Connection Slow
**Symptom**: All queries slow, even simple ones
**Fix**: Check MySQL is running locally (not remote)

## What to Share

After running diagnostics, share:
1. **How many indexes** does CHECK_INDEXES.sql show?
2. **Which API endpoint** is slowest (from Network tab)?
3. **How long** does it take to load?
4. **Any errors** in browser console (F12 → Console tab)?

This will help identify the exact bottleneck!
