# MySQL restore script (Windows)
# Usage: $env:DB_PASSWORD="root password"; .\scripts\restore-db.ps1 -BackupFile .\backups\myshop-20260101-120000.sql
param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [string]$DbName = "myshop",
    [string]$Service = "mysql",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
if (-not $env:DB_PASSWORD) {
    throw "Please set environment variable DB_PASSWORD (MySQL root password) first"
}
if (-not (Test-Path $BackupFile)) {
    throw "Backup file not found: $BackupFile"
}
if (-not $Force) {
    $answer = Read-Host "Restore will overwrite data in database [$DbName]. Type YES to continue"
    if ($answer -ne "YES") {
        "Restore cancelled"
        exit 1
    }
}

# Resolve the container id through the compose service name instead of a fixed container name
$containerId = (docker compose ps -q $Service).Trim()
if (-not $containerId) {
    throw "Compose service is not running: $Service"
}

$remoteFile = "/tmp/myshop-restore.sql"
"Uploading backup file to container $containerId"
docker cp $BackupFile "${containerId}:$remoteFile"
try {
    "Restoring database $DbName"
    docker exec $containerId sh -c "mysql -u root -p'$env:DB_PASSWORD' --default-character-set=utf8mb4 $DbName < $remoteFile"
    "Restore finished: $BackupFile"
}
finally {
    docker exec $containerId rm -f $remoteFile | Out-Null
}
