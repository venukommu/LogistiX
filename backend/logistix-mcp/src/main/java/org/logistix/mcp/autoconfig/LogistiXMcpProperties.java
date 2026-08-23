package org.logistix.mcp.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Spring Boot Configuration Properties for LogistiX MCP Adapter.
 * Contains only transport- and executor-specific properties.
 * Trust and authority configuration is strictly owned by LogistiX core security.
 */
@ConfigurationProperties(prefix = "logistix.mcp")
public class LogistiXMcpProperties {

    private boolean enabled = true;
    private Duration executionTimeout = Duration.ofSeconds(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(Duration executionTimeout) {
        this.executionTimeout = executionTimeout;
    }
}
