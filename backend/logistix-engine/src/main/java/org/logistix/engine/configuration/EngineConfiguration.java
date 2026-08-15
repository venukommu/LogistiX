package org.logistix.engine.configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable runtime configuration parameters governing the LogistiX execution engine.
 */
public record EngineConfiguration(
        Duration defaultExecutionTimeout,
        TraceLevel traceLevel,
        boolean strictConstraintEnforcement,
        boolean failFastOnRuleError,
        int maxParallelism,
        Map<String, Object> customProperties
) {
    public EngineConfiguration {
        Objects.requireNonNull(defaultExecutionTimeout, "Default execution timeout must not be null");
        Objects.requireNonNull(traceLevel, "Trace level must not be null");
        if (maxParallelism <= 0) {
            throw new IllegalArgumentException("Max parallelism must be strictly positive");
        }
        customProperties = customProperties != null ? Map.copyOf(customProperties) : Collections.emptyMap();
    }

    public static EngineConfiguration defaults() {
        return new EngineConfiguration(
                Duration.ofSeconds(10),
                TraceLevel.DETAILED,
                true,
                false,
                Runtime.getRuntime().availableProcessors(),
                Collections.emptyMap()
        );
    }
}
