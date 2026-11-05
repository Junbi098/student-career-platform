$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDirectory = Join-Path $projectRoot '.local'
$secretsFile = Join-Path $localDirectory 'database-secrets.json'
if (-not (Test-Path -LiteralPath $secretsFile)) { throw 'Lancer Initialize-Database.ps1 en premier.' }
$settings = Get-Content -LiteralPath $secretsFile -Raw | ConvertFrom-Json
$dataDirectory = Join-Path $localDirectory 'postgres-data'
$pgctl = Join-Path $settings.binDirectory 'pg_ctl.exe'
& $pgctl -D $dataDirectory status *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Output 'PostgreSQL du projet fonctionne deja.'
} else {
& $pgctl -D $dataDirectory -l (Join-Path $localDirectory 'postgres.log') -o "-h 127.0.0.1 -p $($settings.port)" -w start
if ($LASTEXITCODE -ne 0) { throw 'Demarrage PostgreSQL du projet impossible.' }
}
