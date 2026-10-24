@echo off
setlocal
cd /d "%~dp0.."

echo ============================================
echo  LDAP User Management - Stop
echo ============================================

REM 1. Stop the app process
echo.
echo [1/2] Stopping app process...
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*spring-boot-user-management-ldap*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Host ('Stopped PID ' + $_.ProcessId) }"

REM 2. Stop Docker containers (LDAP data stays in data\slapd\)
echo.
echo [2/2] Stopping Docker containers...
docker compose down

echo.
echo Stopped. LDAP data is kept in data\slapd\. Run scripts\start.bat to bring it back.
pause
