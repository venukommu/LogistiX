package org.logistix.domain.action;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable, technology-neutral proposal for an enterprise action originating from AI, rules, humans, or system automation.
 * An ActionProposal is strictly an unverified request and CANNOT be executed directly without LogistiX governance authorization.
 */
public record ActionProposal(
        String actionId,
        ActionType actionType,
        String targetResource,
        Map<String, Object> parameters,
        String reason,
        ActionProposalSource source,
        double confidence,
        String riskLevel,
        String requestedBy,
        String correlationId,
        String idempotencyKey,
        Instant requestedAt
) {
    public ActionProposal {
        actionId = (actionId != null && !actionId.isBlank()) ? actionId : UUID.randomUUID().toString();
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(targetResource, "targetResource must not be null");
        parameters = parameters != null ? Collections.unmodifiableMap(new LinkedHashMap<>(parameters)) : Collections.emptyMap();
        reason = reason != null ? reason : "";
        source = source != null ? source : ActionProposalSource.AI;
        riskLevel = riskLevel != null ? riskLevel.toUpperCase() : "LOW";
        requestedBy = requestedBy != null ? requestedBy : "system";
        correlationId = (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
        idempotencyKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : actionId;
        requestedAt = requestedAt != null ? requestedAt : Instant.now();

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String actionId;
        private ActionType actionType;
        private String targetResource;
        private final Map<String, Object> parameters = new LinkedHashMap<>();
        private String reason;
        private ActionProposalSource source = ActionProposalSource.AI;
        private double confidence = 0.90;
        private String riskLevel = "LOW";
        private String requestedBy = "AI Contextual Advisor";
        private String correlationId;
        private String idempotencyKey;
        private Instant requestedAt = Instant.now();

        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder targetResource(String targetResource) {
            this.targetResource = targetResource;
            return this;
        }

        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            if (parameters != null) {
                this.parameters.putAll(parameters);
            }
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder source(ActionProposalSource source) {
            this.source = source;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder requestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder requestedAt(Instant requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public ActionProposal build() {
            return new ActionProposal(
                    actionId, actionType, targetResource, parameters, reason,
                    source, confidence, riskLevel, requestedBy, correlationId,
                    idempotencyKey, requestedAt
            );
        }
    }
}
