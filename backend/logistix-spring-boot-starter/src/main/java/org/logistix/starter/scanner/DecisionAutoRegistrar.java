package org.logistix.starter.scanner;

import org.logistix.dsl.LogistiX;
import org.logistix.engine.context.LogistiXContext;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.plugins.DecisionPlugin;
import org.logistix.engine.plugins.PluginContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.Collections;
import java.util.List;

/**
 * Automatically populates the LogistiXContext with scanned pipelines and plugins upon application startup.
 */
public class DecisionAutoRegistrar {

    private final LogistiXContext logistixContext;
    private final PipelineScanner pipelineScanner;
    private final PluginScanner pluginScanner;

    public DecisionAutoRegistrar(
            LogistiXContext logistixContext,
            PipelineScanner pipelineScanner,
            PluginScanner pluginScanner
    ) {
        this.logistixContext = logistixContext;
        this.pipelineScanner = pipelineScanner;
        this.pluginScanner = pluginScanner;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 1. Auto-register scanned pipelines
        List<DecisionPipeline> pipelines = pipelineScanner.scanPipelines();
        for (DecisionPipeline pipeline : pipelines) {
            logistixContext.getDecisionRegistry().register(pipeline);
        }

        // 2. Auto-register scanned plugins
        List<DecisionPlugin> plugins = pluginScanner.scanPlugins();
        for (DecisionPlugin plugin : plugins) {
            logistixContext.getPluginRegistry().register(plugin);
            plugin.initialize(PluginContext.of(Collections.emptyMap()));
        }

        // 3. Make this context globally accessible to LogistiX public facade
        LogistiX.setContext(logistixContext);
    }
}
