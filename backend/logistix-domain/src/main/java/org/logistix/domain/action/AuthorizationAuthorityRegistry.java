package org.logistix.domain.action;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-process domain registry of trusted LogistiX Authorization Authorities.
 * Enforces a strict configuration lifecycle: configure -> validate -> freeze -> runtime read-only.
 * Validates that an AuthorizedAction was issued by an active, recognized governance authority instance.
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
