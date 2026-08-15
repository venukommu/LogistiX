package org.logistix.examples;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.engine.hooks.DecisionHook;
import org.logistix.engine.plugins.DecisionPlugin;
import org.logistix.engine.plugins.PluginContext;
import org.logistix.engine.steps.DecisionStep;

import java.util.Collections;
import java.util.List;

/**
 * <h3>Custom Plugin Example</h3>
 * Demonstrates creating a third-party LogistiX extension plugin.
 */
@org.logistix.dsl.annotation.DecisionPlugin(id = "telemetry-audit-plugin", name = "Audit Telemetry Plugin", version = "1.0.0")
public class CustomPluginExample implements DecisionPlugin {

    @Override
    public String getPluginId() {
        return "telemetry-audit-plugin";
    }

    @Override
    public String getName() {
        return "Audit Telemetry Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize(PluginContext context) {
        System.out.println("Telemetry Audit Plugin initialized for environment: " + context.environment());
    }

    @Override
    public List<DecisionStep> getContributedSteps() {
        return Collections.emptyList();
    }

    @Override
    public List<DecisionHook> getContributedHooks() {
        return List.of(new DecisionHook() {
            @Override
            public void beforeDecision(DecisionContext context) {
                System.out.println("[AUDIT HOOK] Starting decision: " + context.decisionType());
            }

            @Override
            public void afterDecision(DecisionContext context, DecisionResult<?> result) {
                System.out.println("[AUDIT HOOK] Finished decision in " + result.executionTime().toMillis() + "ms");
            }
        });
    }
}
