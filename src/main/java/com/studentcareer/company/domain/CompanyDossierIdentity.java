package com.studentcareer.company.domain;

import com.studentcareer.source.model.SourceReference;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class CompanyDossierIdentity {
    private CompanyDossierIdentity() { }

    public static UUID from(SourceReference source) {
        return UUID.nameUUIDFromBytes((source.getKind() + "|" + source.getUrl())
                .getBytes(StandardCharsets.UTF_8));
    }
}
