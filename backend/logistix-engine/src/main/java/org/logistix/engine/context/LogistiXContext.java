package org.logistix.engine.context;

import org.logistix.domain.events.DomainEventPublisher;
import org.logistix.engine.configuration.EngineConfiguration;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.hooks.HookRegistry;
import org.logistix.engine.metrics.MetricsCollector;
import org.logistix.engine.plugins.PluginRegistry;
import org.logistix.engine.registry.DecisionRegistry;

/**
 * The Central Global Runtime Container of the LogistiX Framework.
 *
 * Serves as the primary operational context (analogous to Spring's ApplicationContext)
 * managing runtime configuration, decision pipelines, dynamic plugins, lifecycle hooks,
 * telemetry metrics collectors, and event dispatchers.
 */
public interface LogistiXContext {

    EngineConfiguration getConfiguration();

    DecisionRegistry getDecisionRegistry();

    PluginRegistry getPluginRegistry();

    HookRegistry getHookRegistry();

    MetricsCollector getMetricsCollector();

    DomainEventPublisher getEventPublisher();

    DecisionExecutor getExecutor();
}
