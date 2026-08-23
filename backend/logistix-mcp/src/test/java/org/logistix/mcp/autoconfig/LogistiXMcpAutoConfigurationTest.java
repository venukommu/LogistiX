package org.logistix.mcp.autoconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.mcp.AuthorizationAuthorityRegistry;
import org.logistix.mcp.McpActionExecutor;
import org.logistix.mcp.MockMcpToolServer;
import org.logistix.mcp.ToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LogistiXMcpAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogistiXMcpAutoConfiguration.class));

    @Test
    @DisplayName("Should auto-configure MCP beans when enabled by default")
    void testDefaultMcpAutoConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuthorizationAuthorityRegistry.class);
            assertThat(context).hasSingleBean(ToolRegistry.class);
            assertThat(context).hasSingleBean(MockMcpToolServer.class);
            assertThat(context).hasSingleBean(McpActionExecutor.class);

            AuthorizationAuthorityRegistry registry = context.getBean(AuthorizationAuthorityRegistry.class);
            assertThat(registry.isFrozen()).isTrue();
            assertThat(registry.isRegisteredAuthority("LogistiX-Governance-Authority")).isTrue();
        });
    }

    @Test
    @DisplayName("Should not configure MCP beans when logistix.mcp.enabled is false")
    void testMcpDisabled() {
        contextRunner
                .withPropertyValues("logistix.mcp.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(McpActionExecutor.class);
                    assertThat(context).doesNotHaveBean(ToolRegistry.class);
                    assertThat(context).doesNotHaveBean(AuthorizationAuthorityRegistry.class);
                });
    }

    @Test
    @DisplayName("Should configure custom authority list from logistix.mcp.authorities")
    void testCustomMcpAuthorities() {
        contextRunner
                .withPropertyValues(
                        "logistix.mcp.authorities[0]=Custom-MCP-Auth-1",
                        "logistix.mcp.authorities[1]=Custom-MCP-Auth-2"
                )
                .run(context -> {
                    AuthorizationAuthorityRegistry registry = context.getBean(AuthorizationAuthorityRegistry.class);
                    assertThat(registry.isFrozen()).isTrue();
                    assertThat(registry.isRegisteredAuthority("Custom-MCP-Auth-1")).isTrue();
                    assertThat(registry.isRegisteredAuthority("Custom-MCP-Auth-2")).isTrue();
                    assertThat(registry.isRegisteredAuthority("LogistiX-Governance-Authority")).isFalse();
                });
    }
}
