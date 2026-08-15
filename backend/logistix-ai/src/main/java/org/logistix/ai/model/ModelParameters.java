package org.logistix.ai.model;

import java.util.Collections;
import java.util.List;

/**
 * Hyperparameters and runtime constraints passed to an AI model invocation.
 */
public record ModelParameters(
        Double temperature,
        Double topP,
        Integer maxTokens,
        List<String> stopSequences,
        String responseFormat
) {
    public ModelParameters {
        stopSequences = stopSequences != null ? List.copyOf(stopSequences) : Collections.emptyList();
    }

    public static ModelParameters defaults() {
        return new ModelParameters(0.2, 0.95, 2048, Collections.emptyList(), "text");
    }

    public static ModelParameters jsonStructured() {
        return new ModelParameters(0.0, 1.0, 4096, Collections.emptyList(), "json_object");
    }
}
