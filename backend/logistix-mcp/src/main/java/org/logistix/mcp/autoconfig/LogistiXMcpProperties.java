package org.logistix.mcp.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot Configuration Properties for LogistiX MCP Adapter.
 */
@ConfigurationProperties(prefix = "logistix.mcp")
public class LogistiXMcpProperties {

    private boolean enabled = true;
    private List<String> authorities = new ArrayList<>(List.of("LogistiX-Governance-Authority", "LogistiX-Authority-Primary"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}
