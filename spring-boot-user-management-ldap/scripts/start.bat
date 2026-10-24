@echo off
setlocal
cd /d "%~dp0.."

REM -------------------------------------------------------
REM  LDAP User Management - one-click start
REM -------------------------------------------------------

echo ============================================
echo  LDAP User Management - Start
echo ============================================

REM 1. Start OpenLDAP + phpLDAPadmin + seed import
echo.
echo [1/4] Starting Docker containers...
docker compose up -d
if errorlevel 1 (
    echo [ERROR] docker compose failed. Is Docker Desktop running?
    pause
    exit /b 1
)

REM 2. Wait until OpenLDAP is healthy
echo.
echo [2/4] Waiting for OpenLDAP...
:wait_ldap
for /f "tokens=*" %%i in ('docker inspect --format "{{.State.Health.Status}}" user-management-openldap 2^>nul') do set LDAP_HEALTH=%%i
if /i not "%LDAP_HEALTH%"=="healthy" (
    %SystemRoot%\System32\timeout.exe /t 3 /nobreak >nul
    goto wait_ldap
)
echo        OpenLDAP is ready.

REM 3. Stop the old app process (match by command line, do not kill other java apps)
echo.
echo [3/4] Stopping old app process...
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*spring-boot-user-management-ldap*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }" 2>nul

REM 4. Check jar exists
if not exist "target\spring-boot-user-management-ldap-0.0.1-SNAPSHOT.jar" (
    echo [ERROR] jar not found. Run first: mvn package -DskipTests
    pause
    exit /b 1
)

REM 5. Start app in background, logs go to target\app.log
echo.
echo [4/4] Starting application...
set "JAVA_CMD=%LOCALAPPDATA%\Programs\IntelliJ IDEA\jbr\bin\java.exe"
where java >nul 2>&1
if not errorlevel 1 set "JAVA_CMD=java"
if /i not "%JAVA_CMD%"=="java" (
    if not exist "%JAVA_CMD%" (
        echo [ERROR] java not found in PATH, and IntelliJ JBR path is missing.
        pause
        exit /b 1
    )
)
powershell -NoProfile -Command "Start-Process -FilePath '%JAVA_CMD%' -ArgumentList '-jar','target\spring-boot-user-management-ldap-0.0.1-SNAPSHOT.jar' -WorkingDirectory '%CD%' -RedirectStandardOutput '%CD%\target\app.log' -RedirectStandardError '%CD%\target\app.err.log' -WindowStyle Hidden"

REM 6. Wait until the app answers
echo        Waiting for the app to come up...
:wait_app
curl -s -o nul http://localhost:1064/login.html 2>nul
if errorlevel 1 (
    %SystemRoot%\System32\timeout.exe /t 2 /nobreak >nul
    goto wait_app
)

echo.
echo ============================================
echo  Started!
echo    Login page : http://localhost:1064/login.html
echo    Dashboard  : http://localhost:1064/
echo    Swagger UI : http://localhost:1064/api/swagger-ui.html
echo    Account    : admin / admin123
echo    Logs       : target\app.log
echo ============================================
pause
