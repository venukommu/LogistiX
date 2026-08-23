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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production-grade Hexagonal Adapter connecting the LogistiX AIProvider SPI
 * to Spring AI ChatModel implementations (e.g. OpenAI, Azure OpenAI, Ollama, Anthropic).
 */
public class SpringAIDispatchAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(SpringAIDispatchAIProvider.class);

    private final ChatModel chatModel;
    private final String modelName;
    private final ObjectMapper objectMapper;

    public SpringAIDispatchAIProvider(ChatModel chatModel) {
        this(chatModel, "SpringAI-Model");
    }

    public SpringAIDispatchAIProvider(ChatModel chatModel, String modelName) {
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel must not be null");
        this.modelName = modelName != null ? modelName : "SpringAI-Model";
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getProviderName() {
        return "SpringAI-" + modelName;
    }

    @Override
    public <T> Optional<T> infer(DecisionContext context, Class<T> responseType) {
        try {
            String userPrompt = DispatchPromptBuilder.buildUserPrompt(context, null);
            String rawJson = callModel(userPrompt);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("Spring AI provider received empty completion response from model");
                return Optional.empty();
            }

            String cleanJson = extractJsonPayload(rawJson);
            T parsed = objectMapper.readValue(cleanJson, responseType);
            return Optional.ofNullable(parsed);

        } catch (Exception e) {
            log.warn("Spring AI inference failed or encountered error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String generateReasoning(DecisionContext context, Object candidate) {
        try {
            String userPrompt = DispatchPromptBuilder.buildUserPrompt(context, candidate);
            String rawJson = callModel(userPrompt);

            if (rawJson == null || rawJson.isBlank()) {
                throw new IllegalStateException("Empty model response received");
            }

            String cleanJson = extractJsonPayload(rawJson);
            DispatchAIAdvice advice = objectMapper.readValue(cleanJson, DispatchAIAdvice.class);

            if (advice.reasoning() != null && !advice.reasoning().isBlank()) {
                return String.format("Spring AI [%s - Risk: %s, Conf: %.2f]: %s",
                        modelName, advice.riskLevel(), advice.advisoryConfidence(), advice.reasoning());
            }

            return "Spring AI Analysis: Candidate evaluated with risk level " + advice.riskLevel();

        } catch (Exception e) {
            log.warn("Spring AI reasoning generation failed: {}", e.getMessage());
            throw new RuntimeException("Spring AI reasoning invocation failed: " + e.getMessage(), e);
        }
    }

    public Optional<DispatchAIAdvice> evaluateCandidate(DecisionContext context, Object candidate) {
        try {
            String userPrompt = DispatchPromptBuilder.buildUserPrompt(context, candidate);
            String rawJson = callModel(userPrompt);

            if (rawJson == null || rawJson.isBlank()) {
                return Optional.empty();
            }

            String cleanJson = extractJsonPayload(rawJson);
            DispatchAIAdvice advice = objectMapper.readValue(cleanJson, DispatchAIAdvice.class);
            return Optional.of(advice);

        } catch (Exception e) {
            log.warn("Spring AI candidate evaluation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String callModel(String userPromptContent) {
        SystemMessage systemMessage = new SystemMessage(DispatchPromptBuilder.getSystemPrompt());
        UserMessage userMessage = new UserMessage(userPromptContent);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        ChatResponse response = chatModel.call(prompt);
        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
            return response.getResult().getOutput().getText();
        }
        return null;
    }

    /**
     * Extracts pure JSON string if model wraps response in Markdown code fences (e.g. ```json ... ```).
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
