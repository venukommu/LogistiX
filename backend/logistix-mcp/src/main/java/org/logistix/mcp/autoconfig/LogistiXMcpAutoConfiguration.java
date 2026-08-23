package org.logistix.mcp.autoconfig;

import org.logistix.mcp.AuthorizationAuthorityRegistry;
import org.logistix.mcp.McpActionExecutor;
import org.logistix.mcp.MockMcpToolServer;
import org.logistix.mcp.ToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.List;

/**
 * Spring Boot AutoConfiguration for LogistiX MCP Adapter.
 * Activates only when MCP classes are present and logistix.mcp.enabled is true.
 */
@AutoConfiguration
@ConditionalOnClass(McpActionExecutor.class)
@EnableConfigurationProperties(LogistiXMcpProperties.class)
@ConditionalOnProperty(prefix = "logistix.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogistiXMcpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationAuthorityRegistry logistixAuthorizationAuthorityRegistry(LogistiXMcpProperties properties) {
        AuthorizationAuthorityRegistry registry = AuthorizationAuthorityRegistry.empty();
        List<String> authorities = properties.getAuthorities();
        if (authorities != null && !authorities.isEmpty()) {
            for (String auth : authorities) {
                registry.registerAuthority(auth);
            }
        } else {
            registry.registerAuthority("LogistiX-Governance-Authority");
        }
        registry.freeze();
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry logistixToolRegistry() {
        return ToolRegistry.withStandardLogisticsTools();
    }

    @Bean
    @ConditionalOnMissingBean
    public MockMcpToolServer logistixMockMcpToolServer() {
        return new MockMcpToolServer();
    }

    @Bean
    @ConditionalOnMissingBean
    public McpActionExecutor logistixMcpActionExecutor(
            ToolRegistry toolRegistry,
            MockMcpToolServer toolServer,
            AuthorizationAuthorityRegistry authorityRegistry
    ) {
        return new McpActionExecutor(toolRegistry, toolServer, authorityRegistry, Clock.systemUTC());
    }
}
