package org.logistix.ai.dispatch;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Batched AI response returned from a single LLM invocation evaluating multiple candidates.
 */
public record BatchedDispatchAIAdvice(
        List<DispatchAIAdvice> candidateAdvices,
        String overallContextAssessment,
        String promptVersion,
        Instant timestamp
) {
    public BatchedDispatchAIAdvice {
        candidateAdvices = candidateAdvices != null ? List.copyOf(candidateAdvices) : Collections.emptyList();
        overallContextAssessment = overallContextAssessment != null ? overallContextAssessment : "";
        promptVersion = promptVersion != null ? promptVersion : DispatchPromptBuilder.PROMPT_VERSION;
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public Optional<DispatchAIAdvice> getAdviceForCandidate(String candidateId) {
        if (candidateId == null) return Optional.empty();
        return candidateAdvices.stream()
                .filter(a -> candidateId.equalsIgnoreCase(a.candidateId()))
                .findFirst();
    }

    public static BatchedDispatchAIAdvice of(List<DispatchAIAdvice> advices, String overallAssessment) {
        return new BatchedDispatchAIAdvice(advices, overallAssessment, DispatchPromptBuilder.PROMPT_VERSION, Instant.now());
    }

    public static BatchedDispatchAIAdvice empty() {
        return new BatchedDispatchAIAdvice(Collections.emptyList(), "No AI advice generated.", DispatchPromptBuilder.PROMPT_VERSION, Instant.now());
    }
}
