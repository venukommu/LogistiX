package org.logistix.starter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot AutoConfiguration entry point for LogistiX AI Platform.
 */
@AutoConfiguration
@EnableConfigurationProperties(LogistixProperties.class)
@ConditionalOnProperty(prefix = "logistix", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogistixAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LogistixAutoConfiguration.class);

    public LogistixAutoConfiguration(LogistixProperties properties) {
        log.info("Initialized LogistiX AI Platform Starter [AI Provider: {}, RAG Enabled: {}, Decision Explainability: {}]",
                properties.getAi().getProvider(),
                properties.getRag().isEnabled(),
                properties.getDecisionEngine().isExplainabilityEnabled());
    }
}
