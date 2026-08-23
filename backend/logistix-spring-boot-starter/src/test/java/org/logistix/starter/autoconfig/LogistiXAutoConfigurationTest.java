package org.logistix.starter.autoconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.ai.dispatch.SpringAIDispatchAIProvider;
import org.logistix.domain.ports.AIProvider;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

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
