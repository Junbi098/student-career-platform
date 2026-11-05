package com.studentcareer.company.domain;

import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.importing.ImportStatus;
import java.net.URI;
import java.util.UUID;

public record CompanyCatalogEntry(UUID id, CompanyDossier dossier, ImportStatus importStatus, URI duplicateOf) { }
