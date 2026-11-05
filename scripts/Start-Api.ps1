param(
    [ValidateRange(0, 65535)]
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildDirectory = Join-Path $projectRoot 'target'
$archive = Join-Path $buildDirectory 'student-career-platform-1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) {
    throw 'Executez le goal Maven clean package dans IntelliJ avant de lancer ce script.'
}
& (Join-Path $PSScriptRoot 'Start-Database.ps1')
$socketDirectory = (Get-Item -LiteralPath $buildDirectory).FullName
# Only this Java process receives the setting; Windows and global Java settings stay untouched.
& java "-Djdk.net.unixdomain.tmpdir=$socketDirectory" -jar $archive "--server.port=$Port"
exit $LASTEXITCODE
