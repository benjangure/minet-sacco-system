# M-Pesa Daraja Integration Guide

## Overview
This guide covers implementing real M-Pesa integration using Safaricom's Daraja API for both STK Push (deposits) and B2C (withdrawals).

## Prerequisites

### 1. Safaricom Developer Account
- Go to https://developer.safaricom.co.ke
- Sign up and create an account
- Create an app to get:
  - **Consumer Key**
  - **Consumer Secret**
  - **Business Short Code** (for STK Push)
  - **Lipa Na M-Pesa Online Passkey** (for STK Push)

### 2. M-Pesa Test Credentials
- Test Phone: `254708374149` (or any valid M-Pesa number)
- Test Amount: Any amount (will be reversed after 24 hours)
- Environment: Use sandbox first, then production

### 3. Callback URLs
You need to expose your backend to the internet for Daraja callbacks:
- Use **ngrok** for local testing: `ngrok http 8080`
- Or deploy to a server with public IP

## Implementation Steps

### Step 1: Add Dependencies to pom.xml
```xml
<!-- HTTP Client for API calls -->
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

<!-- JWT for token generation -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### Step 2: Add Configuration Properties
In `application.properties`:
```properties
# M-Pesa Daraja Configuration
mpesa.consumer-key=YOUR_CONSUMER_KEY
mpesa.consumer-secret=YOUR_CONSUMER_SECRET
mpesa.business-short-code=YOUR_SHORT_CODE
mpesa.passkey=YOUR_PASSKEY
mpesa.environment=sandbox
mpesa.callback-url=https://YOUR_DOMAIN/api/mpesa/callback
mpesa.timeout-url=https://YOUR_DOMAIN/api/mpesa/timeout
```

### Step 3: Create M-Pesa Service Classes
- `MpesaAuthService` - Get OAuth tokens
- `MpesaStkPushService` - Initiate STK Push (deposits)
- `MpesaB2CService` - Send B2C payments (withdrawals)
- `MpesaCallbackService` - Handle callbacks

### Step 4: Update Frontend
- Modify `MpesaTransaction.tsx` to handle real API responses
- Show STK prompt for deposits
- Handle callback confirmations

### Step 5: Database Updates
- Add `mpesa_reference` column to transactions table
- Add `mpesa_status` column (INITIATED, COMPLETED, FAILED)
- Add `mpesa_response` column for storing API responses

## API Endpoints Overview

### STK Push (Deposits)
```
POST /api/mpesa/stk-push
Request: { amount, phoneNumber }
Response: { checkoutRequestId, responseCode, message }
```

### B2C Payment (Withdrawals)
```
POST /api/mpesa/b2c
Request: { amount, phoneNumber }
Response: { conversationId, responseCode, message }
```

### Callback Handler
```
POST /api/mpesa/callback
Receives: Transaction confirmation from Daraja
```

## Testing Flow

### Deposit Flow (STK Push)
1. Member enters amount and phone
2. Backend calls STK Push API
3. M-Pesa prompt appears on member's phone
4. Member enters PIN
5. Daraja sends callback to backend
6. Backend credits member's account
7. Frontend shows success

### Withdrawal Flow (B2C)
1. Member enters amount and phone
2. Backend calls B2C API
3. Money sent to member's M-Pesa
4. Daraja sends callback to backend
5. Backend confirms transaction
6. Frontend shows success

## Security Considerations
- Store credentials in environment variables, not in code
- Validate all callbacks using Daraja's signature
- Use HTTPS for all API calls
- Implement rate limiting on callback endpoints
- Log all M-Pesa transactions for audit

## Troubleshooting
- **Invalid credentials**: Check Consumer Key/Secret
- **Callback not received**: Ensure callback URL is publicly accessible
- **STK timeout**: Member didn't enter PIN within 30 seconds
- **Insufficient balance**: Member doesn't have enough M-Pesa balance

## Next Steps
1. Get Daraja credentials from Safaricom
2. Set up ngrok for local testing
3. Implement M-Pesa service classes
4. Update database schema
5. Test with sandbox environment
6. Move to production
