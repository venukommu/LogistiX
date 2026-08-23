package org.logistix.starter.autoconfig;

import org.logistix.engine.configuration.TraceLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
    }

    public static class AuthorizationSecurityProperties {
        private String authorityId = "LogistiX-Governance-Authority";
        private String issuerId = "LogistiX-Governance-Authority";
        private List<String> authorities = new ArrayList<>(List.of("LogistiX-Governance-Authority", "LogistiX-Authority-Primary"));

        public String getAuthorityId() { return authorityId; }
        public void setAuthorityId(String authorityId) { this.authorityId = authorityId; }

        public String getIssuerId() { return issuerId; }
        public void setIssuerId(String issuerId) { this.issuerId = issuerId; }

        public List<String> getAuthorities() { return authorities; }
        public void setAuthorities(List<String> authorities) { this.authorities = authorities; }
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
