$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDirectory = Join-Path $projectRoot '.local'
$dataDirectory = Join-Path $localDirectory 'postgres-data'
$secretsFile = Join-Path $localDirectory 'database-secrets.json'
$port = 15432
$psqlCommand = Get-Command psql -ErrorAction Stop
$binDirectory = Split-Path -Parent $psqlCommand.Source
$initdb = Join-Path $binDirectory 'initdb.exe'
$pgctl = Join-Path $binDirectory 'pg_ctl.exe'
$createdb = Join-Path $binDirectory 'createdb.exe'
$null = New-Item -ItemType Directory -Path $localDirectory -Force
function New-LocalDatabasePassword {
    $bytes = [byte[]]::new(32)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return [BitConverter]::ToString($bytes).Replace('-', '')
}
if ((Test-Path -LiteralPath (Join-Path $dataDirectory 'PG_VERSION')) -and -not (Test-Path -LiteralPath $secretsFile)) {
    throw 'Instance dediee existante sans identifiants locaux : restauration requise, aucun reglage ne sera remplace.'
}

if (Test-Path -LiteralPath $secretsFile) {
    $secrets = Get-Content -LiteralPath $secretsFile -Raw | ConvertFrom-Json
} else {
    $secrets = [pscustomobject]@{
        adminPassword = (New-LocalDatabasePassword)
        appPassword = (New-LocalDatabasePassword)
        binDirectory = $binDirectory
        port = $port
    }
    $secrets | ConvertTo-Json | Set-Content -LiteralPath $secretsFile -Encoding utf8
}
$binDirectory = $secrets.binDirectory
$pgctl = Join-Path $binDirectory 'pg_ctl.exe'
$initdb = Join-Path $binDirectory 'initdb.exe'
$createdb = Join-Path $binDirectory 'createdb.exe'
$psql = Join-Path $binDirectory 'psql.exe'
$port = [int]$secrets.port

if (-not (Test-Path -LiteralPath (Join-Path $dataDirectory 'PG_VERSION'))) {
    $probe = [Net.Sockets.TcpClient]::new()
    try {
        $probe.Connect('127.0.0.1', $port)
        throw "Le port $port est deja utilise. Aucune instance existante ne sera modifiee."
    } catch [Net.Sockets.SocketException] {
        # The dedicated port is free.
    } finally {
        $probe.Dispose()
    }
    $passwordFile = Join-Path $localDirectory 'init-password.tmp'
    try {
        [IO.File]::WriteAllText($passwordFile, $secrets.adminPassword, [Text.UTF8Encoding]::new($false))
        & $initdb -D $dataDirectory -U student_career_admin --encoding=UTF8 --locale=C --auth-host=scram-sha-256 --auth-local=scram-sha-256 "--pwfile=$passwordFile" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Initialisation PostgreSQL impossible.' }
    } finally {
        if (Test-Path -LiteralPath $passwordFile) { Remove-Item -LiteralPath $passwordFile }
    }
}

& $pgctl -D $dataDirectory status *> $null
if ($LASTEXITCODE -ne 0) {
    & $pgctl -D $dataDirectory -l (Join-Path $localDirectory 'postgres.log') -o "-h 127.0.0.1 -p $port" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Demarrage PostgreSQL impossible.' }
}

$previousPassword = $env:PGPASSWORD
$setupSql = Join-Path $localDirectory 'setup-role.tmp.sql'
try {
    $env:PGPASSWORD = $secrets.adminPassword
    $exists = & $psql -X -w -h 127.0.0.1 -p $port -U student_career_admin -d postgres -Atc "SELECT 1 FROM pg_roles WHERE rolname = 'student_career'"
    if ($LASTEXITCODE -ne 0) { throw 'Connexion a la base dediee impossible.' }
    if ($exists -ne '1') {
        $sql = "CREATE ROLE student_career LOGIN PASSWORD '$($secrets.appPassword)' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;"
        [IO.File]::WriteAllText($setupSql, $sql, [Text.UTF8Encoding]::new($false))
        & $psql -X -w -h 127.0.0.1 -p $port -U student_career_admin -d postgres -v ON_ERROR_STOP=1 -f $setupSql
        if ($LASTEXITCODE -ne 0) { throw 'Creation du compte applicatif impossible.' }
    }
    foreach ($database in @('student_career', 'student_career_test')) {
        $exists = & $psql -X -w -h 127.0.0.1 -p $port -U student_career_admin -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname = '$database'"
        if ($LASTEXITCODE -ne 0) { throw 'Verification de la base impossible.' }
        if ($exists -ne '1') {
            & $createdb -w -h 127.0.0.1 -p $port -U student_career_admin -O student_career $database
            if ($LASTEXITCODE -ne 0) { throw "Creation de $database impossible." }
        }
    }
    $properties = "spring.datasource.url=jdbc:postgresql://127.0.0.1:$port/student_career`nspring.datasource.username=student_career`nspring.datasource.password=$($secrets.appPassword)`n"
    [IO.File]::WriteAllText((Join-Path $localDirectory 'database.properties'), $properties, [Text.UTF8Encoding]::new($false))
} finally {
    if ($null -eq $previousPassword) { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue } else { $env:PGPASSWORD = $previousPassword }
    if (Test-Path -LiteralPath $setupSql) { Remove-Item -LiteralPath $setupSql }
}
Write-Output "PostgreSQL du projet pret sur 127.0.0.1:$port. Bases application et tests separees."
Write-Output 'Identifiants locaux enregistres dans .local, exclu de Git. Aucun mot de passe affiche.'
