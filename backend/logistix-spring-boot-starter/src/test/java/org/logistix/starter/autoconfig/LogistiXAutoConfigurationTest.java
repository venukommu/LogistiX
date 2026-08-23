package org.logistix.starter.autoconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.SpringAIDispatchAIProvider;
import org.logistix.domain.action.ActionApprovalIssuer;
import org.logistix.domain.action.ActionAuthorizationIssuer;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.TrustedApproverRegistry;
import org.logistix.domain.ports.AIProvider;
import org.logistix.mcp.AuthorizationAuthorityRegistry;
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
    @DisplayName("Should auto-configure frozen security registries and trusted issuers by default")
    void testDefaultSecurityRegistriesAndIssuers() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuthorizationAuthorityRegistry.class);
            AuthorizationAuthorityRegistry authRegistry = context.getBean(AuthorizationAuthorityRegistry.class);
            assertThat(authRegistry.isFrozen()).isTrue();
            assertThat(authRegistry.isRegisteredAuthority("LogistiX-Governance-Authority")).isTrue();
            assertThatThrownBy(() -> authRegistry.registerAuthority("rogue-authority"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen and immutable");

            assertThat(context).hasSingleBean(TrustedApproverRegistry.class);
            TrustedApproverRegistry approverRegistry = context.getBean(TrustedApproverRegistry.class);
            assertThat(approverRegistry.isFrozen()).isTrue();
            assertThat(approverRegistry.isAuthorizedApprover("SUPERVISOR-001")).isTrue();
            assertThatThrownBy(() -> approverRegistry.registerApprover("rogue-approver", Set.of(ActionType.ASSIGN_DRIVER)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen and immutable");

            assertThat(context).hasSingleBean(ActionAuthorizationIssuer.class);
            assertThat(context).hasSingleBean(ActionApprovalIssuer.class);
        });
    }

    @Test
    @DisplayName("Should auto-configure custom security authorities and approvers from properties")
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
