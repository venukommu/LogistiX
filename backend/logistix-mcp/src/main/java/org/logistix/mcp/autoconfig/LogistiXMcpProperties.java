package org.logistix.mcp.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Spring Boot Configuration Properties for LogistiX MCP Adapter.
 *
 * <p><strong>Property Semantics:</strong></p>
 * <ul>
 *   <li>{@code logistix.mcp.enabled} (default {@code true}): Enables the MCP execution adapter only when
 *       the {@code logistix-mcp} module is present on the classpath and the core LogistiX
 *       {@code AuthorizationAuthorityRegistry} bean is available.</li>
 *   <li>If core security is disabled ({@code logistix.security.enabled=false}), MCP auto-configuration
 *       safely backs off and will not activate.</li>
 *   <li>MCP contains no trust or authority configuration; all authorization authority is owned exclusively
 *       by core LogistiX security.</li>
 * </ul>
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
