package org.logistix.ai.dispatch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.ports.AIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Production-grade Hexagonal Adapter connecting LogistiX AIProvider SPI
 * to Spring AI ChatModel implementations (e.g. Ollama, OpenAI, Azure, Anthropic).
 */
public class SpringAIDispatchAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(SpringAIDispatchAIProvider.class);

    private final ChatModel chatModel;
    private final String modelName;
    private final Duration timeout;
    private final ObjectMapper objectMapper;

    public SpringAIDispatchAIProvider(ChatModel chatModel) {
        this(chatModel, "SpringAI-Model", Duration.ofSeconds(3), createDefaultObjectMapper());
    }

    public SpringAIDispatchAIProvider(ChatModel chatModel, String modelName) {
        this(chatModel, modelName, Duration.ofSeconds(3), createDefaultObjectMapper());
    }

    public SpringAIDispatchAIProvider(ChatModel chatModel, String modelName, Duration timeout, ObjectMapper objectMapper) {
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel must not be null");
        this.modelName = modelName != null ? modelName : "SpringAI-Model";
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(3);
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    private static ObjectMapper createDefaultObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getProviderName() {
        return "SpringAI-" + modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
        try {
            DispatchAIRequest request = context.getFactValue("aiRequest", DispatchAIRequest.class).orElse(null);
            String userPrompt = request != null
                    ? DispatchPromptBuilder.buildUserPrompt(request)
                    : DispatchPromptBuilder.buildUserPrompt(context, null);

            String rawJson = callModelWithTimeout(userPrompt);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("Spring AI provider received empty completion response from model");
                return Optional.empty();
            }

            String cleanJson = extractJsonPayload(rawJson);

            if (responseType.isAssignableFrom(BatchedDispatchAIAdvice.class)) {
                BatchedDispatchAIAdvice batched = objectMapper.readValue(cleanJson, BatchedDispatchAIAdvice.class);
                return Optional.ofNullable(responseType.cast(batched));
            } else if (responseType.isAssignableFrom(DispatchAIAdvice.class)) {
                // If single advice is requested, parse directly or extract first candidate from batched advice
                try {
                    DispatchAIAdvice single = objectMapper.readValue(cleanJson, DispatchAIAdvice.class);
                    return Optional.ofNullable(responseType.cast(single));
                } catch (Exception e) {
                    BatchedDispatchAIAdvice batched = objectMapper.readValue(cleanJson, BatchedDispatchAIAdvice.class);
                    if (!batched.candidateAdvices().isEmpty()) {
                        return Optional.ofNullable(responseType.cast(batched.candidateAdvices().get(0)));
                    }
                }
            } else {
                T parsed = objectMapper.readValue(cleanJson, responseType);
                return Optional.ofNullable(parsed);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.warn("Spring AI inference failed or timed out: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String generateReasoning(DecisionContext context, Object candidate) {
        try {
            DispatchAIRequest request = context.getFactValue("aiRequest", DispatchAIRequest.class).orElse(null);
            String userPrompt = request != null
                    ? DispatchPromptBuilder.buildUserPrompt(request)
                    : DispatchPromptBuilder.buildUserPrompt(context, candidate);

            String rawJson = callModelWithTimeout(userPrompt);

            if (rawJson == null || rawJson.isBlank()) {
                throw new IllegalStateException("Empty model response received");
            }

            String cleanJson = extractJsonPayload(rawJson);

            try {
                BatchedDispatchAIAdvice batched = objectMapper.readValue(cleanJson, BatchedDispatchAIAdvice.class);
                if (!batched.candidateAdvices().isEmpty()) {
                    DispatchAIAdvice first = batched.candidateAdvices().get(0);
                    return String.format("Spring AI [%s - Risk: %s, Conf: %.2f]: %s",
                            modelName, first.riskLevel(), first.advisoryConfidence(), first.reasoning());
                }
            } catch (Exception ignored) {
                DispatchAIAdvice advice = objectMapper.readValue(cleanJson, DispatchAIAdvice.class);
                return String.format("Spring AI [%s - Risk: %s, Conf: %.2f]: %s",
                        modelName, advice.riskLevel(), advice.advisoryConfidence(), advice.reasoning());
            }

            return "Spring AI Analysis: Candidates evaluated successfully.";

        } catch (Exception e) {
            log.warn("Spring AI reasoning generation failed: {}", e.getMessage());
            throw new RuntimeException("Spring AI reasoning invocation failed: " + e.getMessage(), e);
        }
    }

    private String callModelWithTimeout(String userPromptContent) throws Exception {
        SystemMessage systemMessage = new SystemMessage(DispatchPromptBuilder.getSystemPrompt());
        UserMessage userMessage = new UserMessage(userPromptContent);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            ChatResponse response = chatModel.call(prompt);
            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
            return null;
        });

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            throw new TimeoutException("Spring AI model invocation timed out after " + timeout.toMillis() + " ms");
        }
    }

    /**
     * Extracts pure JSON string if model wraps response in Markdown code fences.
     */
    static String extractJsonPayload(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
