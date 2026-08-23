package org.logistix.starter.autoconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.SpringAIDispatchAIProvider;
import org.logistix.domain.action.ActionApprovalIssuer;
import org.logistix.domain.action.ActionAuthorizationIssuer;
import org.logistix.domain.action.ActionType;
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
    @DisplayName("Should auto-configure MockDispatchAIProvider by default")
    void testDefaultMockAiProvider() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AIProvider.class);
            AIProvider provider = context.getBean(AIProvider.class);
            assertThat(provider).isInstanceOf(MockDispatchAIProvider.class);
            assertThat(provider.getProviderName()).contains("Mock");

            assertThat(context).hasSingleBean(org.logistix.domain.ports.KnowledgeProvider.class);
            org.logistix.domain.ports.KnowledgeProvider kp = context.getBean(org.logistix.domain.ports.KnowledgeProvider.class);
            assertThat(kp).isInstanceOf(org.logistix.rag.knowledge.InMemoryKnowledgeProvider.class);
        });
    }

    @Test
    @DisplayName("Should auto-configure security registries and trusted issuers without requiring MCP")
    void testDefaultSecurityRegistriesAndIssuers() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TrustedApproverRegistry.class);
            TrustedApproverRegistry approverRegistry = context.getBean(TrustedApproverRegistry.class);
            assertThat(approverRegistry.isFrozen()).isTrue();
            // Default with no approvers is Option A: empty frozen registry
            assertThat(approverRegistry.getRegisteredApproverIds()).isEmpty();
            assertThat(approverRegistry.isAuthorizedApprover("SUPERVISOR-001")).isFalse();

            assertThatThrownBy(() -> approverRegistry.registerApprover("rogue-approver", Set.of(ActionType.ASSIGN_DRIVER)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen and immutable");

            assertThat(context).hasSingleBean(ActionAuthorizationIssuer.class);
            assertThat(context).hasSingleBean(ActionApprovalIssuer.class);

            ActionAuthorizationIssuer issuer = context.getBean(ActionAuthorizationIssuer.class);
            assertThat(issuer).isInstanceOf(DefaultActionAuthorizationIssuer.class);
            assertThat(((DefaultActionAuthorizationIssuer) issuer).getIssuerAuthorityId())
                    .isEqualTo("LogistiX-Governance-Authority");
        });
    }

    @Test
    @DisplayName("Should disable security beans when logistix.security.enabled=false")
    void testSecurityDisabled() {
        contextRunner
                .withPropertyValues("logistix.security.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TrustedApproverRegistry.class);
                    assertThat(context).doesNotHaveBean(ActionAuthorizationIssuer.class);
                    assertThat(context).doesNotHaveBean(ActionApprovalIssuer.class);
                });
    }

    @Test
    @DisplayName("Should auto-configure custom security approvers from properties and freeze")
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
