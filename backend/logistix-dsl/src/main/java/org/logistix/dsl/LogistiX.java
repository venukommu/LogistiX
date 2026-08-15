package org.logistix.dsl;

import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.events.DomainEvent;
import org.logistix.domain.events.DomainEventPublisher;
import org.logistix.dsl.builder.ConfigurationBuilder;
import org.logistix.dsl.builder.ContextBuilder;
import org.logistix.dsl.builder.PipelineBuilder;
import org.logistix.dsl.builder.PluginBuilder;
import org.logistix.dsl.config.LogistiXConfiguration;
import org.logistix.dsl.fluent.FluentContext;
import org.logistix.dsl.fluent.FluentDecision;
import org.logistix.dsl.fluent.FluentPipeline;
import org.logistix.engine.configuration.EngineConfiguration;
import org.logistix.engine.context.LogistiXContext;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.executor.DefaultDecisionExecutor;
import org.logistix.engine.hooks.DecisionHook;
import org.logistix.engine.hooks.HookRegistry;
import org.logistix.engine.metrics.DecisionMetrics;
import org.logistix.engine.metrics.MetricsCollector;
import org.logistix.engine.metrics.StepMetrics;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.engine.plugins.DecisionPlugin;
import org.logistix.engine.plugins.PluginContext;
import org.logistix.engine.plugins.PluginRegistry;
import org.logistix.engine.registry.DecisionRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public Main Entry Point for the LogistiX Framework.
 *
 * <p>Exposes fluent static factory methods for constructing and executing decision pipelines,
 * configuring runtime instances, and building operational contexts with zero boilerplate.</p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // 1. Fluent Decision Execution
 * DecisionResult<Driver> result = LogistiX.decision("driver-dispatch")
 *     .fact("shipment", shipment)
 *     .fact("candidateDrivers", drivers)
 *     .execute();
 *
 * // 2. Fluent Pipeline Assembly
 * DecisionPipeline pipeline = LogistiX.pipeline("carrier-selection")
 *     .step(new CarrierConstraintStep())
 *     .step(new CarrierRuleStep())
 *     .step(new CarrierScoringStep())
 *     .build();
 * }</pre>
 */
public final class LogistiX {

    private static volatile LogistiXContext globalContext = createDefaultContext();

    private LogistiX() {
        // Prevent direct instantiation of utility class
    }

    /**
     * Start building a fluent Decision execution for the given decision type.
     */
    public static <T> FluentDecision<T> decision(String decisionType) {
        return new FluentDecision<>(decisionType);
    }

    /**
     * Start building a fluent DecisionPipeline for the given decision type.
     */
    public static FluentPipeline pipeline(String decisionType) {
        return new FluentPipeline(decisionType);
    }

    /**
     * Start building a fluent DecisionContext for the given decision type.
     */
    public static FluentContext context(String decisionType) {
        return new FluentContext(decisionType);
    }

    /**
     * Obtain a fluent configuration builder for LogistiX.
     */
    public static ConfigurationBuilder configure() {
        return new ConfigurationBuilder();
    }

    /**
     * Fluent builder for assembling a standalone DecisionPipeline.
     */
    public static PipelineBuilder pipelineBuilder(String decisionType) {
        return PipelineBuilder.of(decisionType);
    }

    /**
     * Fluent builder for assembling a standalone DecisionContext.
     */
    public static ContextBuilder contextBuilder() {
        return new ContextBuilder();
    }

    /**
     * Fluent builder for declaring an inline DecisionPlugin.
     */
    public static PluginBuilder pluginBuilder(String pluginId) {
        return PluginBuilder.of(pluginId);
    }

    /**
     * Access the active LogistiXContext runtime container.
     */
    public static LogistiXContext getContext() {
        return globalContext;
    }

    /**
     * Set or override the global LogistiXContext (e.g. called by Spring Boot AutoConfiguration).
     */
    public static void setContext(LogistiXContext context) {
        globalContext = Objects.requireNonNull(context, "LogistiXContext must not be null");
    }

    /**
     * Reset the global runtime context to default settings.
     */
    public static void reset() {
        globalContext = createDefaultContext();
    }

    private static LogistiXContext createDefaultContext() {
        EngineConfiguration engineConfig = EngineConfiguration.defaults();
        DecisionRegistry decisionRegistry = new InMemoryDecisionRegistry();
        PluginRegistry pluginRegistry = new InMemoryPluginRegistry();
        HookRegistry hookRegistry = new InMemoryHookRegistry();
        MetricsCollector metricsCollector = new SimpleMetricsCollector();
        DomainEventPublisher eventPublisher = new SimpleEventPublisher();

        DecisionExecutor executor = new DefaultDecisionExecutor(
                decisionRegistry,
                hookRegistry,
                engineConfig,
                eventPublisher
        );

        return new DefaultLogistiXContext(
                engineConfig,
                decisionRegistry,
                pluginRegistry,
                hookRegistry,
                metricsCollector,
                eventPublisher,
                executor
        );
    }

    private static class DefaultLogistiXContext implements LogistiXContext {
        private final EngineConfiguration configuration;
        private final DecisionRegistry decisionRegistry;
        private final PluginRegistry pluginRegistry;
        private final HookRegistry hookRegistry;
        private final MetricsCollector metricsCollector;
        private final DomainEventPublisher eventPublisher;
        private final DecisionExecutor executor;

        DefaultLogistiXContext(
                EngineConfiguration configuration,
                DecisionRegistry decisionRegistry,
                PluginRegistry pluginRegistry,
                HookRegistry hookRegistry,
                MetricsCollector metricsCollector,
                DomainEventPublisher eventPublisher,
                DecisionExecutor executor
        ) {
            this.configuration = configuration;
            this.decisionRegistry = decisionRegistry;
            this.pluginRegistry = pluginRegistry;
            this.hookRegistry = hookRegistry;
            this.metricsCollector = metricsCollector;
            this.eventPublisher = eventPublisher;
            this.executor = executor;
        }

        @Override public EngineConfiguration getConfiguration() { return configuration; }
        @Override public DecisionRegistry getDecisionRegistry() { return decisionRegistry; }
        @Override public PluginRegistry getPluginRegistry() { return pluginRegistry; }
        @Override public HookRegistry getHookRegistry() { return hookRegistry; }
        @Override public MetricsCollector getMetricsCollector() { return metricsCollector; }
        @Override public DomainEventPublisher getEventPublisher() { return eventPublisher; }
        @Override public DecisionExecutor getExecutor() { return executor; }
    }

    private static class InMemoryDecisionRegistry implements DecisionRegistry {
        private final Map<String, DecisionPipeline> registry = new ConcurrentHashMap<>();

        @Override
        public void register(DecisionPipeline pipeline) {
            registry.put(pipeline.decisionType(), pipeline);
        }

        @Override
        public Optional<DecisionPipeline> getPipeline(String decisionType) {
            return Optional.ofNullable(registry.get(decisionType));
        }

        @Override
        public boolean hasPipeline(String decisionType) {
            return registry.containsKey(decisionType);
        }

        @Override
        public List<DecisionPipeline> getAllPipelines() {
            return List.copyOf(registry.values());
        }

        @Override
        public void unregister(String decisionType) {
            registry.remove(decisionType);
        }
    }

    private static class InMemoryPluginRegistry implements PluginRegistry {
        private final Map<String, DecisionPlugin> plugins = new ConcurrentHashMap<>();

        @Override
        public void register(DecisionPlugin plugin) {
            plugins.put(plugin.getPluginId(), plugin);
        }

        @Override
        public Optional<DecisionPlugin> getPlugin(String pluginId) {
            return Optional.ofNullable(plugins.get(pluginId));
        }

        @Override
        public List<DecisionPlugin> getAllPlugins() {
            return List.copyOf(plugins.values());
        }

        @Override
        public void initializeAll(PluginContext context) {
            plugins.values().forEach(p -> p.initialize(context));
        }

        @Override
        public void shutdownAll() {
            plugins.values().forEach(DecisionPlugin::shutdown);
        }
    }

    private static class InMemoryHookRegistry implements HookRegistry {
        private final List<DecisionHook> hooks = new CopyOnWriteArrayList<>();

        @Override
        public void register(DecisionHook hook) {
            hooks.add(hook);
        }

        @Override
        public List<DecisionHook> getHooks() {
            return List.copyOf(hooks);
        }
    }

    private static class SimpleMetricsCollector implements MetricsCollector {
        private final List<StepMetrics> stepMetrics = new CopyOnWriteArrayList<>();
        private long aiTokens = 0;
        private Duration aiDuration = Duration.ZERO;
        private double confidence = 0.0;
        private final List<String> warnings = new CopyOnWriteArrayList<>();
        private final List<String> errors = new CopyOnWriteArrayList<>();

        @Override
        public void recordStep(StepMetrics metrics) {
            stepMetrics.add(metrics);
        }

        @Override
        public void recordAiUsage(long tokens, Duration duration) {
            this.aiTokens += tokens;
            this.aiDuration = this.aiDuration.plus(duration);
        }

        @Override
        public void recordConfidence(double confidence) {
            this.confidence = confidence;
        }

        @Override
        public void recordWarning(String warning) {
            warnings.add(warning);
        }

        @Override
        public void recordError(String error) {
            errors.add(error);
        }

        @Override
        public DecisionMetrics snapshot(Duration totalDuration) {
            return new DecisionMetrics(
                    totalDuration,
                    List.copyOf(stepMetrics),
                    0, 0, 0,
                    aiTokens,
                    aiDuration,
                    confidence,
                    warnings.size(),
                    errors.size()
            );
        }
    }

    private static class SimpleEventPublisher implements DomainEventPublisher {
        @Override
        public void publish(DomainEvent event) {
            // Default no-op dispatcher in core runtime
        }
    }
}
