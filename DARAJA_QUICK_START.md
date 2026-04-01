# M-Pesa Daraja - Quick Start (5 Minutes)

## 1. Get Credentials (2 min)
- Go to https://developer.safaricom.co.ke
- Sign up → Create App
- Copy: Consumer Key, Consumer Secret, Business Short Code, Passkey

## 2. Set Up ngrok (1 min)
```bash
# Download from https://ngrok.com/download
ngrok http 8080
# Copy the HTTPS URL (e.g., https://abc123.ngrok.io)
```

## 3. Configure Backend (1 min)
Create `backend/.env`:
```
MPESA_CONSUMER_KEY=your_key
MPESA_CONSUMER_SECRET=your_secret
MPESA_BUSINESS_SHORT_CODE=174379
MPESA_PASSKEY=bfb279f9aa9bdbcf158e97dd1a503b6e78c6a6781e97a45ffe23bae5d31c2157
MPESA_ENVIRONMENT=sandbox
MPESA_CALLBACK_URL=https://abc123.ngrok.io/api/mpesa/callback/stk
MPESA_TIMEOUT_URL=https://abc123.ngrok.io/api/mpesa/callback/timeout
```

## 4. Add Dependencies (1 min)
In `backend/pom.xml`, add:
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

## 5. Start & Test (1 min)
```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend (new terminal)
cd minetsacco-main
npm run dev

# Test
1. Login as member
2. Go to Member Dashboard
3. Click "Deposit via M-Pesa"
4. Enter: Amount=100, Phone=254708374149
5. Click Confirm
6. Check phone for M-Pesa prompt
```

## What Happens Next?

### Deposit Flow:
1. ✅ Member enters amount & phone
2. ✅ Backend calls Daraja STK Push API
3. ✅ M-Pesa prompt appears on phone
4. ✅ Member enters PIN
5. ✅ Daraja sends callback to backend
6. ✅ Backend credits member's account
7. ✅ Frontend shows success

### Withdrawal Flow:
1. ✅ Member enters amount & phone
2. ✅ Backend deducts from savings immediately
3. ✅ Backend calls Daraja B2C API
4. ✅ Money sent to member's M-Pesa
5. ✅ Daraja sends callback
6. ✅ Frontend shows success

## Files Created

### Backend Services:
- `MpesaConfig.java` - Configuration
- `MpesaAuthService.java` - OAuth tokens
- `MpesaStkPushService.java` - Deposits
- `MpesaB2CService.java` - Withdrawals
- `MpesaCallbackService.java` - Callback handling
- `MpesaDarajaController.java` - API endpoints

### Frontend:
- `MpesaTransaction.tsx` - Updated with real API calls

### Admin:
- `MpesaAdminController.java` - Admin endpoints
- `MpesaTransactionManagement.tsx` - Admin dashboard

## API Endpoints

### Member Endpoints:
```
POST /api/mpesa/deposit/initiate
  { amount, phoneNumber }
  → { success, checkoutRequestId }

POST /api/mpesa/withdraw/initiate
  { amount, phoneNumber }
  → { success, conversationId }
```

### Callback Endpoints:
```
POST /api/mpesa/callback/stk
POST /api/mpesa/callback/b2c
POST /api/mpesa/callback/timeout
```

### Admin Endpoints:
```
GET /api/admin/mpesa/pending
POST /api/admin/mpesa/deposit/{id}/complete
POST /api/admin/mpesa/withdraw/{id}/complete
POST /api/admin/mpesa/{id}/fail
```

## Test Credentials (Sandbox)
- Phone: 254708374149
- Amount: Any (will be reversed after 24h)
- PIN: 1234 (when prompted)

## Next Steps
1. ✅ Get real Daraja credentials
2. ✅ Set up ngrok
3. ✅ Configure environment variables
4. ✅ Add dependencies to pom.xml
5. ✅ Start backend & frontend
6. ✅ Test deposit flow
7. ✅ Test withdrawal flow
8. ✅ Test admin dashboard
9. ✅ Deploy to production

## Troubleshooting

**M-Pesa prompt not appearing?**
- Check phone number is valid
- Verify credentials are correct
- Check ngrok is running

**Callback not received?**
- Verify callback URL in config
- Check ngrok is running
- Check backend logs

**Transaction not updating?**
- Check database for transaction record
- Review backend logs
- Verify callback endpoint is being called

## Support
- Daraja Docs: https://developer.safaricom.co.ke/docs
- Test Credentials: https://developer.safaricom.co.ke/test-credentials
