# Start the Minet SACCO backend with HR sign-in enabled.
# Run in PowerShell on the server:   powershell -ExecutionPolicy Bypass -File .\start-backend.ps1
# Edit the values below. Do NOT commit real passwords to git.

$Jar = "C:\minetsacco-deploy\backend\target\minet-sacco-0.0.1-SNAPSHOT.jar"

# ---- HR sign-in ----
$env:HR_API_URL                = "https://dmvic.minet.co.ke/api/login.php"
$env:ALLOW_LOCAL_AUTH_FALLBACK = "true"    # set to "false" once HR sign-in is confirmed working
$env:HR_API_DEBUG              = "true"    # TEMPORARY - shows HR's reply in failed-login messages; set "false" afterwards

# ---- Optional ----
# $env:SMTP_HOST = "smtp.office365.com"
# $env:SMTP_PORT = "587"
# $env:SMTP_USER = "no_reply@minet.co.ke"
# $env:SMTP_PASS = "<mailbox app password>"

# Stop the running backend (only java processes on this server)
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 3

# Start it (runs in this window; use Ctrl+C to stop). Logs go to the file configured in application-prod.properties.
java -jar $Jar --spring.profiles.active=prod
