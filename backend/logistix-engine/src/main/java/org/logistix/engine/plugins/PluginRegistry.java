package org.logistix.engine.plugins;

import java.util.List;
import java.util.Optional;

/**
 * Registry managing discovery, initialization, and lifecycle of DecisionPlugins.
 */
public interface PluginRegistry {

    void register(DecisionPlugin plugin);

    Optional<DecisionPlugin> getPlugin(String pluginId);

    List<DecisionPlugin> getAllPlugins();

    void initializeAll(PluginContext context);

    void shutdownAll();
}
