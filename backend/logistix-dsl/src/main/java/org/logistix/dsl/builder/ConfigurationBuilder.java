package org.logistix.dsl.builder;

import org.logistix.dsl.config.DecisionConfiguration;
import org.logistix.dsl.config.LogistiXConfiguration;
import org.logistix.dsl.config.PluginConfiguration;
import org.logistix.dsl.config.ProviderConfiguration;
import org.logistix.engine.configuration.EngineConfiguration;
import org.logistix.engine.configuration.TraceLevel;

import java.time.Duration;

/**
 * Fluent builder for assembling LogistiXConfiguration.
 */
public class ConfigurationBuilder {

    private Duration timeout = Duration.ofSeconds(10);
    private TraceLevel traceLevel = TraceLevel.DETAILED;
    private boolean strictConstraints = true;
    private boolean failFast = false;
    private boolean autoDiscovery = true;

    public ConfigurationBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public ConfigurationBuilder traceLevel(TraceLevel traceLevel) {
        this.traceLevel = traceLevel;
        return this;
    }

    public ConfigurationBuilder strictConstraints(boolean strictConstraints) {
        this.strictConstraints = strictConstraints;
        return this;
    }

    public ConfigurationBuilder failFast(boolean failFast) {
        this.failFast = failFast;
        return this;
    }

    public ConfigurationBuilder autoDiscovery(boolean autoDiscovery) {
        this.autoDiscovery = autoDiscovery;
        return this;
    }

    public LogistiXConfiguration build() {
        EngineConfiguration engineConfig = new EngineConfiguration(
                timeout,
                traceLevel,
                strictConstraints,
                failFast,
                Runtime.getRuntime().availableProcessors(),
                java.util.Collections.emptyMap()
        );

        DecisionConfiguration decisionConfig = new DecisionConfiguration(
                timeout,
                strictConstraints,
                true,
                0.60,
                java.util.Collections.emptyMap()
        );

        PluginConfiguration pluginConfig = new PluginConfiguration(
                autoDiscovery,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyMap()
        );

        ProviderConfiguration providerConfig = ProviderConfiguration.defaults();

        return new LogistiXConfiguration(engineConfig, decisionConfig, pluginConfig, providerConfig);
    }
}
