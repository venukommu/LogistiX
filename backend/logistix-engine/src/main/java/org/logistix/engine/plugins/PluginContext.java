package org.logistix.engine.plugins;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Context provided to a DecisionPlugin during initialization.
 */
public record PluginContext(
        String environment,
        Map<String, Object> pluginProperties
) {
    public PluginContext {
        environment = environment != null ? environment : "default";
        pluginProperties = pluginProperties != null ? Map.copyOf(pluginProperties) : Collections.emptyMap();
    }

    public static PluginContext of(Map<String, Object> properties) {
        return new PluginContext("default", properties);
    }
}
