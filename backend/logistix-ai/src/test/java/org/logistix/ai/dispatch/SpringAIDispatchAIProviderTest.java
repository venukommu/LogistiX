package org.logistix.ai.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.domain.decision.DecisionContext;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SpringAIDispatchAIProviderTest {

    private ChatModel mockChatModel;
    private SpringAIDispatchAIProvider provider;
    private DecisionContext context;

    @BeforeEach
    void setUp() {
        mockChatModel = Mockito.mock(ChatModel.class);
        provider = new SpringAIDispatchAIProvider(mockChatModel, "llama3.2");
        context = DecisionContext.of("driver-dispatch");
    }

    @Test
    @DisplayName("Should successfully parse structured JSON response from ChatModel")
    void testSuccessfulStructuredInference() {
        String jsonResponse = """
                {
                  "candidateId": "driver-123",
                  "riskLevel": "LOW",
                  "advisoryConfidence": 0.94,
                  "reasoning": "Optimal route with minimal weather risk and sufficient HOS.",
                  "contributingFactors": ["Clear weather", "Close proximity"],
                  "warnings": [],
                  "suggestedScoreAdjustment": 0.05
                }
                """;

        ChatResponse chatResponse = createMockChatResponse(jsonResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<DispatchAIAdvice> adviceOpt = provider.infer(context, DispatchAIAdvice.class);

        assertThat(adviceOpt).isPresent();
        DispatchAIAdvice advice = adviceOpt.get();
        assertThat(advice.candidateId()).isEqualTo("driver-123");
        assertThat(advice.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(advice.advisoryConfidence()).isEqualTo(0.94);
        assertThat(advice.reasoning()).contains("Optimal route");
        assertThat(advice.suggestedScoreAdjustment()).isEqualTo(0.05);
    }

    @Test
    @DisplayName("Should strip Markdown code fences when model wraps JSON")
    void testExtractJsonWithMarkdownCodeBlocks() {
        String wrappedJson = """
                ```json
                {
                  "candidateId": "driver-456",
                  "riskLevel": "MEDIUM",
                  "advisoryConfidence": 0.88,
                  "reasoning": "Moderate rain on I-5 corridor; extra 30 min buffer recommended.",
                  "contributingFactors": ["Wet road surface"],
                  "warnings": ["Potential congestion near Grapevine"],
                  "suggestedScoreAdjustment": -0.02
                }
                ```
                """;

        ChatResponse chatResponse = createMockChatResponse(wrappedJson);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<DispatchAIAdvice> adviceOpt = provider.infer(context, DispatchAIAdvice.class);

        assertThat(adviceOpt).isPresent();
        assertThat(adviceOpt.get().candidateId()).isEqualTo("driver-456");
        assertThat(adviceOpt.get().riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(adviceOpt.get().warnings()).contains("Potential congestion near Grapevine");
    }

    @Test
    @DisplayName("Should return empty Optional when model response is malformed JSON")
    void testMalformedJsonHandling() {
        String invalidJson = "This is not valid JSON from the model...";

        ChatResponse chatResponse = createMockChatResponse(invalidJson);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<DispatchAIAdvice> adviceOpt = provider.infer(context, DispatchAIAdvice.class);
        assertThat(adviceOpt).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional when model returns empty or null response")
    void testEmptyResponseHandling() {
        ChatResponse chatResponse = createMockChatResponse("");
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<DispatchAIAdvice> adviceOpt = provider.infer(context, DispatchAIAdvice.class);
        assertThat(adviceOpt).isEmpty();
    }

    @Test
    @DisplayName("GenerateReasoning should throw exception when model is unavailable to trigger pipeline fallback")
    void testGenerateReasoningFailureThrowsException() {
        when(mockChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Connection timed out to Ollama"));

        assertThatThrownBy(() -> provider.generateReasoning(context, "some-candidate"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Spring AI reasoning invocation failed");
    }

    private ChatResponse createMockChatResponse(String content) {
        AssistantMessage assistantMessage = new AssistantMessage(content);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }
}
