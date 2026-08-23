package org.logistix.domain.action;

import java.time.Duration;
import java.time.Instant;

/**
 * Domain port for issuing authentic, tamper-evident AuthorizedAction instances.
 * Only implementations running within the trusted LogistiX decision boundary may mint authorizations.
 */
public interface ActionAuthorizationIssuer {

    /**
     * Issues an authentic AuthorizedAction for an approved ActionProposal.
     */
    AuthorizedAction issue(
            ActionProposal proposal,
            String policyApplied,
            String authorizedBy,
            Duration validityDuration,
            Instant issuedAt
    );

    /**
     * Issues an authentic AuthorizedAction with default 5-minute validity duration.
     */
    default AuthorizedAction issue(ActionProposal proposal, String policyApplied, String authorizedBy) {
        return issue(proposal, policyApplied, authorizedBy, Duration.ofMinutes(5), Instant.now());
    }
}
