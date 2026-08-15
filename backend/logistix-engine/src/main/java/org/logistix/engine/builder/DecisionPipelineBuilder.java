package org.logistix.engine.builder;

import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.steps.DecisionStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fluent builder for constructing immutable DecisionPipeline instances.
 */
public class DecisionPipelineBuilder {

    private UUID pipelineId = UUID.randomUUID();
    private String decisionType;
    private String name;
    private String version = "1.0.0";
    private final List<DecisionStep> steps = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public DecisionPipelineBuilder pipelineId(UUID pipelineId) {
        this.pipelineId = Objects.requireNonNull(pipelineId, "Pipeline ID cannot be null");
        return this;
    }

    public DecisionPipelineBuilder decisionType(String decisionType) {
        this.decisionType = Objects.requireNonNull(decisionType, "Decision type cannot be null");
        if (this.name == null) {
            this.name = decisionType + "-Pipeline";
        }
        return this;
    }

    public DecisionPipelineBuilder name(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        return this;
    }

    public DecisionPipelineBuilder version(String version) {
        this.version = Objects.requireNonNull(version, "Version cannot be null");
        return this;
    }

    public DecisionPipelineBuilder step(DecisionStep step) {
        this.steps.add(Objects.requireNonNull(step, "DecisionStep cannot be null"));
        return this;
    }

    public DecisionPipelineBuilder steps(List<DecisionStep> steps) {
        if (steps != null) {
            steps.forEach(this::step);
        }
        return this;
    }

    public DecisionPipelineBuilder withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public DecisionPipeline build() {
        if (this.decisionType == null || this.decisionType.isBlank()) {
            throw new IllegalStateException("Decision type must be specified to build a DecisionPipeline");
        }
        if (this.name == null || this.name.isBlank()) {
            this.name = this.decisionType + "-Pipeline";
        }

        // Sort steps by their defined metadata order
        List<DecisionStep> sortedSteps = new ArrayList<>(this.steps);
        sortedSteps.sort(Comparator.comparingInt(s -> s.getMetadata() != null ? s.getMetadata().order() : 0));

        return new DecisionPipeline(
                this.pipelineId,
                this.decisionType,
                this.name,
                this.version,
                sortedSteps,
                this.metadata
        );
    }
}
