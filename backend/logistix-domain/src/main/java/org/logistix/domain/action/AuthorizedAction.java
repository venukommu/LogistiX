package org.logistix.domain.action;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, tamper-evident class representing an Action that has been deterministically validated and
 * authorized by LogistiX Governance.
 *
 * Implemented as a final class with controlled factory instantiation to prevent unauthorized manual instantiation.
 * Enforces exact action binding via recursive parameter canonicalization, reference provenance verification,
 * and exact-boundary expiration evaluation.
 */
public final class AuthorizedAction {

    private final String actionId;
    private final ActionType actionType;
    private final String targetResource;
    private final Map<String, Object> parameters;
    private final String authorizationToken;
    private final String authorizationFingerprint;
    private final AuthorizationProvenance provenance;
    private final String authorizedBy;
    private final String policyApplied;
    private final String correlationId;
    private final String idempotencyKey;
    private final Instant authorizedAt;
    private final Instant expiresAt;

    private AuthorizedAction(
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
        this.actionId = Objects.requireNonNull(actionId, "actionId must not be null");
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
        this.targetResource = Objects.requireNonNull(targetResource, "targetResource must not be null");
        this.parameters = ParameterCanonicalizer.deepUnmodifiableCopy(parameters);
        this.authorizationToken = Objects.requireNonNull(authorizationToken, "authorizationToken must not be null");
        this.provenance = Objects.requireNonNull(provenance, "provenance must not be null");
        this.authorizedBy = authorizedBy != null ? authorizedBy : "LogistiX-Governance";
        this.policyApplied = policyApplied != null ? policyApplied : "DEFAULT_POLICY";
        this.correlationId = correlationId != null ? correlationId : actionId;
        this.idempotencyKey = idempotencyKey != null ? idempotencyKey : actionId;
        this.authorizedAt = authorizedAt != null ? authorizedAt : Instant.now();
        this.expiresAt = expiresAt != null ? expiresAt : this.authorizedAt.plus(Duration.ofMinutes(5));

        if (authorizationFingerprint == null || authorizationFingerprint.isBlank()) {
            this.authorizationFingerprint = computeFingerprint(
                    this.actionType, this.targetResource, this.parameters, this.policyApplied,
                    this.correlationId, this.idempotencyKey, this.expiresAt, this.provenance.issuerId()
            );
        } else {
            this.authorizationFingerprint = authorizationFingerprint;
        }
    }

    /**
     * Controlled factory method: Issues a valid, authentic AuthorizedAction from an ActionProposal.
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
        String token = "AUTH-LGX-" + UUID.randomUUID();
        AuthorizationProvenance prov = AuthorizationProvenance.of(authorizedBy, start);

        String fingerprint = computeFingerprint(
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                policyApplied != null ? policyApplied : "DEFAULT_POLICY",
                proposal.correlationId(),
                proposal.idempotencyKey(),
                expiration,
                prov.issuerId()
        );

        return new AuthorizedAction(
                proposal.actionId(),
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                token,
                fingerprint,
                prov,
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
     * Testing factory for synthesizing adversarial edge cases (e.g. forged tokens, tampered fingerprints).
     */
    public static AuthorizedAction createForTesting(
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
        return new AuthorizedAction(
                actionId, actionType, targetResource, parameters, authorizationToken,
                authorizationFingerprint, provenance != null ? provenance : AuthorizationProvenance.of(authorizedBy),
                authorizedBy, policyApplied, correlationId, idempotencyKey, authorizedAt, expiresAt
        );
    }

    // Accessors
    public String actionId() { return actionId; }
    public ActionType actionType() { return actionType; }
    public String targetResource() { return targetResource; }
    public Map<String, Object> parameters() { return parameters; }
    public String authorizationToken() { return authorizationToken; }
    public String authorizationFingerprint() { return authorizationFingerprint; }
    public AuthorizationProvenance provenance() { return provenance; }
    public String authorizedBy() { return authorizedBy; }
    public String policyApplied() { return policyApplied; }
    public String correlationId() { return correlationId; }
    public String idempotencyKey() { return idempotencyKey; }
    public Instant authorizedAt() { return authorizedAt; }
    public Instant expiresAt() { return expiresAt; }

    /**
     * Evaluates whether the authorization is expired relative to a reference timestamp.
     * Enforces the exact-boundary rule: now >= expiresAt means EXPIRED.
     */
    public boolean isExpired(Instant referenceTime) {
        Instant now = referenceTime != null ? referenceTime : Instant.now();
        return !now.isBefore(expiresAt); // now >= expiresAt
    }

    /**
     * Evaluates whether the authorization has expired relative to a system clock.
     */
    public boolean isExpired(Clock clock) {
        return isExpired(clock != null ? clock.instant() : Instant.now());
    }

    /**
     * Verifies that the current action's state matches the cryptographic SHA-256 authorization fingerprint.
     */
    public boolean matchesFingerprint() {
        String calculated = computeFingerprint(
                actionType, targetResource, parameters, policyApplied,
                correlationId, idempotencyKey, expiresAt, provenance.issuerId()
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
            Instant expiresAt,
            String issuerId
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            StringBuilder canonical = new StringBuilder();
            canonical.append("TYPE:").append(actionType.code()).append("|");
            canonical.append("TARGET:").append(targetResource).append("|");
            canonical.append("POLICY:").append(policyApplied).append("|");
            canonical.append("ISSUER:").append(issuerId != null ? issuerId : "UNKNOWN").append("|");
            canonical.append("CORR:").append(correlationId).append("|");
            canonical.append("IDEMP:").append(idempotencyKey).append("|");
            canonical.append("EXPIRES:").append(expiresAt.toEpochMilli()).append("|");
            canonical.append("PARAMS:").append(ParameterCanonicalizer.canonicalize(parameters));

            byte[] hash = digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizedAction that = (AuthorizedAction) o;
        return Objects.equals(actionId, that.actionId) &&
                Objects.equals(actionType, that.actionType) &&
                Objects.equals(targetResource, that.targetResource) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(authorizationToken, that.authorizationToken) &&
                Objects.equals(authorizationFingerprint, that.authorizationFingerprint) &&
                Objects.equals(provenance, that.provenance) &&
                Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionId, actionType, targetResource, parameters, authorizationToken, authorizationFingerprint, provenance, expiresAt);
    }

    @Override
    public String toString() {
        return "AuthorizedAction[" +
                "actionId='" + actionId + '\'' +
                ", actionType=" + actionType +
                ", targetResource='" + targetResource + '\'' +
                ", authorizationToken='" + authorizationToken + '\'' +
                ", authorizationFingerprint='" + authorizationFingerprint + '\'' +
                ", authorizedBy='" + authorizedBy + '\'' +
                ", expiresAt=" + expiresAt +
                ']';
    }
}
