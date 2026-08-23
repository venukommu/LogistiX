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
 *
 * <p><strong>Lifecycle & Boundary Rules:</strong></p>
 * <ul>
 *   <li>Consumes the single core {@link AuthorizationAuthorityRegistry} bean configured by core LogistiX security.</li>
 *   <li>Activates only when MCP classes are present, {@code logistix.mcp.enabled} is true,
 *       and the core {@link AuthorizationAuthorityRegistry} bean exists in the context.</li>
 *   <li>Evaluates after {@code LogistiXAutoConfiguration} to ensure the core authority registry is initialized.</li>
 *   <li>If core security is disabled ({@code logistix.security.enabled=false}), this configuration backs off
 *       and creates zero MCP execution beans.</li>
 * </ul>
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
