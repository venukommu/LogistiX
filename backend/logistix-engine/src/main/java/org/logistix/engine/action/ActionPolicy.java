package org.logistix.engine.action;

import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Declarative policy governing deterministic authorization of ActionProposals.
 */
public record ActionPolicy(
        String policyId,
        String policyName,
        Set<ActionType> allowedActionTypes,
        Set<String> requiredPermissions,
        String maxAllowedRiskLevel,
        double minConfidenceRequired,
        Set<String> approvalRequiredRiskLevels,
        Predicate<ActionProposal> hardConstraintValidator
) {
    public ActionPolicy {
        Objects.requireNonNull(policyId, "policyId must not be null");
        policyName = policyName != null ? policyName : policyId;
        allowedActionTypes = allowedActionTypes != null ? Set.copyOf(allowedActionTypes) : Collections.emptySet();
        requiredPermissions = requiredPermissions != null ? Set.copyOf(requiredPermissions) : Collections.emptySet();
        maxAllowedRiskLevel = maxAllowedRiskLevel != null ? maxAllowedRiskLevel.toUpperCase() : "HIGH";
        approvalRequiredRiskLevels = approvalRequiredRiskLevels != null ? Set.copyOf(approvalRequiredRiskLevels) : Collections.emptySet();
        hardConstraintValidator = hardConstraintValidator != null ? hardConstraintValidator : p -> true;

        if (minConfidenceRequired < 0.0 || minConfidenceRequired > 1.0) {
            throw new IllegalArgumentException("minConfidenceRequired must be between 0.0 and 1.0");
        }
    }

    public static Builder builder(String policyId) {
        return new Builder(policyId);
    }

    public static ActionPolicy standardOperationalPolicy() {
        return builder("standard-operational-policy")
                .policyName("Standard Logistics Action Policy")
                .allowActionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                .allowActionType(ActionType.ASSIGN_DRIVER)
                .allowActionType(ActionType.UPDATE_SHIPMENT_STATUS)
                .minConfidence(0.80)
                .maxRiskLevel("HIGH")
                .requireApprovalForRisk("HIGH")
                .requireApprovalForRisk("CRITICAL")
                .build();
    }

    public static class Builder {
        private final String policyId;
        private String policyName;
        private final Set<ActionType> allowedActionTypes = new HashSet<>();
        private final Set<String> requiredPermissions = new HashSet<>();
        private String maxAllowedRiskLevel = "HIGH";
        private double minConfidenceRequired = 0.75;
        private final Set<String> approvalRequiredRiskLevels = new HashSet<>();
        private Predicate<ActionProposal> hardConstraintValidator = p -> true;

        public Builder(String policyId) {
            this.policyId = policyId;
        }

        public Builder policyName(String policyName) {
            this.policyName = policyName;
            return this;
        }

        public Builder allowActionType(ActionType actionType) {
            this.allowedActionTypes.add(actionType);
            return this;
        }

        public Builder allowActionTypes(Set<ActionType> actionTypes) {
            if (actionTypes != null) {
                this.allowedActionTypes.addAll(actionTypes);
            }
            return this;
        }

        public Builder requirePermission(String permission) {
            this.requiredPermissions.add(permission);
            return this;
        }

        public Builder maxRiskLevel(String maxRiskLevel) {
            this.maxAllowedRiskLevel = maxRiskLevel;
            return this;
        }

        public Builder minConfidence(double minConfidence) {
            this.minConfidenceRequired = minConfidence;
            return this;
        }

        public Builder requireApprovalForRisk(String riskLevel) {
            this.approvalRequiredRiskLevels.add(riskLevel.toUpperCase());
            return this;
        }

        public Builder hardConstraintValidator(Predicate<ActionProposal> validator) {
            this.hardConstraintValidator = validator != null ? validator : p -> true;
            return this;
        }

        public ActionPolicy build() {
            return new ActionPolicy(
                    policyId, policyName, allowedActionTypes, requiredPermissions,
                    maxAllowedRiskLevel, minConfidenceRequired, approvalRequiredRiskLevels,
                    hardConstraintValidator
            );
        }
    }
}
