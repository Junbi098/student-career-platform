param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [switch]$IncludeDemo
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

function Assert-Equal($Actual, $Expected, [string]$Message) {
    if ($Actual -ne $Expected) {
        throw "$Message : attendu $Expected, obtenu $Actual"
    }
}

function Assert-HttpStatus([string]$Path, [int]$Expected) {
    try {
        $null = Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Get
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        Assert-Equal ([int]$_.Exception.Response.StatusCode) $Expected "Statut HTTP de $Path"
        return
    }
    throw "La requete $Path aurait du retourner HTTP $Expected"
}

$status = Invoke-RestMethod "$BaseUrl/api/status"
Assert-Equal $status.status 'UP' 'Application disponible'
Assert-Equal $status.storage 'POSTGRESQL' 'Stockage persistant'

if (-not $IncludeDemo) {
    $page = Invoke-RestMethod "$BaseUrl/api/companies?size=2"
    if ($page.items.Count -gt 0) {
        $detail = Invoke-RestMethod "$BaseUrl/api/companies/$($page.items[0].id)"
        Assert-Equal $detail.id $page.items[0].id 'Lecture du dossier'
    }
    Assert-HttpStatus '/api/companies?size=101' 400
    Assert-HttpStatus '/api/companies/00000000-0000-0000-0000-000000000000' 404
    Write-Output 'API validee en lecture seule : PostgreSQL, catalogue, detail et erreurs 400/404.'
    return
}

# Demo mode must first be explicitly enabled in an isolated environment.
# Explicitly load fictional bundled data; never read a user-supplied file or web URL.
$report = Invoke-RestMethod "$BaseUrl/api/imports/demo" -Method Post
Assert-Equal $report.mode 'FICTIONAL_DEMO' 'Nature des donnees'
Assert-Equal $report.total 4 'Entrees analysees'
Assert-Equal $report.summary.SUCCESS 2 'Reussites'
Assert-Equal $report.summary.POSSIBLE_DUPLICATE 1 'Doublons possibles'
Assert-Equal $report.summary.NO_COMPANY 1 'Entreprise absente'
Assert-Equal $report.summary.READ_ERROR 0 'Erreurs de lecture'
Assert-Equal $report.summary.INVALID_SOURCE 0 'Sources invalides'

$page = Invoke-RestMethod "$BaseUrl/api/companies?size=2"
if ($page.totalElements -lt 3) { throw 'Au moins trois dossiers de demonstration sont attendus.' }
Assert-Equal $page.totalPages ([int][Math]::Ceiling($page.totalElements / 2.0)) 'Pages'
Assert-Equal $page.items.Count 2 'Elements de la premiere page'
$all = Invoke-RestMethod "$BaseUrl/api/companies?size=100"
$fixtures = @($all.items | Where-Object { $_.source.kind -eq 'FIXTURE' })
Assert-Equal $fixtures.Count 3 'Dossiers fictifs conserves'
$id = $fixtures[0].id
$detail = Invoke-RestMethod "$BaseUrl/api/companies/$id"
Assert-Equal $detail.source.kind 'FIXTURE' 'Provenance fictive'
Assert-Equal $detail.source.url 'classpath:/demo/company-complete.html' 'Source du dossier'

$filtered = Invoke-RestMethod "$BaseUrl/api/companies?q=ISLAND&industry=software&location=EBENE"
Assert-Equal $filtered.totalElements 2 'Recherche combinee'
$second = Invoke-RestMethod "$BaseUrl/api/companies?page=1&size=2"
Assert-Equal $second.items.Count ([Math]::Min(2, $page.totalElements - 2)) 'Deuxieme page'

$null = Invoke-RestMethod "$BaseUrl/api/imports/demo" -Method Post
$again = Invoke-RestMethod "$BaseUrl/api/companies"
Assert-Equal $again.totalElements $page.totalElements 'Reimport sans ajout de dossiers'
Assert-Equal (@($again.items | Where-Object { $_.source.kind -eq 'FIXTURE' })[0].id) $id 'Identifiant conserve'

Assert-HttpStatus '/api/companies?size=101' 400
Assert-HttpStatus '/api/companies/00000000-0000-0000-0000-000000000000' 404
Write-Output 'API validee : import fictif, 3 dossiers fictifs, recherche, pagination, reimport, erreurs 400/404.'
