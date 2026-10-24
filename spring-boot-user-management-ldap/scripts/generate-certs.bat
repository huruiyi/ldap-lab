@echo off
setlocal EnableExtensions

REM =============================================================================
REM generate-certs.bat - Regenerate the three TLS cert files for the OpenLDAP
REM container on Windows.
REM
REM Background:
REM   data\certificates\server.key, server.crt and dhparam.pem are auto-created
REM   by the osixia/openldap container on first boot (internally via cfssl).
REM   The folder is gitignored. This script recreates equivalent files with
REM   openssl.exe. Normally you do NOT need it - deleting data\ and restarting
REM   the container rebuilds everything. Use it only to regenerate manually.
REM
REM Usage (double-click or run in cmd):
REM   generate-certs.bat
REM
REM Output (written to data\certificates\):
REM   server.key                  server private key (EC P-384, same as container)
REM   server.crt                  server cert (CN=openldap, SAN=DNS:openldap,
REM                               signed by local CA, valid 365 days)
REM   dhparam.pem                 DH parameters (2048 bit)
REM   springframework.org.ca.crt  CA cert (copy; on Linux this is a symlink,
REM                               which does not work in a Windows mount)
REM =============================================================================

set "SCRIPT_DIR=%~dp0"
set "CERT_DIR=%SCRIPT_DIR%..\data\certificates"
set "CA_DIR=%CERT_DIR%\.ca"
set "DAYS=365"

REM --- openssl.exe is expected on PATH (OpenSSL-WinUniversal bin\x64 added to user PATH) ---
set "OPENSSL=openssl.exe"
where openssl.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] openssl.exe not found in PATH. Install OpenSSL or fix PATH first.
    exit /b 1
)
echo Using: %OPENSSL%

if not exist "%CERT_DIR%" mkdir "%CERT_DIR%"
if not exist "%CA_DIR%" mkdir "%CA_DIR%"

echo ==^> 1/4 Generate CA key and CA cert (default-ca)
"%OPENSSL%" ecparam -name secp384r1 -genkey -noout -out "%CA_DIR%\default-ca-key.pem"
if errorlevel 1 goto :fail
"%OPENSSL%" req -x509 -new -key "%CA_DIR%\default-ca-key.pem" -subj "/C=US/ST=New Mexico/L=Albuquerque/O=A1A Car Wash/OU=Information Technology Dep./CN=docker-light-baseimage" -days 3650 -out "%CA_DIR%\default-ca.pem"
if errorlevel 1 goto :fail

echo ==^> 2/4 Generate server private key server.key (EC P-384)
"%OPENSSL%" ecparam -name secp384r1 -genkey -noout -out "%CERT_DIR%\server.key"
if errorlevel 1 goto :fail

echo ==^> 3/4 Sign server cert server.crt with CA (CN=openldap, SAN=DNS:openldap)
"%OPENSSL%" req -new -key "%CERT_DIR%\server.key" -subj "/C=US/ST=New Mexico/L=Albuquerque/O=A1A Car Wash/OU=Information Technology Dep./CN=openldap" -out "%CA_DIR%\server.csr"
if errorlevel 1 goto :fail
echo subjectAltName=DNS:openldap> "%CA_DIR%\san.ext"
"%OPENSSL%" x509 -req -in "%CA_DIR%\server.csr" -CA "%CA_DIR%\default-ca.pem" -CAkey "%CA_DIR%\default-ca-key.pem" -CAcreateserial -days %DAYS% -extfile "%CA_DIR%\san.ext" -out "%CERT_DIR%\server.crt"
if errorlevel 1 goto :fail

echo ==^> 4/4 Generate DH params dhparam.pem (2048 bit, may take a while)
"%OPENSSL%" dhparam -out "%CERT_DIR%\dhparam.pem" 2048
if errorlevel 1 goto :fail

REM The container uses a symlink to the CA cert here; on Windows copy instead.
copy /y "%CA_DIR%\default-ca.pem" "%CERT_DIR%\springframework.org.ca.crt" >nul

REM Clean up intermediate files (CA key is kept in .ca for future re-signing;
REM never commit it - the whole data\certificates folder is gitignored).
del /q "%CA_DIR%\server.csr" "%CA_DIR%\san.ext" "%CA_DIR%\default-ca.srl" 2>nul

echo.
echo Done. Files written to %CERT_DIR%:
dir /b "%CERT_DIR%"
echo.
echo Note: %CA_DIR% contains the CA private key for local re-signing only.
exit /b 0

:fail
echo [ERROR] certificate generation failed.
exit /b 1
