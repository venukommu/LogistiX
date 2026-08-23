package org.logistix.domain.action;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Test-only factory for synthesizing AuthorizedAction test doubles, tampered actions, and forged edge cases.
 * Strictly restricted to test sources; never exposed in production distributions.
 */
public final class AuthorizedActionTestFactory {

    private AuthorizedActionTestFactory() {}

    public static AuthorizedAction validAction(
            ActionProposal proposal,
            String policyApplied,
            String authorizedBy,
            Duration validityDuration,
            Instant issuedAt
    ) {
        ActionAuthorizationIssuer issuer = new DefaultActionAuthorizationIssuer();
        return issuer.issue(proposal, policyApplied, authorizedBy, validityDuration, issuedAt);
    }

    public static AuthorizedAction forgedAction(
            String actionId,
            ActionType actionType,
            String targetResource,
            Map<String, Object> parameters,
            String authorizationToken,
            String authorizationFingerprint,
            AuthorizationProvenance provenance,
            String authorizedBy,
            String policyApplied,
            String correlationId,
            String idempotencyKey,
            Instant authorizedAt,
            Instant expiresAt
    ) {
        return AuthorizedAction.createInternal(
                actionId, actionType, targetResource, parameters, authorizationToken,
                authorizationFingerprint, provenance != null ? provenance : AuthorizationProvenance.of(authorizedBy),
                authorizedBy, policyApplied, correlationId, idempotencyKey, authorizedAt, expiresAt
        );
    }

    public static AuthorizedAction expiredAction(ActionProposal proposal, Instant expiredAt) {
        Instant issued = expiredAt.minus(Duration.ofMinutes(10));
        return AuthorizedAction.createInternal(
                proposal.actionId(),
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                "AUTH-LGX-EXPIRED",
                "",
                AuthorizationProvenance.of("LogistiX-Governance-Authority", issued),
                "LogistiX-Governance",
                "POLICY",
                proposal.correlationId(),
                proposal.idempotencyKey(),
                issued,
                expiredAt
        );
    }
}
