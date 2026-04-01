# M-Pesa Daraja Integration - Step by Step Instructions

## STEP 1: Get Daraja Credentials (5 minutes)

### 1.1 Create Safaricom Developer Account
1. Go to https://developer.safaricom.co.ke
2. Click "Sign Up"
3. Fill in your details
4. Verify email
5. Login

### 1.2 Create an App
1. Click "Create App"
2. Fill in app name: "Minet SACCO"
3. Select "Sandbox" environment
4. Click "Create"

### 1.3 Copy Your Credentials
You'll see a page with:
- **Consumer Key** - Copy this
- **Consumer Secret** - Copy this
- **Business Short Code** - Should be 174379 for sandbox
- **Lipa Na M-Pesa Online Passkey** - Copy this

Save these somewhere safe!

## STEP 2: Set Up ngrok (5 minutes)

### 2.1 Download ngrok
1. Go to https://ngrok.com/download
2. Download for your OS (Windows/Mac/Linux)
3. Extract the file

### 2.2 Run ngrok
```bash
# Navigate to ngrok folder
cd path/to/ngrok

# Run ngrok
./ngrok http 8080

# On Windows:
ngrok.exe http 8080
```

### 2.3 Copy Your Public URL
You'll see output like:
```
Forwarding                    https://abc123.ngrok.io -> http://localhost:8080
```

Copy the HTTPS URL: `https://abc123.ngrok.io`

**Keep ngrok running!** You need it for callbacks.

## STEP 3: Configure Backend (5 minutes)

### 3.1 Create Environment File
In the `backend` folder, create a file named `.env`:

```
MPESA_CONSUMER_KEY=your_consumer_key_here
MPESA_CONSUMER_SECRET=your_consumer_secret_here
MPESA_BUSINESS_SHORT_CODE=174379
MPESA_PASSKEY=bfb279f9aa9bdbcf158e97dd1a503b6e78c6a6781e97a45ffe23bae5d31c2157
MPESA_ENVIRONMENT=sandbox
MPESA_CALLBACK_URL=https://abc123.ngrok.io/api/mpesa/callback/stk
MPESA_TIMEOUT_URL=https://abc123.ngrok.io/api/mpesa/callback/timeout
```

Replace:
- `your_consumer_key_here` with your Consumer Key
- `your_consumer_secret_here` with your Consumer Secret
- `abc123.ngrok.io` with your ngrok URL

### 3.2 Verify pom.xml Has Dependencies
Check that `backend/pom.xml` has these dependencies (should already be there):
```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

## STEP 4: Start Backend (3 minutes)

### 4.1 Open Terminal
Navigate to the `backend` folder:
```bash
cd backend
```

### 4.2 Run Maven
```bash
mvn spring-boot:run
```

Wait for it to start. You should see:
```
Started Application in X seconds
```

## STEP 5: Start Frontend (2 minutes)

### 5.1 Open New Terminal
Navigate to the `minetsacco-main` folder:
```bash
cd minetsacco-main
```

### 5.2 Run Frontend
```bash
npm run dev
```

You should see:
```
VITE v... ready in ... ms

➜  Local:   http://localhost:5173/
```

## STEP 6: Test Deposit (5 minutes)

### 6.1 Login as Member
1. Open http://localhost:5173/member
2. Enter:
   - Phone/Employee ID: `254708374149`
   - National ID: `12345678`
3. Click "Login"

### 6.2 Go to Dashboard
You should see the Member Dashboard with account balances.

### 6.3 Click "Deposit via M-Pesa"
1. Click the "Deposit via M-Pesa" card
2. A dialog will open

### 6.4 Enter Deposit Details
1. Amount: `100`
2. Phone: `254708374149`
3. Click "Continue"

### 6.5 Confirm
1. Review the details
2. Click "Confirm"

### 6.6 Check Your Phone
1. You should see an M-Pesa prompt on your phone
2. Enter PIN: `1234`
3. Transaction completes

### 6.7 Verify Success
1. You should see "Deposit Request Submitted" message
2. A reference number will be shown
3. Your savings balance should increase by 100 KES

## STEP 7: Test Withdrawal (5 minutes)

### 7.1 Go Back to Dashboard
Click "Done" to close the dialog.

### 7.2 Click "Withdraw via M-Pesa"
1. Click the "Withdraw via M-Pesa" card
2. A dialog will open

### 7.3 Enter Withdrawal Details
1. Amount: `50`
2. Phone: `254708374149`
3. Click "Continue"

### 7.4 Confirm
1. Review the details
2. Click "Confirm"

### 7.5 Verify Success
1. You should see "Withdrawal Request Submitted" message
2. Your savings balance should decrease by 50 KES immediately
3. Money will be sent to your M-Pesa

## STEP 8: Test Admin Dashboard (5 minutes)

### 8.1 Login as Admin
1. Go to http://localhost:5173/login
2. Enter admin credentials
3. Click "Login"

### 8.2 Go to M-Pesa Transactions
1. Click "Administration" in sidebar
2. Click "M-Pesa Transactions"

### 8.3 View Pending Transactions
You should see a list of pending M-Pesa transactions.

### 8.4 Complete a Deposit
1. Find a pending deposit
2. Click "Complete"
3. The member's account should be credited
4. Transaction should disappear from list

### 8.5 Complete a Withdrawal
1. Find a pending withdrawal
2. Click "Complete"
3. Transaction should disappear from list

### 8.6 Fail a Transaction
1. Find a pending transaction
2. Click "Fail"
3. Enter reason: "Test failure"
4. Click "Confirm Failure"
5. For withdrawals: Balance should be restored
6. Transaction should disappear from list

## STEP 9: Check Database (Optional)

### 9.1 View Transactions
```sql
SELECT * FROM transactions 
WHERE description LIKE '%M-Pesa%' 
ORDER BY transaction_date DESC;
```

### 9.2 View Account Balances
```sql
SELECT m.first_name, m.last_name, a.account_type, a.balance
FROM members m
JOIN accounts a ON m.id = a.member_id
ORDER BY m.id;
```

## STEP 10: Troubleshooting

### Issue: M-Pesa Prompt Not Appearing
**Solution:**
1. Check ngrok is running
2. Verify phone number is valid
3. Check credentials in .env file
4. Review backend logs for errors

### Issue: Callback Not Received
**Solution:**
1. Verify ngrok URL in MPESA_CALLBACK_URL
2. Check ngrok is still running
3. Look at backend logs
4. Ensure firewall allows incoming requests

### Issue: "Invalid credentials" Error
**Solution:**
1. Double-check Consumer Key and Secret
2. Verify they're from the correct app
3. Check .env file for typos
4. Restart backend after changing .env

### Issue: Transaction Not Updating
**Solution:**
1. Check backend logs for errors
2. Verify callback endpoint is being called
3. Check database for transaction record
4. Review JSON parsing in callback handler

## STEP 11: Going to Production

### 11.1 Get Production Credentials
1. Go to Daraja dashboard
2. Switch to "Production" environment
3. Create new app
4. Copy production credentials

### 11.2 Update Configuration
1. Change MPESA_ENVIRONMENT to `production`
2. Update MPESA_CONSUMER_KEY and MPESA_CONSUMER_SECRET
3. Update callback URLs to your production domain
4. Update MPESA_BUSINESS_SHORT_CODE if different

### 11.3 Deploy Backend
1. Build: `mvn clean package`
2. Deploy to production server
3. Set environment variables on server
4. Start application

### 11.4 Deploy Frontend
1. Build: `npm run build`
2. Deploy to production server
3. Update API_BASE_URL to production backend

### 11.5 Test Production
1. Test with small amounts first
2. Monitor logs for errors
3. Verify callbacks are received
4. Test with real M-Pesa transactions

## Quick Reference

### Test Credentials
- Phone: `254708374149`
- PIN: `1234` (when prompted)
- Amount: Any amount (will be reversed after 24h)

### API Endpoints
- Deposit: `POST /api/mpesa/deposit/initiate`
- Withdraw: `POST /api/mpesa/withdraw/initiate`
- Callback: `POST /api/mpesa/callback/stk`
- Admin: `GET /api/admin/mpesa/pending`

### Files to Check
- Backend config: `backend/src/main/resources/application.properties`
- Frontend component: `minetsacco-main/src/components/MpesaTransaction.tsx`
- Admin page: `minetsacco-main/src/pages/MpesaTransactionManagement.tsx`

### Useful Commands
```bash
# Start backend
cd backend && mvn spring-boot:run

# Start frontend
cd minetsacco-main && npm run dev

# Start ngrok
ngrok http 8080

# View backend logs
tail -f logs/application.log

# Check database
mysql -u root sacco_db
```

## Support

If you get stuck:
1. Check the logs (backend and browser console)
2. Review DARAJA_TESTING_GUIDE.md for detailed scenarios
3. Check Daraja docs: https://developer.safaricom.co.ke/docs
4. Verify ngrok is running and URL is correct
5. Ensure credentials are correct in .env file

## Success Checklist

- [ ] Daraja credentials obtained
- [ ] ngrok running and URL copied
- [ ] .env file created with credentials
- [ ] Backend started successfully
- [ ] Frontend started successfully
- [ ] Member login works
- [ ] Deposit test successful
- [ ] Withdrawal test successful
- [ ] Admin dashboard accessible
- [ ] Admin can complete transactions
- [ ] Database shows transactions
- [ ] Ready for production!

You're all set! Start with STEP 1 and follow through. Good luck! 🚀
