# Authentication Flow Diagram

## NORMAL FLOW (WORKING)

```
┌─────────────────────────────────────────────────────────────────┐
│ USER LOGS IN                                                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Frontend sends: POST /api/auth/login                            │
│ Body: { username: "user", password: "pass" }                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Backend receives login request                                  │
│ Validates credentials                                           │
│ Generates JWT token                                             │
│ Returns: { token: "eyJhbGc..." }                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Frontend stores token in localStorage                           │
│ localStorage.setItem("session", JSON.stringify({                │
│   token: "eyJhbGc...",                                          │
│   user: { username: "user", role: "CUSTOMER_SUPPORT" }         │
│ }))                                                             │
└─────────────────────────────────────────────────────────────────┘
                        