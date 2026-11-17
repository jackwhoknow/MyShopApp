# MySQL backup script (Windows)
# Usage: $env:DB_PASSWORD="password"; .\scripts\backup-db.ps1
param(
    [string]$BackupDir = ".\backups",
    [int]$KeepDays = 7,
    [string]$DbName = "myshop",
    [string]$DbUser = "root",
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$Mysqldump = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
)

$ErrorActionPreference = "Stop"
if (-not $env:DB_PASSWORD) {
    throw "Please set environment variable DB_PASSWORD first"
}
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$target = Join-Path $BackupDir "$DbName-$timestamp.sql"

$env:MYSQL_PWD = $env:DB_PASSWORD
& $Mysqldump -h $DbHost -P $DbPort -u $DbUser --single-transaction --routines --triggers --set-gtid-purged=OFF --default-character-set=utf8mb4 $DbName |
    Out-File -Encoding utf8 $target

Get-Item $target | ForEach-Object { "Backup finished: $($_.FullName) ($([math]::Round($_.Length / 1MB, 2)) MB)" }

# Remove backups older than KeepDays
$expireBefore = (Get-Date).AddDays(-$KeepDays)
Get-ChildItem -Path $BackupDir -Filter "$DbName-*.sql" |
    Where-Object { $_.LastWriteTime -lt $expireBefore } |
    Remove-Item -Force
"Removed backups older than $KeepDays days"
