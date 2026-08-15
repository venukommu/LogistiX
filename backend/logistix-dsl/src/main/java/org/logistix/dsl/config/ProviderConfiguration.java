package org.logistix.dsl.config;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable configuration for external AI, Knowledge, and Rule providers.
 */
public record ProviderConfiguration(
        String defaultAiProvider,
        String defaultKnowledgeProvider,
        String defaultRuleProvider,
        Map<String, Object> providerOptions
) {
    public ProviderConfiguration {
        defaultAiProvider = defaultAiProvider != null ? defaultAiProvider : "default-ai";
        defaultKnowledgeProvider = defaultKnowledgeProvider != null ? defaultKnowledgeProvider : "default-knowledge";
        defaultRuleProvider = defaultRuleProvider != null ? defaultRuleProvider : "default-rules";
        providerOptions = providerOptions != null ? Map.copyOf(providerOptions) : Collections.emptyMap();
    }

    public static ProviderConfiguration defaults() {
        return new ProviderConfiguration("default-ai", "default-knowledge", "default-rules", Collections.emptyMap());
    }
}
