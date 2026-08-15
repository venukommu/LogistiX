package org.logistix.engine.plugins;

import org.logistix.engine.hooks.DecisionHook;
import org.logistix.engine.steps.DecisionStep;

import java.util.Collections;
import java.util.List;

/**
 * SPI for extending LogistiX decision pipelines with dynamic custom steps, rules, constraints, and hooks.
 */
public interface DecisionPlugin {

    String getPluginId();

    String getName();

    String getVersion();

    void initialize(PluginContext context);

    default List<DecisionStep> getContributedSteps() {
        return Collections.emptyList();
    }

    default List<DecisionHook> getContributedHooks() {
        return Collections.emptyList();
    }

    default void shutdown() {}
}
