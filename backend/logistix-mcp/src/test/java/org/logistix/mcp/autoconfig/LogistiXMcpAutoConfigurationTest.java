package org.logistix.mcp.autoconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.domain.action.AuthorizationAuthorityRegistry;
import org.logistix.mcp.McpActionExecutor;
import org.logistix.mcp.MockMcpToolServer;
import org.logistix.mcp.ToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LogistiXMcpAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogistiXMcpAutoConfiguration.class));

    @Test
    @DisplayName("Should auto-configure MCP beans when core AuthorizationAuthorityRegistry bean is present")
    void testMcpAutoConfigurationWithCoreAuthorityRegistry() {
        contextRunner
                .withUserConfiguration(TestSecurityRegistryConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationAuthorityRegistry.class);
                    assertThat(context).hasSingleBean(ToolRegistry.class);
                    assertThat(context).hasSingleBean(MockMcpToolServer.class);
                    assertThat(context).hasSingleBean(McpActionExecutor.class);

                    McpActionExecutor executor = context.getBean(McpActionExecutor.class);
                    assertThat(executor.getAuthorityRegistry()).isSameAs(context.getBean(AuthorizationAuthorityRegistry.class));
                });
    }

    @Test
    @DisplayName("Should NOT activate MCP auto-configuration when core AuthorizationAuthorityRegistry bean is missing")
    void testMcpNotActivatedWithoutAuthorityRegistry() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(McpActionExecutor.class);
            assertThat(context).doesNotHaveBean(ToolRegistry.class);
            assertThat(context).doesNotHaveBean(MockMcpToolServer.class);
            assertThat(context).doesNotHaveBean(AuthorizationAuthorityRegistry.class);
        });
    }

    @Test
    @DisplayName("Should not configure MCP beans when logistix.mcp.enabled is false even if authority registry is present")
    void testMcpDisabled() {
        contextRunner
                .withUserConfiguration(TestSecurityRegistryConfig.class)
                .withPropertyValues("logistix.mcp.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationAuthorityRegistry.class);
                    assertThat(context).doesNotHaveBean(McpActionExecutor.class);
                    assertThat(context).doesNotHaveBean(ToolRegistry.class);
                });
    }

    @Configuration
    static class TestSecurityRegistryConfig {
        @Bean
        public AuthorizationAuthorityRegistry logistixAuthorizationAuthorityRegistry() {
            return AuthorizationAuthorityRegistry.withStandardAuthorities();
        }
    }
}
