package org.logistix.engine.action;

import org.logistix.domain.action.ActionApprovalGrant;
import org.logistix.domain.action.ActionAuditEntry;
import org.logistix.domain.action.ActionDecision;
import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionStatus;
import org.logistix.domain.action.ActionTelemetry;
import org.logistix.domain.action.AuthorizedAction;
import org.logistix.domain.ports.ActionExecutor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production-grade deterministic Action Governance Engine with hardened boundary protections:
 * - Exact action binding via SHA-256 fingerprinting
 * - Clock-based expiration TTL evaluation
 * - Idempotency key mutation & replay detection
 * - Supervisor revalidation lifecycle for APPROVAL_REQUIRED actions
 * - Defensive audit logging
 */
public class DefaultActionGovernanceEngine implements ActionGovernanceEngine {

    private final ActionPolicy defaultPolicy;
    private final InMemoryActionAuditStore auditStore;
    private final Clock clock;
    private final Duration authorizationTtl;

    private final Map<String, ActionProposal> proposalIdempotencyStore = new ConcurrentHashMap<>();
    private final Map<String, ActionDecision> decisionIdempotencyStore = new ConcurrentHashMap<>();
    private final Map<String, ActionResult> executionIdempotencyStore = new ConcurrentHashMap<>();
    private volatile ActionTelemetry lastTelemetry;

    public DefaultActionGovernanceEngine() {
        this(ActionPolicy.standardOperationalPolicy(), new InMemoryActionAuditStore(), Clock.systemUTC(), Duration.ofMinutes(5));
    }

    public DefaultActionGovernanceEngine(ActionPolicy defaultPolicy, InMemoryActionAuditStore auditStore) {
        this(defaultPolicy, auditStore, Clock.systemUTC(), Duration.ofMinutes(5));
    }

    public DefaultActionGovernanceEngine(
            ActionPolicy defaultPolicy,
            InMemoryActionAuditStore auditStore,
            Clock clock,
            Duration authorizationTtl
    ) {
        this.defaultPolicy = defaultPolicy != null ? defaultPolicy : ActionPolicy.standardOperationalPolicy();
        this.auditStore = auditStore != null ? auditStore : new InMemoryActionAuditStore();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.authorizationTtl = (authorizationTtl != null && !authorizationTtl.isNegative() && !authorizationTtl.isZero())
                ? authorizationTtl : Duration.ofMinutes(5);
    }

    public InMemoryActionAuditStore getAuditStore() {
        return auditStore;
    }

    public Clock getClock() {
        return clock;
    }

    @Override
    public ActionDecision evaluate(ActionProposal proposal) {
        return evaluate(proposal, defaultPolicy);
    }

    @Override
    public ActionDecision evaluate(ActionProposal proposal, ActionPolicy policy) {
        Objects.requireNonNull(proposal, "ActionProposal must not be null");
        ActionPolicy activePolicy = policy != null ? policy : defaultPolicy;

        // 1. Idempotency & Tampering Protection
        String idempKey = proposal.idempotencyKey();
        if (proposalIdempotencyStore.containsKey(idempKey)) {
            ActionProposal originalProposal = proposalIdempotencyStore.get(idempKey);
            // Verify if someone attempts to reuse an existing idempotency key with mutated parameters or target
            if (!Objects.equals(originalProposal.targetResource(), proposal.targetResource()) ||
                !Objects.equals(originalProposal.parameters(), proposal.parameters()) ||
                !Objects.equals(originalProposal.actionType(), proposal.actionType())) {
                List<String> violations = List.of("Idempotency key reused with mutated parameters or target resource");
                ActionDecision tampered = ActionDecision.rejected(proposal, "Idempotency key parameter tampering detected", violations);
                recordDecision(proposal, activePolicy, tampered);
                return tampered;
            }
            return decisionIdempotencyStore.get(idempKey);
        }

        List<String> violations = new ArrayList<>();
        List<String> requiredApprovals = new ArrayList<>();

        // 2. Action Type Whitelist Check
        if (!activePolicy.allowedActionTypes().isEmpty() && !activePolicy.allowedActionTypes().contains(proposal.actionType())) {
            violations.add(String.format("Action type [%s] is not permitted by policy [%s]",
                    proposal.actionType().code(), activePolicy.policyId()));
            ActionDecision decision = ActionDecision.rejected(proposal, "Action type not permitted by policy", violations);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        // 3. HARD Constraint Validation
        try {
            if (!activePolicy.hardConstraintValidator().test(proposal)) {
                violations.add("Action violates HARD operational constraint or regulatory safety rule");
                ActionDecision decision = ActionDecision.rejected(proposal, "HARD constraint violation", violations);
                recordDecision(proposal, activePolicy, decision);
                return decision;
            }
        } catch (Exception ex) {
            violations.add("HARD constraint validator error: " + ex.getMessage());
            ActionDecision decision = ActionDecision.rejected(proposal, "Constraint validation error", violations);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        // 4. Maximum Allowed Risk Level Check
        int proposalRiskRank = riskRank(proposal.riskLevel());
        int maxPolicyRiskRank = riskRank(activePolicy.maxAllowedRiskLevel());
        if (proposalRiskRank > maxPolicyRiskRank) {
            violations.add(String.format("Risk level [%s] exceeds maximum allowed policy risk [%s]",
                    proposal.riskLevel(), activePolicy.maxAllowedRiskLevel()));
            ActionDecision decision = ActionDecision.rejected(proposal, "Risk level exceeds policy threshold", violations);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        // 5. Human Operational Approval Requirement Check
        if (activePolicy.approvalRequiredRiskLevels().contains(proposal.riskLevel().toUpperCase())) {
            requiredApprovals.add(String.format("Risk level [%s] requires human operational supervisor approval", proposal.riskLevel()));
            ActionDecision decision = ActionDecision.approvalRequired(proposal, "Human operational approval required", requiredApprovals);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        // 6. Minimum Confidence Check
        if (proposal.confidence() < activePolicy.minConfidenceRequired()) {
            requiredApprovals.add(String.format("Advisory confidence (%.2f) is below autonomous threshold (%.2f)",
                    proposal.confidence(), activePolicy.minConfidenceRequired()));
            ActionDecision decision = ActionDecision.approvalRequired(proposal, "Confidence below autonomous execution threshold", requiredApprovals);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        // 7. Deterministic Authorization Approval with Fingerprint and Expiration TTL
        Instant now = clock.instant();
        AuthorizedAction authorizedAction = AuthorizedAction.issue(
                proposal,
                activePolicy.policyId(),
                "LogistiX-ActionGovernance",
                authorizationTtl,
                now
        );
        ActionDecision decision = ActionDecision.approved(
                proposal,
                authorizedAction,
                "Action deterministically authorized under policy " + activePolicy.policyId()
        );
        recordDecision(proposal, activePolicy, decision);
        return decision;
    }

    @Override
    public ActionDecision revalidateAndAuthorize(ActionProposal proposal, ActionApprovalGrant grant, ActionPolicy policy) {
        Objects.requireNonNull(proposal, "ActionProposal must not be null");
        Objects.requireNonNull(grant, "ActionApprovalGrant must not be null");
        ActionPolicy activePolicy = policy != null ? policy : defaultPolicy;

        List<String> violations = new ArrayList<>();

        // Verify grant matches proposal
        if (!Objects.equals(proposal.actionId(), grant.actionId())) {
            violations.add(String.format("Approval grant action ID [%s] does not match proposal action ID [%s]",
                    grant.actionId(), proposal.actionId()));
            ActionDecision decision = ActionDecision.rejected(proposal, "Approval grant action ID mismatch", violations);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        if (grant.expectedTargetResource() != null && !grant.expectedTargetResource().equals(proposal.targetResource())) {
            violations.add(String.format("Approval grant target [%s] does not match proposal target [%s]",
                    grant.expectedTargetResource(), proposal.targetResource()));
            ActionDecision decision = ActionDecision.rejected(proposal, "Approval grant target resource mismatch", violations);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        // Revalidate HARD constraints
        if (!activePolicy.hardConstraintValidator().test(proposal)) {
            violations.add("Revalidation failed: Proposal violates HARD operational constraint");
            ActionDecision decision = ActionDecision.rejected(proposal, "HARD constraint violation during revalidation", violations);
            recordDecision(proposal, activePolicy, decision);
            return decision;
        }

        // Issue fresh AuthorizedAction bound to the supervisor's grant
        Instant now = clock.instant();
        AuthorizedAction authorizedAction = AuthorizedAction.issue(
                proposal,
                activePolicy.policyId(),
                grant.approvedBy(),
                authorizationTtl,
                now
        );

        ActionDecision decision = ActionDecision.approved(
                proposal,
                authorizedAction,
                "Action revalidated and approved with supervisor grant " + grant.grantId()
        );
        recordDecision(proposal, activePolicy, decision);
        return decision;
    }

    @Override
    public ActionResult executeIfAuthorized(ActionProposal proposal, ActionPolicy policy, ActionExecutor executor) {
        Objects.requireNonNull(proposal, "ActionProposal must not be null");
        Objects.requireNonNull(executor, "ActionExecutor must not be null");

        // Idempotent execution check
        String idempKey = proposal.idempotencyKey();
        if (executionIdempotencyStore.containsKey(idempKey)) {
            return executionIdempotencyStore.get(idempKey);
        }

        Instant govStart = clock.instant();
        ActionDecision decision = evaluate(proposal, policy);
        Duration govLatency = Duration.between(govStart, clock.instant());

        // NON-NEGOTIABLE BOUNDARY: Only APPROVED actions may reach the executor
        if (!decision.isApproved() || decision.authorizedAction().isEmpty()) {
            this.lastTelemetry = ActionTelemetry.of(
                    proposal.actionId(),
                    proposal.actionType(),
                    decision.status(),
                    govLatency,
                    Duration.ZERO,
                    executor.getExecutorType(),
                    false,
                    proposal.correlationId()
            );

            ActionResult rejectedResult = ActionResult.failure(
                    proposal.actionId(),
                    "NONE",
                    decision.reason(),
                    "Action not authorized: " + decision.status(),
                    Duration.ZERO
            );

            recordExecution(proposal, policy, decision, rejectedResult, executor.getExecutorType());
            return rejectedResult;
        }

        AuthorizedAction authorizedAction = decision.authorizedAction().get();

        // 1. Authorization Expiration Invariant Check
        if (authorizedAction.isExpired(clock)) {
            this.lastTelemetry = ActionTelemetry.of(
                    proposal.actionId(),
                    proposal.actionType(),
                    ActionStatus.FAILED,
                    govLatency,
                    Duration.ZERO,
                    executor.getExecutorType(),
                    false,
                    proposal.correlationId()
            );
            ActionResult expiredResult = ActionResult.failure(
                    proposal.actionId(),
                    "EXPIRED",
                    "AuthorizedAction expired before execution",
                    "Security Violation: Attempted to execute expired authorization",
                    Duration.ZERO
            );
            recordExecution(proposal, policy, decision, expiredResult, executor.getExecutorType());
            return expiredResult;
        }

        // 2. Exact Action Fingerprint Invariant Check
        if (!authorizedAction.matchesFingerprint()) {
            this.lastTelemetry = ActionTelemetry.of(
                    proposal.actionId(),
                    proposal.actionType(),
                    ActionStatus.FAILED,
                    govLatency,
                    Duration.ZERO,
                    executor.getExecutorType(),
                    false,
                    proposal.correlationId()
            );
            ActionResult tamperedResult = ActionResult.failure(
                    proposal.actionId(),
                    "TAMPERED",
                    "AuthorizedAction fingerprint mismatch",
                    "Security Violation: Parameter or target tampering detected",
                    Duration.ZERO
            );
            recordExecution(proposal, policy, decision, tamperedResult, executor.getExecutorType());
            return tamperedResult;
        }

        // Execute authorized action through the outbound executor/MCP adapter
        Instant execStart = clock.instant();
        ActionResult result;
        try {
            result = executor.execute(authorizedAction);
        } catch (Exception ex) {
            Duration execLatency = Duration.between(execStart, clock.instant());
            result = ActionResult.failure(
                    proposal.actionId(),
                    "ERR-" + proposal.actionId(),
                    "Execution exception: " + ex.getMessage(),
                    ex.toString(),
                    execLatency
            );
        }

        Duration execLatency = Duration.between(execStart, clock.instant());
        this.lastTelemetry = ActionTelemetry.of(
                proposal.actionId(),
                proposal.actionType(),
                decision.status(),
                govLatency,
                execLatency,
                executor.getExecutorType(),
                result.isSuccess(),
                proposal.correlationId()
        );

        executionIdempotencyStore.put(idempKey, result);
        recordExecution(proposal, policy, decision, result, executor.getExecutorType());
        return result;
    }

    @Override
    public ActionTelemetry getLastTelemetry() {
        return lastTelemetry;
    }

    private void recordDecision(ActionProposal proposal, ActionPolicy policy, ActionDecision decision) {
        proposalIdempotencyStore.put(proposal.idempotencyKey(), proposal);
        decisionIdempotencyStore.put(proposal.idempotencyKey(), decision);
        auditStore.record(new ActionAuditEntry(
                proposal.actionId(),
                proposal.actionType(),
                proposal.source(),
                proposal.requestedBy(),
                decision.status(),
                decision.reason(),
                policy.policyId(),
                proposal.riskLevel(),
                proposal.confidence(),
                decision.status(),
                "NONE",
                "",
                proposal.correlationId(),
                proposal.idempotencyKey(),
                Collections.emptyMap(),
                clock.instant()
        ));
    }

    private void recordExecution(ActionProposal proposal, ActionPolicy policy, ActionDecision decision, ActionResult result, String executorType) {
        auditStore.record(new ActionAuditEntry(
                proposal.actionId(),
                proposal.actionType(),
                proposal.source(),
                proposal.requestedBy(),
                decision.status(),
                decision.reason(),
                policy != null ? policy.policyId() : defaultPolicy.policyId(),
                proposal.riskLevel(),
                proposal.confidence(),
                result.status(),
                executorType,
                result.operationId(),
                proposal.correlationId(),
                proposal.idempotencyKey(),
                Collections.emptyMap(),
                clock.instant()
        ));
    }

    private int riskRank(String risk) {
        if (risk == null) return 1;
        return switch (risk.toUpperCase()) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "HIGH" -> 3;
            case "CRITICAL" -> 4;
            default -> 1;
        };
    }
}
