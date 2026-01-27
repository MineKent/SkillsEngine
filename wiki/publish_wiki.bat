@echo off
setlocal

REM Run from SkillsEngine\wiki. This script will:
REM - auto-stash local changes
REM - pull --rebase from origin/main
REM - restore changes
REM - commit and push
REM Usage:
REM   publish_wiki.bat
REM   publish_wiki.bat "your commit message"

set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%publish_wiki.ps1" -RepoPath "%SCRIPT_DIR%." -Message "%~1" -Branch main
set EXITCODE=%ERRORLEVEL%
if not %EXITCODE%==0 (
  echo.
  echo Publish failed with exit code %EXITCODE%.
  pause
)
exit /b %EXITCODE%
