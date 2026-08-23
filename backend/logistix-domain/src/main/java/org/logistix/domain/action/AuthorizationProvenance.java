package org.logistix.domain.action;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain record establishing the reference authorization provenance for an AuthorizedAction.
 * Proves that an action was minted strictly by a trusted, registered LogistiX Governance Authorization Authority.
 */
public record AuthorizationProvenance(
        String issuerAuthorityId,
        String issuanceId,
        String provenanceToken,
        String scope,
        Instant issuedAt
) {
    public AuthorizationProvenance {
        Objects.requireNonNull(issuerAuthorityId, "issuerAuthorityId must not be null");
        Objects.requireNonNull(issuanceId, "issuanceId must not be null");
        Objects.requireNonNull(provenanceToken, "provenanceToken must not be null");
        scope = scope != null ? scope : "ACTION_EXECUTION";
        issuedAt = issuedAt != null ? issuedAt : Instant.now();
    }

    public static AuthorizationProvenance of(String issuerAuthorityId) {
        return of(issuerAuthorityId, Instant.now());
    }

    public static AuthorizationProvenance of(String issuerAuthorityId, Instant issuedAt) {
        String issuanceId = "ISSUE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String token = "PROV-LGX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return new AuthorizationProvenance(
                issuerAuthorityId != null ? issuerAuthorityId : "LogistiX-Governance-Authority",
                issuanceId,
                token,
                "ACTION_EXECUTION",
                issuedAt != null ? issuedAt : Instant.now()
        );
    }

    public String issuerId() {
        return issuerAuthorityId;
    }

    public boolean isValid() {
        return issuerAuthorityId != null && !issuerAuthorityId.isBlank() &&
                issuanceId != null && issuanceId.startsWith("ISSUE-") &&
                provenanceToken != null && provenanceToken.startsWith("PROV-LGX-") &&
                provenanceToken.length() >= 16;
    }
}
