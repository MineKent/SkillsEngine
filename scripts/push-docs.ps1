param(
  [string]$DocsDir = "docs",
  [string]$Remote  = "origin",
  [string]$Branch  = "main",
  [string]$CommitMsg = ("docs: update " + (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ"))
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

if (!(Test-Path $DocsDir)) {
  throw "Папка '$DocsDir' не найдена. Убедитесь, что docs/ существует."
}

# Проверка, что это git репозиторий
& git rev-parse --is-inside-work-tree *> $null
if ($LASTEXITCODE -ne 0) {
  throw "Это не git-репозиторий. Сначала запустите: powershell -File scripts\\init-github.ps1"
}

# Проверка remote
& git remote get-url $Remote *> $null
if ($LASTEXITCODE -ne 0) {
  throw "Не настроен remote '$Remote'. Сначала запустите: powershell -File scripts\\init-github.ps1"
}

# Переключение на ветку
& git fetch $Remote *> $null
ExecGit checkout $Branch

# Добавляем только docs
ExecGit add $DocsDir

# Если staged изменений нет — выходим
& git diff --cached --quiet
if ($LASTEXITCODE -eq 0) {
  Write-Host "Изменений в '$DocsDir' нет — пушить нечего."
  exit 0
}

ExecGit commit -m $CommitMsg
ExecGit push $Remote $Branch

Write-Host "Готово: '$DocsDir' запушена в $Remote/$Branch"
