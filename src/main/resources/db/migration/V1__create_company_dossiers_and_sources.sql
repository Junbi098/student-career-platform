CREATE TABLE source_references (
    id UUID PRIMARY KEY,
    source_url TEXT NOT NULL,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    source_kind VARCHAR(32) NOT NULL CHECK (source_kind IN ('FIXTURE', 'LOCAL_DOCUMENT', 'WEB_OBSERVATION', 'UNCLASSIFIED')),
    accessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_updated_at TIMESTAMP WITH TIME ZONE,
    license VARCHAR(128),
    verification_status VARCHAR(32) NOT NULL CHECK (verification_status IN ('FICTIONAL', 'UNVERIFIED')),
    verified_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT source_verification_consistency CHECK (
        ((source_kind = 'FIXTURE' AND verification_status = 'FICTIONAL') OR
         (source_kind <> 'FIXTURE' AND verification_status = 'UNVERIFIED')) AND verified_at IS NULL
    )
);

-- One observation/dossier per source. Possible duplicates are not confirmed company identities.
CREATE TABLE company_dossiers (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL UNIQUE REFERENCES source_references(id),
    name VARCHAR(512) NOT NULL CHECK (length(trim(name)) > 0),
    website TEXT,
    industry TEXT,
    location TEXT,
    import_status VARCHAR(32) NOT NULL CHECK (import_status IN ('SUCCESS', 'POSSIBLE_DUPLICATE')),
    duplicate_of TEXT,
    import_order BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL UNIQUE,
    CONSTRAINT duplicate_consistency CHECK (
        (import_status = 'POSSIBLE_DUPLICATE' AND duplicate_of IS NOT NULL) OR
        (import_status = 'SUCCESS' AND duplicate_of IS NULL)
    )
);
