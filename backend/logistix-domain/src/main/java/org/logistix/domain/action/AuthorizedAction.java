package org.logistix.domain.action;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Immutable, tamper-evident tokenized representation of an Action that has been deterministically validated and
 * authorized by LogistiX Governance.
 *
 * Implements exact action binding via a canonical SHA-256 authorization fingerprint and time-to-live (TTL) expiration window.
 * ONLY an AuthorizedAction may be accepted and executed by an ActionExecutor.
 */
public record AuthorizedAction(
        String actionId,
        ActionType actionType,
        String targetResource,
        Map<String, Object> parameters,
        String authorizationToken,
        String authorizationFingerprint,
        String authorizedBy,
        String policyApplied,
        String correlationId,
        String idempotencyKey,
        Instant authorizedAt,
        Instant expiresAt
) {
    public AuthorizedAction {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(targetResource, "targetResource must not be null");
        Objects.requireNonNull(authorizationToken, "authorizationToken must not be null");

        // Total defensive copy of parameters into unmodifiable map
        parameters = parameters != null ? Collections.unmodifiableMap(new LinkedHashMap<>(parameters)) : Collections.emptyMap();
        authorizedBy = authorizedBy != null ? authorizedBy : "LogistiX-Governance";
        policyApplied = policyApplied != null ? policyApplied : "DEFAULT_POLICY";
        correlationId = correlationId != null ? correlationId : actionId;
        idempotencyKey = idempotencyKey != null ? idempotencyKey : actionId;
        authorizedAt = authorizedAt != null ? authorizedAt : Instant.now();
        expiresAt = expiresAt != null ? expiresAt : authorizedAt.plus(Duration.ofMinutes(5));

        if (authorizationFingerprint == null || authorizationFingerprint.isBlank()) {
            authorizationFingerprint = computeFingerprint(
                    actionType, targetResource, parameters, policyApplied,
                    correlationId, idempotencyKey, expiresAt
            );
        }
    }

    /**
     * Issues an AuthorizedAction from a validated ActionProposal with a defined validity duration.
     */
    public static AuthorizedAction issue(
            ActionProposal proposal,
            String policyApplied,
            String authorizedBy,
            Duration validityDuration,
            Instant issuedAt
    ) {
        Objects.requireNonNull(proposal, "proposal must not be null");
        Instant start = issuedAt != null ? issuedAt : Instant.now();
        Duration ttl = (validityDuration != null && !validityDuration.isNegative() && !validityDuration.isZero())
                ? validityDuration : Duration.ofMinutes(5);
        Instant expiration = start.plus(ttl);
        String token = "AUTH-" + UUID.randomUUID();

        String fingerprint = computeFingerprint(
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                policyApplied != null ? policyApplied : "DEFAULT_POLICY",
                proposal.correlationId(),
                proposal.idempotencyKey(),
                expiration
        );

        return new AuthorizedAction(
                proposal.actionId(),
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                token,
                fingerprint,
                authorizedBy != null ? authorizedBy : "LogistiX-Governance",
                policyApplied != null ? policyApplied : "DEFAULT_POLICY",
                proposal.correlationId(),
                proposal.idempotencyKey(),
                start,
                expiration
        );
    }

    public static AuthorizedAction of(ActionProposal proposal, String policyApplied, String authorizedBy) {
        return issue(proposal, policyApplied, authorizedBy, Duration.ofMinutes(5), Instant.now());
    }

    /**
     * Evaluates whether the authorization has expired relative to a reference timestamp.
     */
    public boolean isExpired(Instant referenceTime) {
        Instant now = referenceTime != null ? referenceTime : Instant.now();
        return now.isAfter(expiresAt);
    }

    /**
     * Evaluates whether the authorization has expired relative to a system clock.
     */
    public boolean isExpired(Clock clock) {
        return isExpired(clock != null ? clock.instant() : Instant.now());
    }

    /**
     * Verifies that the current action's state matches the cryptographic SHA-256 authorization fingerprint.
     * Prevents parameter or target mutation after authorization.
     */
    public boolean matchesFingerprint() {
        String calculated = computeFingerprint(
                actionType, targetResource, parameters, policyApplied,
                correlationId, idempotencyKey, expiresAt
        );
        return Objects.equals(this.authorizationFingerprint, calculated);
    }

    /**
     * Computes canonical deterministic SHA-256 fingerprint for exact action binding.
     */
    public static String computeFingerprint(
            ActionType actionType,
            String targetResource,
            Map<String, Object> parameters,
            String policyApplied,
            String correlationId,
            String idempotencyKey,
            Instant expiresAt
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            StringBuilder canonical = new StringBuilder();
            canonical.append("TYPE:").append(actionType.code()).append("|");
            canonical.append("TARGET:").append(targetResource).append("|");
            canonical.append("POLICY:").append(policyApplied).append("|");
            canonical.append("CORR:").append(correlationId).append("|");
            canonical.append("IDEMP:").append(idempotencyKey).append("|");
            canonical.append("EXPIRES:").append(expiresAt.toEpochMilli()).append("|");
            canonical.append("PARAMS:");

            if (parameters != null && !parameters.isEmpty()) {
                // Canonical deterministic sorting of parameter keys
                TreeMap<String, Object> sortedParams = new TreeMap<>(parameters);
                for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
                    canonical.append(entry.getKey()).append("=").append(String.valueOf(entry.getValue())).append(";");
                }
            }

            byte[] hash = digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM", e);
        }
    }
}
