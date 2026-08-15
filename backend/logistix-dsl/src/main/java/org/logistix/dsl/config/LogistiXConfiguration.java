package org.logistix.dsl.config;

import org.logistix.dsl.builder.ConfigurationBuilder;
import org.logistix.engine.configuration.EngineConfiguration;
import org.logistix.engine.configuration.TraceLevel;

import java.util.Objects;

/**
 * Top-level immutable configuration for the LogistiX Framework.
 */
public record LogistiXConfiguration(
        EngineConfiguration engine,
        DecisionConfiguration decision,
        PluginConfiguration plugins,
        ProviderConfiguration providers
) {
    public LogistiXConfiguration {
        engine = engine != null ? engine : EngineConfiguration.defaults();
        decision = decision != null ? decision : DecisionConfiguration.defaults();
        plugins = plugins != null ? plugins : PluginConfiguration.defaults();
        providers = providers != null ? providers : ProviderConfiguration.defaults();
    }

    public static LogistiXConfiguration defaults() {
        return new LogistiXConfiguration(
                EngineConfiguration.defaults(),
                DecisionConfiguration.defaults(),
                PluginConfiguration.defaults(),
                ProviderConfiguration.defaults()
        );
    }

    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }
}
