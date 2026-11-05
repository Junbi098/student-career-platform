$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDirectory = Join-Path $projectRoot '.local'
$settings = Get-Content -LiteralPath (Join-Path $localDirectory 'database-secrets.json') -Raw | ConvertFrom-Json
$pgctl = Join-Path $settings.binDirectory 'pg_ctl.exe'
& $pgctl -D (Join-Path $localDirectory 'postgres-data') -m fast -w stop
if ($LASTEXITCODE -ne 0) { throw 'Arret PostgreSQL du projet impossible.' }
