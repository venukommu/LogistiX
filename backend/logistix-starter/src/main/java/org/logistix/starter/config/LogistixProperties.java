package org.logistix.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root configuration properties binding for LogistiX AI Platform.
 */
@ConfigurationProperties(prefix = "logistix")
public class LogistixProperties {

    private boolean enabled = true;
    private final Ai ai = new Ai();
    private final Rag rag = new Rag();
    private final DecisionEngine decisionEngine = new DecisionEngine();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Ai getAi() {
        return ai;
    }

    public Rag getRag() {
        return rag;
    }

    public DecisionEngine getDecisionEngine() {
        return decisionEngine;
    }

    public static class Ai {
        private String provider = "spring-ai";
        private String defaultModel = "gpt-4o";
        private double defaultTemperature = 0.2;
        private int maxTokens = 2048;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public double getDefaultTemperature() {
            return defaultTemperature;
        }

        public void setDefaultTemperature(double defaultTemperature) {
            this.defaultTemperature = defaultTemperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class Rag {
        private boolean enabled = true;
        private int defaultTopK = 5;
        private double minSimilarityThreshold = 0.75;
        private int embeddingDimensions = 1536;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public double getMinSimilarityThreshold() {
            return minSimilarityThreshold;
        }

        public void setMinSimilarityThreshold(double minSimilarityThreshold) {
            this.minSimilarityThreshold = minSimilarityThreshold;
        }

        public int getEmbeddingDimensions() {
            return embeddingDimensions;
        }

        public void setEmbeddingDimensions(int embeddingDimensions) {
            this.embeddingDimensions = embeddingDimensions;
        }
    }

    public static class DecisionEngine {
        private boolean explainabilityEnabled = true;
        private double minimumAcceptanceScore = 0.60;
        private boolean strictRuleEvaluation = true;

        public boolean isExplainabilityEnabled() {
            return explainabilityEnabled;
        }

        public void setExplainabilityEnabled(boolean explainabilityEnabled) {
            this.explainabilityEnabled = explainabilityEnabled;
        }

        public double getMinimumAcceptanceScore() {
            return minimumAcceptanceScore;
        }

        public void setMinimumAcceptanceScore(double minimumAcceptanceScore) {
            this.minimumAcceptanceScore = minimumAcceptanceScore;
        }

        public boolean isStrictRuleEvaluation() {
            return strictRuleEvaluation;
        }

        public void setStrictRuleEvaluation(boolean strictRuleEvaluation) {
            this.strictRuleEvaluation = strictRuleEvaluation;
        }
    }
}
