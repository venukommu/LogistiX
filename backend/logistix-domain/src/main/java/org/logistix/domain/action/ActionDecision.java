package org.logistix.domain.action;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Strongly typed, explainable governance outcome for an ActionProposal.
 * Explicitly distinguishes APPROVED, REJECTED, and APPROVAL_REQUIRED states.
 */
public record ActionDecision(
        String actionId,
        ActionType actionType,
        ActionStatus status,
        String reason,
        List<String> violatedConstraints,
        List<String> requiredApprovals,
        String riskLevel,
        double confidence,
        Optional<AuthorizedAction> authorizedAction,
        Map<String, String> auditMetadata,
        String correlationId,
        Instant decidedAt
) {
    public ActionDecision {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        reason = reason != null ? reason : "";
        violatedConstraints = violatedConstraints != null ? List.copyOf(violatedConstraints) : Collections.emptyList();
        requiredApprovals = requiredApprovals != null ? List.copyOf(requiredApprovals) : Collections.emptyList();
        riskLevel = riskLevel != null ? riskLevel : "UNKNOWN";
        authorizedAction = authorizedAction != null ? authorizedAction : Optional.empty();
        auditMetadata = auditMetadata != null ? Map.copyOf(auditMetadata) : Collections.emptyMap();
        correlationId = correlationId != null ? correlationId : actionId;
        decidedAt = decidedAt != null ? decidedAt : Instant.now();
    }

    public static ActionDecision approved(ActionProposal proposal, AuthorizedAction authorizedAction, String reason) {
        return new ActionDecision(
                proposal.actionId(),
                proposal.actionType(),
                ActionStatus.APPROVED,
                reason,
                Collections.emptyList(),
                Collections.emptyList(),
                proposal.riskLevel(),
                proposal.confidence(),
                Optional.of(authorizedAction),
                Collections.emptyMap(),
                proposal.correlationId(),
                Instant.now()
        );
    }

    public static ActionDecision rejected(ActionProposal proposal, String reason, List<String> violatedConstraints) {
        return new ActionDecision(
                proposal.actionId(),
                proposal.actionType(),
                ActionStatus.REJECTED,
                reason,
                violatedConstraints,
                Collections.emptyList(),
                proposal.riskLevel(),
                proposal.confidence(),
                Optional.empty(),
                Collections.emptyMap(),
                proposal.correlationId(),
                Instant.now()
        );
    }

    public static ActionDecision approvalRequired(ActionProposal proposal, String reason, List<String> requiredApprovals) {
        return new ActionDecision(
                proposal.actionId(),
                proposal.actionType(),
                ActionStatus.APPROVAL_REQUIRED,
                reason,
                Collections.emptyList(),
                requiredApprovals,
                proposal.riskLevel(),
                proposal.confidence(),
                Optional.empty(),
                Collections.emptyMap(),
                proposal.correlationId(),
                Instant.now()
        );
    }

    public boolean isApproved() {
        return status == ActionStatus.APPROVED && authorizedAction.isPresent();
    }

    public boolean isRejected() {
        return status == ActionStatus.REJECTED;
    }

    public boolean isApprovalRequired() {
        return status == ActionStatus.APPROVAL_REQUIRED;
    }
}
