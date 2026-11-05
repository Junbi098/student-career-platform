param(
    [string]$BaseUrl = 'http://127.0.0.1:8080'
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
Assert-Equal $status.storage 'IN_MEMORY' 'Stockage temporaire'

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
Assert-Equal $page.totalElements 3 'Dossiers conserves'
Assert-Equal $page.totalPages 2 'Pages'
Assert-Equal $page.items.Count 2 'Elements de la premiere page'
$id = $page.items[0].id
$detail = Invoke-RestMethod "$BaseUrl/api/companies/$id"
Assert-Equal $detail.source.kind 'FIXTURE' 'Provenance fictive'
Assert-Equal $detail.source.url 'classpath:/demo/company-complete.html' 'Source du dossier'

$filtered = Invoke-RestMethod "$BaseUrl/api/companies?q=ISLAND&industry=software&location=EBENE"
Assert-Equal $filtered.totalElements 2 'Recherche combinee'
$second = Invoke-RestMethod "$BaseUrl/api/companies?page=1&size=2"
Assert-Equal $second.items.Count 1 'Derniere page'

$null = Invoke-RestMethod "$BaseUrl/api/imports/demo" -Method Post
$again = Invoke-RestMethod "$BaseUrl/api/companies"
Assert-Equal $again.totalElements 3 'Reimport sans ajout de dossiers'
Assert-Equal $again.items[0].id $id 'Identifiant conserve'

Assert-HttpStatus '/api/companies?size=101' 400
Assert-HttpStatus '/api/companies/00000000-0000-0000-0000-000000000000' 404
Write-Output 'API validee : import fictif, 3 dossiers, recherche, pagination, reimport, erreurs 400/404.'
