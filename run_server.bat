@echo off
cd server
if exist ".venv\Scripts\node.exe" (
    echo [INFO] Starting server using virtual environment...
    ".venv\Scripts\node.exe" start_server.js
) else (
    echo [ERROR] Virtual environment not found. Please run install_server.bat first.
    pause
)
