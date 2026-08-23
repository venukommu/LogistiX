package org.logistix.domain.action;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Production-grade trusted implementation of ActionAuthorizationIssuer.
 * Mints authentic, tamper-evident AuthorizedAction instances with cryptographic SHA-256 fingerprint,
 * exact-boundary TTL, and reference authorization provenance.
 */
public class DefaultActionAuthorizationIssuer implements ActionAuthorizationIssuer {

    private final String issuerAuthorityId;

    public DefaultActionAuthorizationIssuer() {
        this("LogistiX-Governance-Authority");
    }

    public DefaultActionAuthorizationIssuer(String issuerAuthorityId) {
        this.issuerAuthorityId = (issuerAuthorityId != null && !issuerAuthorityId.isBlank())
                ? issuerAuthorityId : "LogistiX-Governance-Authority";
    }

    public String getIssuerAuthorityId() {
        return issuerAuthorityId;
    }

    @Override
    public AuthorizedAction issue(
            ActionProposal proposal,
            String policyApplied,
            String authorizedBy,
            Duration validityDuration,
            Instant issuedAt
    ) {
        Objects.requireNonNull(proposal, "ActionProposal must not be null");
        Instant start = issuedAt != null ? issuedAt : Instant.now();
        Duration ttl = (validityDuration != null && !validityDuration.isNegative() && !validityDuration.isZero())
                ? validityDuration : Duration.ofMinutes(5);
        Instant expiration = start.plus(ttl);
        String token = "AUTH-LGX-" + UUID.randomUUID();
        AuthorizationProvenance provenance = AuthorizationProvenance.of(issuerAuthorityId, start);

        String fingerprint = AuthorizedAction.computeFingerprint(
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                policyApplied != null ? policyApplied : "DEFAULT_POLICY",
                proposal.correlationId(),
                proposal.idempotencyKey(),
                expiration,
                provenance.issuerId()
        );

        return AuthorizedAction.createInternal(
                proposal.actionId(),
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                token,
                fingerprint,
                provenance,
                authorizedBy != null ? authorizedBy : "LogistiX-Governance",
                policyApplied != null ? policyApplied : "DEFAULT_POLICY",
                proposal.correlationId(),
                proposal.idempotencyKey(),
                start,
                expiration
        );
    }
}
