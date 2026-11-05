# Removes only FIXTURE records from this project's application database, within one transaction.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$settings = Get-Content -LiteralPath (Join-Path $projectRoot '.local/database-secrets.json') -Raw | ConvertFrom-Json
$psql = Join-Path $settings.binDirectory 'psql.exe'
$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $settings.appPassword
    $sql = @'
BEGIN;
DO $$
DECLARE
    before_hash TEXT;
    after_hash TEXT;
BEGIN
    IF current_database() <> 'student_career' THEN
        RAISE EXCEPTION 'Unexpected database: refusing cleanup';
    END IF;
    LOCK TABLE company_dossiers, source_references IN SHARE ROW EXCLUSIVE MODE;
    SELECT md5(coalesce(string_agg(jsonb_build_object('dossier', to_jsonb(d), 'source', to_jsonb(s))::text,
           '' ORDER BY d.id), '')) INTO before_hash
      FROM company_dossiers d JOIN source_references s ON s.id = d.source_id
     WHERE s.source_kind <> 'FIXTURE';
    DELETE FROM company_dossiers WHERE source_id IN
        (SELECT id FROM source_references WHERE source_kind = 'FIXTURE');
    DELETE FROM source_references s WHERE s.source_kind = 'FIXTURE'
        AND NOT EXISTS (SELECT 1 FROM company_dossiers d WHERE d.source_id = s.id);
    SELECT md5(coalesce(string_agg(jsonb_build_object('dossier', to_jsonb(d), 'source', to_jsonb(s))::text,
           '' ORDER BY d.id), '')) INTO after_hash
      FROM company_dossiers d JOIN source_references s ON s.id = d.source_id
     WHERE s.source_kind <> 'FIXTURE';
    IF before_hash IS DISTINCT FROM after_hash THEN
        RAISE EXCEPTION 'Non-fictional records changed: transaction cancelled';
    END IF;
END $$;
COMMIT;
SELECT s.source_kind, count(*) AS dossiers
  FROM company_dossiers d JOIN source_references s ON s.id = d.source_id
 GROUP BY s.source_kind ORDER BY s.source_kind;
'@
    $sql | & $psql -X -w -h 127.0.0.1 -p $settings.port -U student_career -d student_career -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw 'Nettoyage non valide : verifier la transaction.' }
} finally {
    if ($null -eq $previousPassword) { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue } else { $env:PGPASSWORD = $previousPassword }
}
