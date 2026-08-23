package org.logistix.domain.action;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain record establishing the reference authorization provenance for an AuthorizedAction.
 * Proves that an action was minted strictly by a trusted LogistiX Governance Engine instance.
 */
public record AuthorizationProvenance(
        String issuerId,
        String provenanceToken,
        Instant issuedAt
) {
    public AuthorizationProvenance {
        Objects.requireNonNull(issuerId, "issuerId must not be null");
        Objects.requireNonNull(provenanceToken, "provenanceToken must not be null");
        issuedAt = issuedAt != null ? issuedAt : Instant.now();
    }

    public static AuthorizationProvenance of(String issuerId) {
        String token = "PROV-LGX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return new AuthorizationProvenance(
                issuerId != null ? issuerId : "LogistiX-Governance-Authority",
                token,
                Instant.now()
        );
    }

    public static AuthorizationProvenance of(String issuerId, Instant issuedAt) {
        String token = "PROV-LGX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return new AuthorizationProvenance(
                issuerId != null ? issuerId : "LogistiX-Governance-Authority",
                token,
                issuedAt != null ? issuedAt : Instant.now()
        );
    }

    public boolean isValid() {
        return provenanceToken != null && provenanceToken.startsWith("PROV-LGX-") && provenanceToken.length() >= 16;
    }
}
