param(
  [string]$Message
)

$ErrorActionPreference = 'Stop'

function ExecGit([Parameter(ValueFromRemainingArguments=$true)][string[]]$Args) {
  $printable = 'git ' + ($Args -join ' ')
  Write-Host "> $printable" -ForegroundColor Cyan
  & git @Args
  if ($LASTEXITCODE -ne 0) {
    throw "Git command failed with exit code ${LASTEXITCODE}: $printable"
  }
}

# Ensure we're in a git repo
& git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
  throw "This folder is not a git repository. Run the script from the repo root."
}

# Ensure git exists
& git --version *> $null
if ($LASTEXITCODE -ne 0) {
  throw "git is not available in PATH. Install Git for Windows and try again."
}

# Fetch latest info
ExecGit fetch origin

# Rebase onto origin/main (keeps history linear)
ExecGit pull '--rebase' origin main

# Stage everything (not only docs: user requested 'all changes')
ExecGit add '-A'

# If no staged changes, exit successfully
& git diff --cached --quiet
if ($LASTEXITCODE -eq 0) {
  Write-Host "No changes to commit. Nothing to push." -ForegroundColor Yellow
  exit 0
}

if ([string]::IsNullOrWhiteSpace($Message)) {
  $Message = "docs: update " + (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')
}

# Commit
ExecGit commit '-m' $Message

# Push current HEAD to origin/main
ExecGit push origin HEAD:main

Write-Host "Done." -ForegroundColor Green
