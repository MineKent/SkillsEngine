@echo off
setlocal

REM Run this from the repo root (SkillsEngine).
REM It will publish the git repository located in the subfolder .\wiki
REM Usage:
REM   publish_wiki.bat
REM   publish_wiki.bat "your commit message"

set SCRIPT_DIR=%~dp0
set REPO_PATH=%SCRIPT_DIR%wiki

if not exist "%REPO_PATH%\.git" (
  echo Expected a git repo at "%REPO_PATH%" but .git was not found.
  echo Make sure the wiki repository is located in a subfolder named "wiki".
  exit /b 2
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%publish_wiki.ps1" -RepoPath "%REPO_PATH%" -Message "%~1" -Branch main
set EXITCODE=%ERRORLEVEL%
if not %EXITCODE%==0 (
  echo.
  echo Publish failed with exit code %EXITCODE%.
  pause
)
exit /b %EXITCODE%
