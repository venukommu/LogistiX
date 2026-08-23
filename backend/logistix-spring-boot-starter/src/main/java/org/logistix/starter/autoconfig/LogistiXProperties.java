package org.logistix.starter.autoconfig;

import org.logistix.engine.configuration.TraceLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

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
}
