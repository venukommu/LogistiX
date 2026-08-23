package org.logistix.mcp.autoconfig;

import org.logistix.domain.action.AuthorizationAuthorityRegistry;
import org.logistix.mcp.McpActionExecutor;
import org.logistix.mcp.MockMcpToolServer;
import org.logistix.mcp.ToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Spring Boot AutoConfiguration for LogistiX MCP Adapter.
 * Consumes the single core AuthorizationAuthorityRegistry bean configured by core security.
 * Activates only when MCP classes are present, logistix.mcp.enabled is true,
 * and the core AuthorizationAuthorityRegistry bean exists.
 */
@AutoConfiguration
@AutoConfigureAfter(name = "org.logistix.starter.autoconfig.LogistiXAutoConfiguration")
@ConditionalOnClass(McpActionExecutor.class)
@ConditionalOnBean(AuthorizationAuthorityRegistry.class)
@EnableConfigurationProperties(LogistiXMcpProperties.class)
@ConditionalOnProperty(prefix = "logistix.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogistiXMcpAutoConfiguration {

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
