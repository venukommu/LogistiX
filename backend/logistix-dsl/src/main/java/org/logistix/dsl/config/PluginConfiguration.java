package org.logistix.dsl.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable configuration parameters for plugin loading and lifecycle.
 */
public record PluginConfiguration(
        boolean autoDiscoveryEnabled,
        List<String> enabledPluginIds,
        Map<String, Object> pluginSettings
) {
    public PluginConfiguration {
        enabledPluginIds = enabledPluginIds != null ? List.copyOf(enabledPluginIds) : Collections.emptyList();
        pluginSettings = pluginSettings != null ? Map.copyOf(pluginSettings) : Collections.emptyMap();
    }

    public static PluginConfiguration defaults() {
        return new PluginConfiguration(true, Collections.emptyList(), Collections.emptyMap());
    }
}
