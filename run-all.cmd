@echo off
rem Double-click me (Windows): runs every test in the repository and shows a summary.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all.ps1" %*
echo.
pause
