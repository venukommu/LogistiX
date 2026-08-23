package org.logistix.domain.action;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable audit record capturing the entire lifecycle of an Action (Proposal -> Governance -> Authorization -> Execution).
 */
public record ActionAuditEntry(
        String actionId,
        ActionType actionType,
        ActionProposalSource proposalSource,
        String requestedBy,
        ActionStatus governanceStatus,
        String governanceReason,
        String policyApplied,
        String riskLevel,
        double confidence,
        ActionStatus executionStatus,
        String executorType,
        String operationId,
        String correlationId,
        String idempotencyKey,
        Map<String, String> auditMetadata,
        Instant timestamp
) {
    public ActionAuditEntry {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(governanceStatus, "governanceStatus must not be null");
        proposalSource = proposalSource != null ? proposalSource : ActionProposalSource.AI;
        requestedBy = requestedBy != null ? requestedBy : "system";
        governanceReason = governanceReason != null ? governanceReason : "";
        policyApplied = policyApplied != null ? policyApplied : "DEFAULT";
        riskLevel = riskLevel != null ? riskLevel : "LOW";
        executionStatus = executionStatus != null ? executionStatus : governanceStatus;
        executorType = executorType != null ? executorType : "NONE";
        operationId = operationId != null ? operationId : "";
        correlationId = correlationId != null ? correlationId : actionId;
        idempotencyKey = idempotencyKey != null ? idempotencyKey : actionId;
        auditMetadata = auditMetadata != null ? Map.copyOf(auditMetadata) : Collections.emptyMap();
        timestamp = timestamp != null ? timestamp : Instant.now();
    }
}
