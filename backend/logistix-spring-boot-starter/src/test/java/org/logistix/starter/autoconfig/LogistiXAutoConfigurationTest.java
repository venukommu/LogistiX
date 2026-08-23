package org.logistix.starter.autoconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.SpringAIDispatchAIProvider;
import org.logistix.domain.action.ActionApprovalIssuer;
import org.logistix.domain.action.ActionAuthorizationIssuer;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.AuthorizationAuthorityRegistry;
import org.logistix.domain.action.DefaultActionAuthorizationIssuer;
import org.logistix.domain.action.TrustedApproverRegistry;
import org.logistix.domain.ports.AIProvider;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogistiXAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogistiXAutoConfiguration.class));

    @Test
    @DisplayName("CASE A: Starter only — Auto-configure core and security beans without requiring MCP")
    void testStarterOnlyAutoConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AIProvider.class);
            assertThat(context).hasSingleBean(org.logistix.domain.ports.KnowledgeProvider.class);

            assertThat(context).hasSingleBean(AuthorizationAuthorityRegistry.class);
            AuthorizationAuthorityRegistry authRegistry = context.getBean(AuthorizationAuthorityRegistry.class);
            assertThat(authRegistry.isFrozen()).isTrue();
            assertThat(authRegistry.isRegisteredAuthority("LogistiX-Governance-Authority")).isTrue();

            assertThat(context).hasSingleBean(TrustedApproverRegistry.class);
            TrustedApproverRegistry approverRegistry = context.getBean(TrustedApproverRegistry.class);
            assertThat(approverRegistry.isFrozen()).isTrue();
            assertThat(approverRegistry.getRegisteredApproverIds()).isEmpty();

            assertThat(context).hasSingleBean(ActionAuthorizationIssuer.class);
            assertThat(context).hasSingleBean(ActionApprovalIssuer.class);

            // Assert single registry invariant
            assertThat(context.getBeansOfType(AuthorizationAuthorityRegistry.class)).hasSize(1);
        });
    }

    @Test
    @DisplayName("CASE D: Starter with logistix.security.enabled=false disables all security beans")
    void testSecurityDisabled() {
        contextRunner
                .withPropertyValues("logistix.security.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthorizationAuthorityRegistry.class);
                    assertThat(context).doesNotHaveBean(TrustedApproverRegistry.class);
                    assertThat(context).doesNotHaveBean(ActionAuthorizationIssuer.class);
                    assertThat(context).doesNotHaveBean(ActionApprovalIssuer.class);
                });
    }

    @Test
    @DisplayName("CASE B: Combined Starter + MCP — Exactly one AuthorizationAuthorityRegistry bean exists")
    void testCombinedStarterAndMcpSingleRegistry() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        LogistiXAutoConfiguration.class,
                        org.logistix.mcp.autoconfig.LogistiXMcpAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context.getBeansOfType(AuthorizationAuthorityRegistry.class)).hasSize(1);
                    assertThat(context).hasSingleBean(org.logistix.mcp.McpActionExecutor.class);
                    org.logistix.mcp.McpActionExecutor executor = context.getBean(org.logistix.mcp.McpActionExecutor.class);
                    assertThat(executor.getAuthorityRegistry()).isSameAs(context.getBean(AuthorizationAuthorityRegistry.class));
                });
    }

    @Test
    @DisplayName("CASE C: Combined Starter + MCP + Custom Authorities — Single registry consumed by MCP")
    void testCombinedCustomAuthorities() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        LogistiXAutoConfiguration.class,
                        org.logistix.mcp.autoconfig.LogistiXMcpAutoConfiguration.class
                ))
                .withPropertyValues(
                        "logistix.security.authorization.authority-id=Unified-Authority",
                        "logistix.security.authorization.authorities[0]=Unified-Authority"
                )
                .run(context -> {
                    assertThat(context.getBeansOfType(AuthorizationAuthorityRegistry.class)).hasSize(1);
                    AuthorizationAuthorityRegistry registry = context.getBean(AuthorizationAuthorityRegistry.class);
                    assertThat(registry.isRegisteredAuthority("Unified-Authority")).isTrue();
                    assertThat(registry.isRegisteredAuthority("LogistiX-Governance-Authority")).isFalse();

                    org.logistix.mcp.McpActionExecutor executor = context.getBean(org.logistix.mcp.McpActionExecutor.class);
                    assertThat(executor.getAuthorityRegistry().isRegisteredAuthority("Unified-Authority")).isTrue();
                });
    }

    @Test
    @DisplayName("CASE D2: Combined Starter + MCP with security.enabled=false — MCP beans are not activated")
    void testCombinedSecurityDisabledMcpNotActivated() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        LogistiXAutoConfiguration.class,
                        org.logistix.mcp.autoconfig.LogistiXMcpAutoConfiguration.class
                ))
                .withPropertyValues("logistix.security.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthorizationAuthorityRegistry.class);
                    assertThat(context).doesNotHaveBean(org.logistix.mcp.McpActionExecutor.class);
                });
    }

    @Test
    @DisplayName("Should auto-configure custom security authorities and approvers and freeze")
    void testCustomSecurityConfiguration() {
        contextRunner
                .withPropertyValues(
                        "logistix.security.authorization.authority-id=Custom-Auth-Authority",
                        "logistix.security.authorization.authorities[0]=Custom-Auth-Authority",
                        "logistix.security.authorization.authorities[1]=Backup-Auth-Authority",
                        "logistix.security.approvers[0].id=CUSTOM-SUPERVISOR",
                        "logistix.security.approvers[0].allowed-action-types[0]=CHANGE_DELIVERY_APPOINTMENT"
                )
                .run(context -> {
                    AuthorizationAuthorityRegistry authRegistry = context.getBean(AuthorizationAuthorityRegistry.class);
                    assertThat(authRegistry.isFrozen()).isTrue();
                    assertThat(authRegistry.isRegisteredAuthority("Custom-Auth-Authority")).isTrue();
                    assertThat(authRegistry.isRegisteredAuthority("Backup-Auth-Authority")).isTrue();
                    assertThat(authRegistry.isRegisteredAuthority("LogistiX-Governance-Authority")).isFalse();

                    TrustedApproverRegistry approverRegistry = context.getBean(TrustedApproverRegistry.class);
                    assertThat(approverRegistry.isFrozen()).isTrue();
                    assertThat(approverRegistry.isAuthorizedApprover("CUSTOM-SUPERVISOR")).isTrue();
                    assertThat(approverRegistry.isAuthorizedApprover("CUSTOM-SUPERVISOR", ActionType.CHANGE_DELIVERY_APPOINTMENT)).isTrue();
                    assertThat(approverRegistry.isAuthorizedApprover("CUSTOM-SUPERVISOR", ActionType.ASSIGN_DRIVER)).isFalse();

                    ActionAuthorizationIssuer issuer = context.getBean(ActionAuthorizationIssuer.class);
                    assertThat(((DefaultActionAuthorizationIssuer) issuer).getIssuerAuthorityId())
                            .isEqualTo("Custom-Auth-Authority");
                });
    }

    @Test
    @DisplayName("Should fail fast on startup when authority-id is missing from authorities list")
    void testMissingAuthorityIdFromAuthoritiesFailsFast() {
        contextRunner
                .withPropertyValues(
                        "logistix.security.authorization.authority-id=Unregistered-Authority",
                        "logistix.security.authorization.authorities[0]=Registered-Authority-A"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("authority-id ['Unregistered-Authority'] is not present in security.authorization.authorities");
                });
    }

    @Test
    @DisplayName("Should fail fast on startup when conflicting authority-id and issuer-id are configured")
    void testConflictingAuthorityAndIssuerIdFailsFast() {
        contextRunner
                .withPropertyValues(
                        "logistix.security.authorization.authority-id=Authority-A",
                        "logistix.security.authorization.issuer-id=Authority-B"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Conflicting LogistiX security configuration");
                });
    }

    @Test
    @DisplayName("Should fail fast on startup when duplicate approver IDs are configured")
    void testDuplicateApproverIdFailsFast() {
        contextRunner
                .withPropertyValues(
                        "logistix.security.approvers[0].id=SUPERVISOR-A",
                        "logistix.security.approvers[1].id=SUPERVISOR-A"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("duplicate approver id ['SUPERVISOR-A']");
                });
    }

    @Test
    @DisplayName("Should auto-configure SpringAIDispatchAIProvider when ChatModel bean is present and provider=spring-ai")
    void testSpringAiProviderWithChatModel() {
        contextRunner
                .withUserConfiguration(ChatModelTestConfig.class)
                .withPropertyValues("logistix.ai.provider=spring-ai", "logistix.ai.model=llama3.2")
                .run(context -> {
                    assertThat(context).hasSingleBean(AIProvider.class);
                    AIProvider provider = context.getBean(AIProvider.class);
                    assertThat(provider).isInstanceOf(SpringAIDispatchAIProvider.class);
                    assertThat(provider.getProviderName()).contains("SpringAI");
                });
    }

    @Test
    @DisplayName("Should fail fast in production if provider=spring-ai and no ChatModel bean is available and fallback is disabled")
    void testSpringAiProviderMissingChatModelFailsFast() {
        contextRunner
                .withPropertyValues("logistix.ai.provider=spring-ai", "logistix.ai.fallback-to-mock=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("LogistiX AI is configured for 'spring-ai' but no ChatModel bean was found");
                });
    }

    @Test
    @DisplayName("Should fall back to Mock provider when provider=spring-ai and fallback-to-mock=true")
    void testSpringAiProviderMissingChatModelFallsBackWhenEnabled() {
        contextRunner
                .withPropertyValues("logistix.ai.provider=spring-ai", "logistix.ai.fallback-to-mock=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AIProvider.class);
                    AIProvider provider = context.getBean(AIProvider.class);
                    assertThat(provider).isInstanceOf(MockDispatchAIProvider.class);
                    assertThat(provider.getProviderName()).isEqualTo("Mock-Fallback-AI");
                });
    }

    @Configuration
    static class ChatModelTestConfig {
        @Bean
        public ChatModel chatModel() {
            return Mockito.mock(ChatModel.class);
        }
    }
}
