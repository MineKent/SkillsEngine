param(
  # HTTPS URL вида: https://github.com/MineKent/SkillsEngine.git
  [Parameter(Mandatory = $false)]
  [string]$RemoteUrl = "https://github.com/MineKent/SkillsEngine.git",

  [Parameter(Mandatory = $false)]
  [string]$RemoteName = "origin",

  [Parameter(Mandatory = $false)]
  [string]$Branch = "main",

  [Parameter(Mandatory = $false)]
  [string]$CommitMsg = "Initial commit",

  # Опционально: можно сразу указать имя/почту для git (иначе git commit упадет)
  [Parameter(Mandatory = $false)]
  [string]$GitUserName = "MineKent",

  [Parameter(Mandatory = $false)]
  [string]$GitUserEmail = "sazhnev.arseniy0909@bk.ru"
)

$ErrorActionPreference = "Stop"

function ExecGit {
  param(
    [Parameter(Mandatory = $true)][string]$Verb,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$Args
  )

  $pretty = ($Args | ForEach-Object {
    if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
  }) -join ' '
  Write-Host "> git $Verb $pretty"

  & git $Verb @Args
  if ($LASTEXITCODE -ne 0) {
    throw ("git failed with exit code " + $LASTEXITCODE + ": git " + $Verb + " " + $pretty)
  }
}

# 1) git init (если репо еще не инициализирован)
try {
  git rev-parse --is-inside-work-tree *> $null
  $isRepo = ($LASTEXITCODE -eq 0)
} catch {
  $isRepo = $false
}

if (-not $isRepo) {
  ExecGit init
}

# 2) настроить/обновить remote
$existingUrl = ""
try {
  $existingUrl = (git remote get-url $RemoteName 2>$null)
} catch {
  $existingUrl = ""
}

if ([string]::IsNullOrWhiteSpace($existingUrl)) {
  ExecGit remote add $RemoteName $RemoteUrl
} else {
  if ($existingUrl.Trim() -ne $RemoteUrl.Trim()) {
    Write-Host "Remote '$RemoteName' already exists: $existingUrl"
    Write-Host "Updating it to: $RemoteUrl"
    ExecGit remote set-url $RemoteName $RemoteUrl
  }
}

# 3) первый коммит (если еще нет)

# Git требует, чтобы были настроены user.name и user.email
$existingName  = (git config --global user.name  2>$null)
$existingEmail = (git config --global user.email 2>$null)

if ([string]::IsNullOrWhiteSpace($existingName) -or [string]::IsNullOrWhiteSpace($existingEmail)) {
  if ([string]::IsNullOrWhiteSpace($GitUserName) -or [string]::IsNullOrWhiteSpace($GitUserEmail)) {
    Write-Host "Нужно один раз настроить Git user.name и user.email." -ForegroundColor Yellow
    Write-Host "Вариант 1 (рекомендуется):" -ForegroundColor Yellow
    Write-Host "  git config --global user.name  \"Ваше Имя\""
    Write-Host "  git config --global user.email \"you@example.com\""
    Write-Host "Вариант 2: запустить этот скрипт с параметрами:" -ForegroundColor Yellow
    Write-Host "  powershell -ExecutionPolicy Bypass -File scripts\\init-github.ps1 -GitUserName \"Ваше Имя\" -GitUserEmail \"you@example.com\""
    throw "Git user.name/user.email не настроены"
  }

  ExecGit config --global user.name $GitUserName
  ExecGit config --global user.email $GitUserEmail
}

ExecGit add .

# Если staged изменений нет — просто продолжим (может вы уже коммитили)
& git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
  ExecGit commit -m $CommitMsg
} else {
  Write-Host "No staged changes for initial commit (maybe already committed)."
}

# 4) ветка main и пуш
ExecGit branch -M $Branch
ExecGit push -u $RemoteName $Branch

Write-Host "Done. Repo is pushed to $RemoteName/$Branch."
Write-Host "Next time to push only docs: powershell -ExecutionPolicy Bypass -File scripts\\push-docs.ps1"
