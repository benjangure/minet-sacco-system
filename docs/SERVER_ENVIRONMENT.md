# Backend environment variables (set on the server, never commit values)

| Variable | Purpose |
|---|---|
| `SMTP_HOST` | SMTP server (default `smtp.office365.com`) |
| `SMTP_PORT` | SMTP port (default `587`, STARTTLS) |
| `SMTP_USER` | Mailbox used to send (e.g. `no_reply@minet.co.ke`) |
| `SMTP_PASS` | Mailbox password / app password |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | 32+ random characters |
| `JASYPT_PASSWORD` | Encryption password |
| `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_PASSKEY` | M-Pesa Daraja |
| `APP_FRONTEND_URL` | Public site URL used in emails (default `https://sacco.minet.co.ke`) |

Example (Windows service / PowerShell, machine scope):

```powershell
[Environment]::SetEnvironmentVariable("SMTP_HOST","smtp.office365.com","Machine")
[Environment]::SetEnvironmentVariable("SMTP_PORT","587","Machine")
[Environment]::SetEnvironmentVariable("SMTP_USER","no_reply@minet.co.ke","Machine")
[Environment]::SetEnvironmentVariable("SMTP_PASS","<mailbox app password>","Machine")
```

Restart the backend service afterwards, then check `GET /api/email/health` (Admin token required).
Office 365 note: SMTP AUTH must be enabled for the mailbox, and accounts with MFA need an *app password*.

## HR sign-in

| Variable | Purpose |
|---|---|
| `HR_API_URL` | HR login endpoint, e.g. `https://dmvic.minet.co.ke/api/login.php` (empty = local passwords only) |
| `ALLOW_LOCAL_AUTH_FALLBACK` | `false` = members authenticate only via HR (staff accounts unaffected) |
| `HR_API_USERNAME_PARAM` / `HR_API_PASSWORD_PARAM` | Query parameter names if not `username` / `password` |
| `HR_API_TIMEOUT_SECONDS` | Timeout (default 10) |
