package org.logistix.domain.action;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Technology-neutral in-process reference trust registry of authorized LogistiX Governance Authorization Authorities.
 *
 * <p><strong>Architectural Role & Scope:</strong></p>
 * <ul>
 *   <li>This registry represents an in-process reference trust registry that manages startup configuration,
 *       fail-fast validation, and immutable runtime authority lookup for LogistiX.</li>
 *   <li>It is <strong>NOT</strong> a distributed identity service, enterprise IAM (OAuth/OIDC/Keycloak),
 *       persistent database repository, or cryptographic trust root.</li>
 *   <li>It enforces the core LogistiX trust lifecycle:
 *       <code>configure &rarr; validate &rarr; freeze &rarr; runtime read-only</code>.</li>
 *   <li>Production deployments may replace or augment this reference model with enterprise IAM,
 *       distributed key management (KMS/HSM), and cryptographic payload signing envelopes without
 *       altering the technology-neutral domain authorization boundary.</li>
 * </ul>
 */
public class AuthorizationAuthorityRegistry {

    private final Set<String> registeredAuthorities = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean frozen = new AtomicBoolean(false);

    public static AuthorizationAuthorityRegistry withStandardAuthorities() {
        AuthorizationAuthorityRegistry registry = new AuthorizationAuthorityRegistry();
        registry.registerAuthority("LogistiX-Governance-Authority");
        registry.registerAuthority("LogistiX-Authority-Primary");
        registry.freeze();
        return registry;
    }

    public static AuthorizationAuthorityRegistry empty() {
        return new AuthorizationAuthorityRegistry();
    }

    public synchronized void registerAuthority(String authorityId) {
        if (frozen.get()) {
            throw new IllegalStateException("Security Guardrail: Cannot register authority [" + authorityId +
                    "]. AuthorizationAuthorityRegistry is frozen and immutable.");
        }
        if (authorityId == null || authorityId.isBlank()) {
            throw new IllegalArgumentException("authorityId must not be null or blank");
        }
        if (registeredAuthorities.contains(authorityId)) {
            throw new IllegalArgumentException("Authority [" + authorityId + "] is already registered. Duplicate registrations are prohibited.");
        }
        registeredAuthorities.add(authorityId);
    }

    public void freeze() {
        frozen.set(true);
    }

    public boolean isFrozen() {
        return frozen.get();
    }

    public boolean isRegisteredAuthority(String authorityId) {
        if (authorityId == null || authorityId.isBlank()) return false;
        return registeredAuthorities.contains(authorityId);
    }

    public Set<String> getRegisteredAuthorities() {
        return Set.copyOf(registeredAuthorities);
    }
}
