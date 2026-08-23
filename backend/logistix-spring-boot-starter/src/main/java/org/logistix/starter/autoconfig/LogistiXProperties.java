package org.logistix.starter.autoconfig;

import org.logistix.domain.action.ActionType;
import org.logistix.engine.configuration.TraceLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Spring Boot Configuration Properties for LogistiX.
 */
@ConfigurationProperties(prefix = "logistix")
public class LogistiXProperties {

    private boolean enabled = true;
    private Duration defaultTimeout = Duration.ofSeconds(10);
    private TraceLevel traceLevel = TraceLevel.DETAILED;
    private boolean strictConstraints = true;
    private boolean failFastOnRuleError = false;
    private boolean autoDiscovery = true;
    private AiProperties ai = new AiProperties();
    private KnowledgeProperties knowledge = new KnowledgeProperties();
    private SecurityProperties security = new SecurityProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    public void setTraceLevel(TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
    }

    public boolean isStrictConstraints() {
        return strictConstraints;
    }

    public void setStrictConstraints(boolean strictConstraints) {
        this.strictConstraints = strictConstraints;
    }

    public boolean isFailFastOnRuleError() {
        return failFastOnRuleError;
    }

    public void setFailFastOnRuleError(boolean failFastOnRuleError) {
        this.failFastOnRuleError = failFastOnRuleError;
    }

    public boolean isAutoDiscovery() {
        return autoDiscovery;
    }

    public void setAutoDiscovery(boolean autoDiscovery) {
        this.autoDiscovery = autoDiscovery;
    }

    public AiProperties getAi() {
        return ai;
    }

    public void setAi(AiProperties ai) {
        this.ai = ai;
    }

    public KnowledgeProperties getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(KnowledgeProperties knowledge) {
        this.knowledge = knowledge;
    }

    public SecurityProperties getSecurity() {
        return security;
    }

    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }

    public static class AiProperties {
        private boolean enabled = true;
        private String provider = "mock"; // "mock", "spring-ai", "disabled"
        private String model = "llama3.2";
        private Duration timeout = Duration.ofSeconds(3);
        private boolean fallbackToMock = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public boolean isFallbackToMock() { return fallbackToMock; }
        public void setFallbackToMock(boolean fallbackToMock) { this.fallbackToMock = fallbackToMock; }
    }

    public static class KnowledgeProperties {
        private boolean enabled = true;
        private String provider = "in-memory"; // "in-memory", "disabled"
        private int topK = 3;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = Math.max(1, topK); }
    }

    public static class SecurityProperties {
        private boolean enabled = true;
        private AuthorizationSecurityProperties authorization = new AuthorizationSecurityProperties();
        private List<ApproverSecurityProperties> approvers = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public AuthorizationSecurityProperties getAuthorization() { return authorization; }
        public void setAuthorization(AuthorizationSecurityProperties authorization) { this.authorization = authorization; }

        public List<ApproverSecurityProperties> getApprovers() { return approvers; }
        public void setApprovers(List<ApproverSecurityProperties> approvers) { this.approvers = approvers; }

        public void validate() {
            if (!enabled) return;
            if (authorization != null) {
                authorization.validate();
            }
            if (approvers != null) {
                Set<String> seenIds = new HashSet<>();
                for (ApproverSecurityProperties approver : approvers) {
                    if (approver.getId() == null || approver.getId().isBlank()) {
                        throw new IllegalStateException("Invalid LogistiX security configuration: approver id must not be null or blank");
                    }
                    if (!seenIds.add(approver.getId())) {
                        throw new IllegalStateException("Invalid LogistiX security configuration: duplicate approver id ['" +
                                approver.getId() + "']. Approver IDs must be unique.");
                    }
                    if (approver.getAllowedActionTypes() != null) {
                        for (String type : approver.getAllowedActionTypes()) {
                            if (type == null || type.isBlank()) {
                                throw new IllegalStateException("Invalid LogistiX security configuration: approver ['" +
                                        approver.getId() + "'] contains a null or blank allowed-action-type.");
                            }
                        }
                    }
                }
            }
        }
    }

    public static class AuthorizationSecurityProperties {
        public static final String DEFAULT_AUTHORITY_ID = "LogistiX-Governance-Authority";

        private String authorityId = DEFAULT_AUTHORITY_ID;
        private String issuerId = null;
        private List<String> authorities = new ArrayList<>(List.of(DEFAULT_AUTHORITY_ID, "LogistiX-Authority-Primary"));

        public String getAuthorityId() {
            return authorityId;
        }

        public void setAuthorityId(String authorityId) {
            this.authorityId = authorityId;
        }

        public String getIssuerId() {
            return issuerId;
        }

        public void setIssuerId(String issuerId) {
            this.issuerId = issuerId;
        }

        public List<String> getAuthorities() {
            return authorities;
        }

        public void setAuthorities(List<String> authorities) {
            this.authorities = authorities;
        }

        public String getResolvedAuthorityId() {
            if (issuerId != null && !issuerId.isBlank()) {
                if (!Objects.equals(authorityId, DEFAULT_AUTHORITY_ID) && !Objects.equals(authorityId, issuerId)) {
                    throw new IllegalStateException(String.format(
                            "Conflicting LogistiX security configuration: authority-id ['%s'] does not match legacy issuer-id ['%s']. " +
                                    "Use authority-id as the canonical authorization authority identity.",
                            authorityId, issuerId));
                }
                return issuerId;
            }
            return authorityId;
        }

        public void validate() {
            String resolved = getResolvedAuthorityId();
            if (resolved == null || resolved.isBlank()) {
                throw new IllegalStateException("Invalid LogistiX security configuration: authority-id must not be null or blank.");
            }
            if (authorities == null || authorities.isEmpty()) {
                throw new IllegalStateException("Invalid LogistiX security configuration: authorities list must not be null or empty.");
            }
            Set<String> seen = new HashSet<>();
            for (String auth : authorities) {
                if (auth == null || auth.isBlank()) {
                    throw new IllegalStateException("Invalid LogistiX security configuration: authorities contains a null or blank entry.");
                }
                if (!seen.add(auth)) {
                    throw new IllegalStateException("Invalid LogistiX security configuration: duplicate authority ['" +
                            auth + "']. Authority IDs must be unique.");
                }
            }
            if (!authorities.contains(resolved)) {
                throw new IllegalStateException(String.format(
                        "Invalid LogistiX security configuration: authority-id ['%s'] is not present in security.authorization.authorities %s. " +
                                "The authorization issuer authority must be registered in the trusted authority list.",
                        resolved, authorities));
            }
        }
    }

    public static class ApproverSecurityProperties {
        private String id;
        private List<String> allowedActionTypes = new ArrayList<>();
        private boolean enabled = true;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public List<String> getAllowedActionTypes() { return allowedActionTypes; }
        public void setAllowedActionTypes(List<String> allowedActionTypes) { this.allowedActionTypes = allowedActionTypes; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
