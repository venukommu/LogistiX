package org.logistix.starter.scanner;

import org.logistix.engine.plugins.DecisionPlugin;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scans the Spring ApplicationContext for declared DecisionPlugins.
 */
public class PluginScanner {

    private final ApplicationContext applicationContext;

    public PluginScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<DecisionPlugin> scanPlugins() {
        List<DecisionPlugin> discovered = new ArrayList<>();

        // Discover directly defined DecisionPlugin beans
        Map<String, DecisionPlugin> pluginBeans = applicationContext.getBeansOfType(DecisionPlugin.class);
        discovered.addAll(pluginBeans.values());

        return List.copyOf(discovered);
    }
}
