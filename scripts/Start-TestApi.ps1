param(
    [ValidateRange(0, 65535)]
    [int]$Port = 8081
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$archive = Join-Path $projectRoot 'target/student-career-platform-1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $archive)) { throw 'Executer clean package dans Maven en premier.' }
& (Join-Path $PSScriptRoot 'Start-Database.ps1')
$settings = Get-Content -LiteralPath (Join-Path $projectRoot '.local/database-secrets.json') -Raw | ConvertFrom-Json
$testUrl = "jdbc:postgresql://127.0.0.1:$($settings.port)/student_career_test"
Write-Output 'API DE TEST : student_career_test ; les exemples ne vont pas dans le catalogue reel.'
& java "-Djdk.net.unixdomain.tmpdir=$projectRoot/target" -jar $archive "--server.port=$Port" "--spring.datasource.url=$testUrl" --app.demo.enabled=true
exit $LASTEXITCODE
