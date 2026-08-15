package org.logistix.dsl.builder;

import org.logistix.engine.hooks.DecisionHook;
import org.logistix.engine.plugins.DecisionPlugin;
import org.logistix.engine.plugins.PluginContext;
import org.logistix.engine.steps.DecisionStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for declaring inline DecisionPlugins.
 */
public class PluginBuilder {

    private String pluginId;
    private String name;
    private String version = "1.0.0";
    private final List<DecisionStep> steps = new ArrayList<>();
    private final List<DecisionHook> hooks = new ArrayList<>();

    public PluginBuilder(String pluginId) {
        this.pluginId = Objects.requireNonNull(pluginId, "Plugin ID cannot be null");
        this.name = pluginId;
    }

    public static PluginBuilder of(String pluginId) {
        return new PluginBuilder(pluginId);
    }

    public PluginBuilder name(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        return this;
    }

    public PluginBuilder version(String version) {
        this.version = Objects.requireNonNull(version, "Version cannot be null");
        return this;
    }

    public PluginBuilder addStep(DecisionStep step) {
        this.steps.add(Objects.requireNonNull(step, "Step cannot be null"));
        return this;
    }

    public PluginBuilder addHook(DecisionHook hook) {
        this.hooks.add(Objects.requireNonNull(hook, "Hook cannot be null"));
        return this;
    }

    public DecisionPlugin build() {
        return new InlineDecisionPlugin(pluginId, name, version, List.copyOf(steps), List.copyOf(hooks));
    }

    private static class InlineDecisionPlugin implements DecisionPlugin {
        private final String pluginId;
        private final String name;
        private final String version;
        private final List<DecisionStep> steps;
        private final List<DecisionHook> hooks;

        InlineDecisionPlugin(String pluginId, String name, String version, List<DecisionStep> steps, List<DecisionHook> hooks) {
            this.pluginId = pluginId;
            this.name = name;
            this.version = version;
            this.steps = steps;
            this.hooks = hooks;
        }

        @Override
        public String getPluginId() {
            return pluginId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public void initialize(PluginContext context) {}

        @Override
        public List<DecisionStep> getContributedSteps() {
            return steps;
        }

        @Override
        public List<DecisionHook> getContributedHooks() {
            return hooks;
        }
    }
}
