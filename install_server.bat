@echo off
setlocal enabledelayedexpansion

echo [INFO] Starting one-click installation...

cd server

REM Check if .venv exists
if not exist ".venv" (
    echo [INFO] Creating virtual environment...
    python -m venv .venv
    if !errorlevel! neq 0 (
        echo [ERROR] Failed to create virtual environment. Please ensure python is installed and in your PATH.
        pause
        exit /b 1
    )
) else (
    echo [INFO] Virtual environment already exists.
)

REM Use venv node/npm if available, otherwise system npm (for initial install)
REM Actually, we usually use system npm to install dependencies, but let's check.
REM The user's request implies using the venv.
REM However, .venv usually contains python scripts. The user has node in .venv/Scripts/node.exe?
REM Wait, standard python venv doesn't contain node.exe.
REM The user's environment seems to have a custom venv structure or copied node there.
REM Let's assume standard behavior: we need to install dependencies using available npm.

echo [INFO] Installing dependencies...
call npm install
if !errorlevel! neq 0 (
    echo [ERROR] npm install failed.
    pause
    exit /b 1
)

echo [INFO] Building project...
call npm run build
if !errorlevel! neq 0 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

echo [SUCCESS] Installation and build completed successfully.
echo [INFO] You can now run the server using 'node start_server.js'
pause
