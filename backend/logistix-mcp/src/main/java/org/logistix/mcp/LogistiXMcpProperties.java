package org.logistix.mcp;

/**
 * Configuration properties for LogistiX MCP adapter.
 */
public record LogistiXMcpProperties(
        boolean enabled,
        String serverUrl,
        int timeoutSeconds,
        boolean localMockEnabled
) {
    public LogistiXMcpProperties {
        serverUrl = serverUrl != null ? serverUrl : "local://mock-mcp-server";
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 5;
        }
    }

    public static LogistiXMcpProperties standardDefaults() {
        return new LogistiXMcpProperties(true, "local://mock-mcp-server", 5, true);
    }
}
