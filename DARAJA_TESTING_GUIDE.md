# M-Pesa Daraja Integration - Testing Guide

## Phase 1: Setup & Configuration

### Step 1: Get Daraja Credentials
1. Go to https://developer.safaricom.co.ke
2. Sign up or login
3. Create a new app
4. Copy these credentials:
   - **Consumer Key**
   - **Consumer Secret**
   - **Business Short Code** (e.g., 174379 for sandbox)
   - **Lipa Na M-Pesa Online Passkey** (provided by Safaricom)

### Step 2: Set Up ngrok for Local Testing
```bash
# Download ngrok from https://ngrok.com/download
# Extract and run:
ngrok http 8080

# You'll get a URL like: https://abc123.ngrok.io
# This is your public URL for callbacks
```

### Step 3: Configure Environment Variables
Create a `.env` file in the backend root:
```
MPESA_CONSUMER_KEY=your_consumer_key
MPESA_CONSUMER_SECRET=your_consumer_secret
MPESA_BUSINESS_SHORT_CODE=174379
MPESA_PASSKEY=bfb279f9aa9bdbcf158e97dd1a503b6e78c6a6781e97a45ffe23bae5d31c2157
MPESA_ENVIRONMENT=sandbox
MPESA_CALLBACK_URL=https://abc123.ngrok.io/api/mpesa/callback/stk
MPESA_TIMEOUT_URL=https://abc123.ngrok.io/api/mpesa/callback/timeout
```

### Step 4: Update application.properties
```properties
mpesa.consumer-key=${MPESA_CONSUMER_KEY}
mpesa.consumer-secret=${MPESA_CONSUMER_SECRET}
mpesa.business-short-code=${MPESA_BUSINESS_SHORT_CODE}
mpesa.passkey=${MPESA_PASSKEY}
mpesa.environment=${MPESA_ENVIRONMENT}
mpesa.callback-url=${MPESA_CALLBACK_URL}
mpesa.timeout-url=${MPESA_TIMEOUT_URL}
```

### Step 5: Add Dependencies to pom.xml
```xml
<!-- HTTP Client -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>

<!-- JSON Processing -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

## Phase 2: Testing STK Push (Deposits)

### Test Scenario 1: Successful Deposit
1. Start backend: `mvn spring-boot:run`
2. Start frontend: `npm run dev`
3. Login as member
4. Go to Member Dashboard
5. Click "Deposit via M-Pesa"
6. Enter:
   - Amount: 100
   - Phone: 254708374149 (test number)
7. Click "Continue" → "Confirm"
8. **Expected**: M-Pesa prompt appears on test phone
9. Enter M-Pesa PIN
10. **Expected**: 
    - Callback received by backend
    - Member's savings account credited with 100 KES
    - Success message shown
    - Transaction appears in dashboard

### Test Scenario 2: Deposit with Invalid Amount
1. Click "Deposit via M-Pesa"
2. Enter:
   - Amount: -50 or 0
   - Phone: 254708374149
3. Click "Continue"
4. **Expected**: Error message "Amount must be greater than zero"

### Test Scenario 3: Deposit with Invalid Phone
1. Click "Deposit via M-Pesa"
2. Enter:
   - Amount: 100
   - Phone: invalid_number
3. Click "Continue" → "Confirm"
4. **Expected**: Error from Daraja API

### Test Scenario 4: STK Timeout
1. Click "Deposit via M-Pesa"
2. Enter amount and phone
3. Click "Confirm"
4. **Do NOT enter PIN** - wait 30 seconds
5. **Expected**: Timeout callback received, transaction marked as FAILED

## Phase 3: Testing B2C (Withdrawals)

### Test Scenario 1: Successful Withdrawal
1. Ensure member has savings balance (e.g., 500 KES)
2. Click "Withdraw via M-Pesa"
3. Enter:
   - Amount: 100
   - Phone: 254708374149
4. Click "Continue" → "Confirm"
5. **Expected**:
   - Member's savings account debited immediately (500 → 400)
   - B2C API called
   - Callback received
   - Money sent to test phone
   - Success message shown

### Test Scenario 2: Withdrawal with Insufficient Balance
1. Member has 50 KES in savings
2. Click "Withdraw via M-Pesa"
3. Enter:
   - Amount: 100
   - Phone: 254708374149
4. Click "Continue"
5. **Expected**: Error "Insufficient balance. Available: KES 50"

### Test Scenario 3: Withdrawal Failure
1. Click "Withdraw via M-Pesa"
2. Enter:
   - Amount: 100
   - Phone: invalid_number
3. Click "Confirm"
4. **Expected**:
   - B2C call fails
   - Member's balance restored (refunded)
   - Error message shown

### Test Scenario 4: B2C Timeout
1. Click "Withdraw via M-Pesa"
2. Enter amount and phone
3. Click "Confirm"
4. **Expected**: If timeout occurs:
   - Timeout callback received
   - Member's balance restored
   - Transaction marked as TIMEOUT

## Phase 4: Admin Dashboard Testing

### Test Scenario 1: View Pending Transactions
1. Login as admin/treasurer
2. Go to Admin → M-Pesa Transactions
3. **Expected**: List of all pending M-Pesa transactions

### Test Scenario 2: Complete Deposit
1. Find pending deposit in list
2. Click "Complete"
3. **Expected**:
   - Member's account credited
   - Transaction status changed to COMPLETED
   - Removed from pending list

### Test Scenario 3: Complete Withdrawal
1. Find pending withdrawal in list
2. Click "Complete"
3. **Expected**:
   - Transaction status changed to COMPLETED
   - Removed from pending list

### Test Scenario 4: Mark Transaction as Failed
1. Find pending transaction
2. Click "Fail"
3. Enter reason: "Invalid phone number"
4. Click "Confirm Failure"
5. **Expected**:
   - For deposits: No refund (money wasn't credited)
   - For withdrawals: Balance restored to member
   - Transaction marked as FAILED
   - Removed from pending list

## Phase 5: Database Verification

### Check Transaction Records
```sql
-- View all M-Pesa transactions
SELECT * FROM transactions 
WHERE description LIKE '%M-Pesa%' 
ORDER BY transaction_date DESC;

-- View pending transactions
SELECT * FROM transactions 
WHERE description LIKE '%PENDING%' 
ORDER BY transaction_date DESC;

-- View completed transactions
SELECT * FROM transactions 
WHERE description LIKE '%COMPLETED%' 
ORDER BY transaction_date DESC;

-- Check member account balances
SELECT m.id, m.first_name, m.last_name, a.account_type, a.balance
FROM members m
JOIN accounts a ON m.id = a.member_id
ORDER BY m.id;
```

## Phase 6: Callback Testing

### Manual Callback Test (using Postman)
1. Open Postman
2. Create POST request to: `http://localhost:8080/api/mpesa/callback/stk`
3. Set header: `Content-Type: application/json`
4. Body (STK Push callback):
```json
{
  "Body": {
    "stkCallback": {
      "MerchantRequestID": "test-123",
      "CheckoutRequestID": "ws_CO_123456789",
      "ResultCode": 0,
      "ResultDesc": "The service request has been processed successfully.",
      "CallbackMetadata": {
        "Item": [
          {
            "Name": "Amount",
            "Value": 100
          },
          {
            "Name": "MpesaReceiptNumber",
            "Value": "LHG31H500G2"
          },
          {
            "Name": "TransactionDate",
            "Value": 20240101120000
          },
          {
            "Name": "PhoneNumber",
            "Value": 254708374149
          }
        ]
      }
    }
  }
}
```

5. Send request
6. **Expected**: 
   - Response: `{"ResultCode": 0}`
   - Transaction updated in database
   - Account credited

## Phase 7: Production Deployment

### Before Going Live:
1. ✅ Test all scenarios in sandbox
2. ✅ Get production credentials from Safaricom
3. ✅ Update environment variables to production
4. ✅ Deploy to production server
5. ✅ Update callback URLs to production domain
6. ✅ Test with real M-Pesa transactions (small amounts)
7. ✅ Monitor logs for errors
8. ✅ Set up alerts for failed transactions

### Production Configuration:
```properties
mpesa.environment=production
mpesa.callback-url=https://yourdomain.com/api/mpesa/callback/stk
mpesa.timeout-url=https://yourdomain.com/api/mpesa/callback/timeout
```

## Troubleshooting

### Issue: "Invalid credentials"
- Check Consumer Key and Secret
- Verify they're from the correct app
- Ensure they're not expired

### Issue: "Callback not received"
- Verify ngrok is running
- Check callback URL in configuration
- Ensure firewall allows incoming requests
- Check backend logs for errors

### Issue: "STK prompt not appearing"
- Verify phone number is valid M-Pesa account
- Check amount is valid
- Ensure Business Short Code is correct
- Try with different phone number

### Issue: "B2C payment failed"
- Check if phone number has M-Pesa account
- Verify amount is within limits
- Check if account has sufficient balance
- Ensure phone number format is correct (254...)

### Issue: "Transaction not updating"
- Check if callback endpoint is being called
- Verify transaction ID matching in callback
- Check database for transaction record
- Review backend logs for errors

## Monitoring & Logging

### Enable Debug Logging
Add to application.properties:
```properties
logging.level.com.minet.sacco.service=DEBUG
logging.level.com.minet.sacco.controller=DEBUG
```

### Monitor Callbacks
Check backend logs:
```bash
tail -f logs/application.log | grep "M-Pesa\|callback\|STK\|B2C"
```

### Database Monitoring
```sql
-- Monitor transaction status
SELECT 
  DATE(transaction_date) as date,
  transaction_type,
  COUNT(*) as count,
  SUM(amount) as total,
  COUNT(CASE WHEN description LIKE '%COMPLETED%' THEN 1 END) as completed,
  COUNT(CASE WHEN description LIKE '%PENDING%' THEN 1 END) as pending,
  COUNT(CASE WHEN description LIKE '%FAILED%' THEN 1 END) as failed
FROM transactions
WHERE description LIKE '%M-Pesa%'
GROUP BY DATE(transaction_date), transaction_type;
```

## Support & Resources

- Daraja API Docs: https://developer.safaricom.co.ke/docs
- M-Pesa Integration Guide: https://developer.safaricom.co.ke/docs?shell#introduction
- Test Credentials: https://developer.safaricom.co.ke/test-credentials
- Sandbox Environment: https://sandbox.safaricom.co.ke
