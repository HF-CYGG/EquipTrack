@echo off
cd /d "%~dp0server"
set "NODE_EXE=.venv\Scripts\node.exe"
if exist "%NODE_EXE%" (
    "%NODE_EXE%" node_modules\typescript\bin\tsc
    if %errorlevel% neq 0 (
        echo [ERROR] TypeScript build failed
        exit /b 1
    )
    if exist "dist\index.js" (
        "%NODE_EXE%" -r dotenv/config dist\index.js
    ) else (
        echo [ERROR] dist\index.js not found
        exit /b 1
    )
) else (
    echo [ERROR] venv Node not found: server\.venv\Scripts\node.exe
    echo [HINT] Run install_server.bat first
    exit /b 1
)
