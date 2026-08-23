package org.logistix.mcp;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process registry of trusted LogistiX Authorization Authorities.
 * Validates that an AuthorizedAction was issued by an active, recognized governance authority instance.
 */
public class AuthorizationAuthorityRegistry {

    private final Set<String> registeredAuthorities = ConcurrentHashMap.newKeySet();

    public static AuthorizationAuthorityRegistry withStandardAuthorities() {
        AuthorizationAuthorityRegistry registry = new AuthorizationAuthorityRegistry();
        registry.registerAuthority("LogistiX-Governance-Authority");
        registry.registerAuthority("LogistiX-Authority-Primary");
        return registry;
    }

    public static AuthorizationAuthorityRegistry empty() {
        return new AuthorizationAuthorityRegistry();
    }

    public void registerAuthority(String authorityId) {
        Objects.requireNonNull(authorityId, "authorityId must not be null");
        registeredAuthorities.add(authorityId);
    }

    public boolean isRegisteredAuthority(String authorityId) {
        if (authorityId == null) return false;
        return registeredAuthorities.contains(authorityId);
    }

    public Set<String> getRegisteredAuthorities() {
        return Collections.unmodifiableSet(registeredAuthorities);
    }
}
