@echo off
echo ========================================
echo   Import Database Dump to Development
echo ========================================
echo.

echo Step 1: Creating tminet database and user...
echo Please enter MySQL root password when prompted...
mysql -u root -p < setup-dev-db.sql

if errorlevel 1 (
    echo.
    echo Failed to create database!
    pause
    exit /b 1
)

echo.
echo Step 2: Importing dump files...
echo.

set DUMP_PATH=C:\Users\Lenovo\Desktop\Dump20260727

for %%f in ("%DUMP_PATH%\*.sql") do (
    echo Importing %%~nxf...
    mysql -u tminet -p0a0b0c0D. tminet < "%%f"
)

echo.
echo ========================================
echo   Import Complete!
echo ========================================
echo.
echo Development database ready:
echo   Database: tminet
echo   User: tminet
echo   Password: 0a0b0c0D.
echo.
echo Next step: Start the backend
echo   cd backend
echo   mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
echo.
pause
