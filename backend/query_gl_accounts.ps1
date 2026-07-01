# Query GL Accounts for INT_LOANS and LOAN_INTEREST
# This script connects to MySQL and runs the critical diagnostic query

$MySQLPath = "C:\xampp\mysql\bin\mysql.exe"  # Adjust if XAMPP is in a different location
$Host = "localhost"
$User = "root"
$Password = ""  # Empty password as per your setup
$Database = "sacco_db"

$Query = "SELECT id, code, calculation_config FROM gl_accounts WHERE code IN ('INT_LOANS', 'LOAN_INTEREST');"

# Run the query
if (Test-Path $MySQLPath) {
    Write-Host "Running query against $Database database..."
    Write-Host "Query: $Query"
    Write-Host "---" -ForegroundColor Green
    
    & $MySQLPath -h $Host -u $User -p"" $Database -e $Query
    
    Write-Host "---" -ForegroundColor Green
    Write-Host "Query completed." -ForegroundColor Green
} else {
    Write-Host "MySQL not found at: $MySQLPath" -ForegroundColor Red
    Write-Host "Please adjust the `$MySQLPath variable in this script to match your XAMPP installation." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Common paths:" -ForegroundColor Yellow
    Write-Host "  C:\xampp\mysql\bin\mysql.exe"
    Write-Host "  C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    Write-Host "  C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe"
}
