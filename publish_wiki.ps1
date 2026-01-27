param(
  # By default, publish the repo in .\wiki relative to this script (SkillsEngine\wiki)
  [string]$RepoPath = (Join-Path $PSScriptRoot 'wiki'),
  [string]$Message,
  [string]$Branch = 'main'
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

if (-not (Test-Path -LiteralPath $RepoPath -PathType Container)) {
  throw "RepoPath does not exist: $RepoPath"
}

Set-Location -LiteralPath $RepoPath

# Show what we are publishing
$top = (& git rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -ne 0) {
  throw "'$RepoPath' is not a git repository. Expected .git inside this folder."
}
Write-Host ("Publishing repo at: " + $top) -ForegroundColor Green

# If working tree is dirty, stash first so pull --rebase can proceed
$dirty = (& git status --porcelain)
$didStash = $false
if ($dirty) {
  Write-Host "Working tree has local changes. Stashing before pull --rebase..." -ForegroundColor Yellow
  ExecGit stash 'push' '-u' '-m' 'auto-stash before publish_wiki'
  $didStash = $true
}

ExecGit fetch origin
ExecGit pull '--rebase' origin $Branch

if ($didStash) {
  Write-Host "Restoring stashed changes..." -ForegroundColor Yellow
  ExecGit stash 'pop'
}

ExecGit add '-A'

& git diff --cached --quiet
if ($LASTEXITCODE -eq 0) {
  Write-Host "No changes to commit. Nothing to push." -ForegroundColor Yellow
  exit 0
}

if ([string]::IsNullOrWhiteSpace($Message)) {
  $Message = "wiki: update " + (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')
}

ExecGit commit '-m' $Message
ExecGit push origin "HEAD:$Branch"

Write-Host "Done." -ForegroundColor Green
