@echo off

setlocal



REM One-click publish: pulls, commits, and pushes changes to origin/main.

REM Usage:

REM   publish_docs.bat

REM   publish_docs.bat "your commit message"



set SCRIPT_DIR=%~dp0

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%tools\publish_docs.ps1" -Message "%~1"

set EXITCODE=%ERRORLEVEL%

if not %EXITCODE%==0 (

  echo.

  echo Publish failed with exit code %EXITCODE%.

  pause

)

exit /b %EXITCODE%

