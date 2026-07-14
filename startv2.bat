@echo off
chcp 65001 >nul
title Family Tree Launcher

set PROJ_DIR=%~dp0
set BACKEND_DIR=%PROJ_DIR%backend
set FRONTEND_DIR=%PROJ_DIR%frontend

echo ========================================
echo   Family Tree - Launcher
echo ========================================
echo.

set PATH=C:\java\apache-maven-3.9.9\bin;C:\java\nodejs;C:\Windows\system32;C:\Windows;%PATH%

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java not found.
    pause
    exit /b 1
)

where node >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Node.js not found.
    pause
    exit /b 1
)

echo [INFO] Starting backend...
start "Backend" cmd /c "title Backend && set PATH=C:\java\apache-maven-3.9.9\bin;C:\Windows\system32;C:\Windows && cd /d %BACKEND_DIR% && mvn spring-boot:run -DskipTests"

echo [WAIT] Waiting for backend...
setlocal enabledelayedexpansion
set waited=0
:wait_loop
timeout /t 3 /nobreak >nul
set /a waited+=3
powershell -Command "try {$c=New-Object System.Net.Sockets.TcpClient; $c.ConnectAsync('127.0.0.1',8080).Wait(2000); if($c.Connected){$c.Close();exit 0}else{exit 1}}catch{exit 1}" >nul 2>nul
if !errorlevel! equ 0 (
    echo [OK] Backend ready!
    goto backend_ok
)
if !waited! geq 120 (
    echo [WARN] Backend timeout.
    goto backend_ok
)
echo [WAIT] ...(!waited!s)
goto wait_loop
:backend_ok
endlocal

echo.
echo [INFO] Setting up frontend...
if not exist "%FRONTEND_DIR%\node_modules" (
    cd /d "%FRONTEND_DIR%"
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed.
        pause
        exit /b 1
    )
)

echo [INFO] Starting frontend...
start "Frontend" cmd /c "title Frontend && set PATH=C:\java\nodejs;C:\Windows\system32;C:\Windows && cd /d %FRONTEND_DIR% && npm run dev"

timeout /t 4 /nobreak >nul
start http://localhost:5173

echo.
echo ========================================
echo   Started!
echo   Frontend: http://localhost:5173
echo   Backend:  http://localhost:8080
echo ========================================
echo.
pause
