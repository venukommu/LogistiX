package org.logistix.engine.registry;

import org.logistix.engine.pipeline.DecisionPipeline;

import java.util.List;
import java.util.Optional;

/**
 * Central registry for discovering, registering, and routing pipelines by decision type.
 * Enables the engine to support new decision types dynamically without modifying core engine logic.
 */
public interface DecisionRegistry {

    void register(DecisionPipeline pipeline);

    Optional<DecisionPipeline> getPipeline(String decisionType);

    boolean hasPipeline(String decisionType);

    List<DecisionPipeline> getAllPipelines();

    void unregister(String decisionType);
}
