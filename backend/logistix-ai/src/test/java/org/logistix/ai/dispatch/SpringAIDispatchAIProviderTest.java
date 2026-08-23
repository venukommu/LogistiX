package org.logistix.ai.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAIDispatchAIProviderTest {

    private ChatModel mockChatModel;
    private SpringAIDispatchAIProvider provider;
    private DecisionContext context;

    @BeforeEach
    void setUp() {
        mockChatModel = Mockito.mock(ChatModel.class);
        provider = new SpringAIDispatchAIProvider(mockChatModel, "llama3.2", Duration.ofSeconds(2), null);

        DispatchAIRequest request = new DispatchAIRequest(
                "shipment-001",
                "San Francisco",
                "Los Angeles",
                12500.0,
                "2026-08-23T20:00:00Z",
                "CLEAR",
                "HYBRID",
                List.of(
                        new CandidatePromptContext(
                                "driver-123",
                                "Alex Rivera",
                                15.0,
                                20,
                                360,
                                "2026-08-23T18:00:00Z",
                                4.95,
                                0.98,
                                "PLATINUM",
                                0.88,
                                List.of("Preferred Driver Bonus")
                        )
                )
        );

        context = DecisionContext.of("driver-dispatch")
                .withFact(Fact.of("aiRequest", request));
    }

    @Test
    @DisplayName("Should successfully parse structured batched JSON response from ChatModel")
    void testSuccessfulBatchedStructuredInference() {
        String jsonResponse = """
                {
                  "overallContextAssessment": "Optimal transit corridor with minimal environmental hazards.",
                  "candidateAdvices": [
                    {
                      "candidateId": "driver-123",
                      "riskLevel": "LOW",
                      "advisoryConfidence": 0.94,
                      "reasoning": "Optimal route with minimal weather risk and sufficient HOS.",
                      "contributingFactors": ["Clear weather", "Close proximity"],
                      "warnings": []
                    }
                  ]
                }
                """;

        ChatResponse chatResponse = createMockChatResponse(jsonResponse);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<BatchedDispatchAIAdvice> adviceOpt = provider.infer(context, BatchedDispatchAIAdvice.class);

        assertThat(adviceOpt).isPresent();
        BatchedDispatchAIAdvice batched = adviceOpt.get();
        assertThat(batched.overallContextAssessment()).contains("Optimal transit corridor");
        assertThat(batched.candidateAdvices()).hasSize(1);

        DispatchAIAdvice advice = batched.candidateAdvices().get(0);
        assertThat(advice.candidateId()).isEqualTo("driver-123");
        assertThat(advice.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(advice.advisoryConfidence()).isEqualTo(0.94);
        assertThat(advice.reasoning()).contains("Optimal route");

        verify(mockChatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should strip Markdown code fences when model wraps JSON")
    void testExtractJsonWithMarkdownCodeBlocks() {
        String wrappedJson = """
                ```json
                {
                  "overallContextAssessment": "Moderate rain on I-5.",
                  "candidateAdvices": [
                    {
                      "candidateId": "driver-123",
                      "riskLevel": "MEDIUM",
                      "advisoryConfidence": 0.88,
                      "reasoning": "Moderate rain on I-5 corridor; extra 30 min buffer recommended.",
                      "contributingFactors": ["Wet road surface"],
                      "warnings": ["Potential congestion near Grapevine"]
                    }
                  ]
                }
                ```
                """;

        ChatResponse chatResponse = createMockChatResponse(wrappedJson);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<BatchedDispatchAIAdvice> adviceOpt = provider.infer(context, BatchedDispatchAIAdvice.class);

        assertThat(adviceOpt).isPresent();
        DispatchAIAdvice advice = adviceOpt.get().candidateAdvices().get(0);
        assertThat(advice.candidateId()).isEqualTo("driver-123");
        assertThat(advice.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(advice.warnings()).contains("Potential congestion near Grapevine");
    }

    @Test
    @DisplayName("Should return empty Optional when model response is malformed JSON")
    void testMalformedJsonHandling() {
        String invalidJson = "This is not valid JSON from the model...";

        ChatResponse chatResponse = createMockChatResponse(invalidJson);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<BatchedDispatchAIAdvice> adviceOpt = provider.infer(context, BatchedDispatchAIAdvice.class);
        assertThat(adviceOpt).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional when model returns empty or null response")
    void testEmptyResponseHandling() {
        ChatResponse chatResponse = createMockChatResponse("");
        when(mockChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Optional<BatchedDispatchAIAdvice> adviceOpt = provider.infer(context, BatchedDispatchAIAdvice.class);
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

    @Test
    @DisplayName("Should gracefully time out and return empty when model hangs")
    void testTimeoutHandling() {
        SpringAIDispatchAIProvider fastTimeoutProvider = new SpringAIDispatchAIProvider(
                mockChatModel, "llama3.2", Duration.ofMillis(50), null
        );

        when(mockChatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Thread.sleep(200);
            return createMockChatResponse("{}");
        });

        Optional<BatchedDispatchAIAdvice> result = fastTimeoutProvider.infer(context, BatchedDispatchAIAdvice.class);
        assertThat(result).isEmpty();
    }

    private ChatResponse createMockChatResponse(String content) {
        AssistantMessage assistantMessage = new AssistantMessage(content);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }
}
