package org.logistix.starter.autoconfig;

import org.logistix.domain.events.DomainEvent;
import org.logistix.domain.events.DomainEventPublisher;
import org.logistix.dsl.LogistiX;
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
import org.logistix.starter.scanner.DecisionAutoRegistrar;
import org.logistix.starter.scanner.PipelineScanner;
import org.logistix.starter.scanner.PluginScanner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Spring Boot AutoConfiguration for the LogistiX Framework.
 */
@AutoConfiguration
@EnableConfigurationProperties(LogistiXProperties.class)
@ConditionalOnProperty(prefix = "logistix", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogistiXAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EngineConfiguration logistixEngineConfiguration(LogistiXProperties properties) {
        return new EngineConfiguration(
                properties.getDefaultTimeout(),
                properties.getTraceLevel(),
                properties.isStrictConstraints(),
                properties.isFailFastOnRuleError(),
                Runtime.getRuntime().availableProcessors(),
                Collections.emptyMap()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public DecisionRegistry logistixDecisionRegistry() {
        return new SpringDecisionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry logistixPluginRegistry() {
        return new SpringPluginRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public HookRegistry logistixHookRegistry() {
        return new SpringHookRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsCollector logistixMetricsCollector() {
        return new SpringMetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainEventPublisher logistixEventPublisher(ApplicationEventPublisher springPublisher) {
        return springPublisher::publishEvent;
    }

    @Bean
    @ConditionalOnMissingBean
    public DecisionExecutor logistixDecisionExecutor(
            DecisionRegistry decisionRegistry,
            HookRegistry hookRegistry,
            EngineConfiguration engineConfiguration,
            DomainEventPublisher eventPublisher
    ) {
        return new DefaultDecisionExecutor(decisionRegistry, hookRegistry, engineConfiguration, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogistiXContext logistixContext(
            EngineConfiguration configuration,
            DecisionRegistry decisionRegistry,
            PluginRegistry pluginRegistry,
            HookRegistry hookRegistry,
            MetricsCollector metricsCollector,
            DomainEventPublisher eventPublisher,
            DecisionExecutor executor
    ) {
        SpringLogistiXContext context = new SpringLogistiXContext(
                configuration,
                decisionRegistry,
                pluginRegistry,
                hookRegistry,
                metricsCollector,
                eventPublisher,
                executor
        );
        LogistiX.setContext(context);
        return context;
    }

    @Bean
    @ConditionalOnMissingBean
    public PipelineScanner logistixPipelineScanner(ApplicationContext applicationContext) {
        return new PipelineScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginScanner logistixPluginScanner(ApplicationContext applicationContext) {
        return new PluginScanner(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public DecisionAutoRegistrar logistixDecisionAutoRegistrar(
            LogistiXContext logistixContext,
            PipelineScanner pipelineScanner,
            PluginScanner pluginScanner
    ) {
        return new DecisionAutoRegistrar(logistixContext, pipelineScanner, pluginScanner);
    }

    private static class SpringLogistiXContext implements LogistiXContext {
        private final EngineConfiguration configuration;
        private final DecisionRegistry decisionRegistry;
        private final PluginRegistry pluginRegistry;
        private final HookRegistry hookRegistry;
        private final MetricsCollector metricsCollector;
        private final DomainEventPublisher eventPublisher;
        private final DecisionExecutor executor;

        SpringLogistiXContext(
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

    private static class SpringDecisionRegistry implements DecisionRegistry {
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

    private static class SpringPluginRegistry implements PluginRegistry {
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

    private static class SpringHookRegistry implements HookRegistry {
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

    private static class SpringMetricsCollector implements MetricsCollector {
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
}
