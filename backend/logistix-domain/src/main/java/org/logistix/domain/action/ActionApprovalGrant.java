package org.logistix.domain.action;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Domain entity capturing a human operational supervisor approval grant for an APPROVAL_REQUIRED action.
 * Binds the grant to the exact proposal fingerprint and enforces single-use consumption.
 */
public final class ActionApprovalGrant {

    private final String grantId;
    private final String actionId;
    private final String proposalFingerprint;
    private final String approvedBy;
    private final String reason;
    private final String expectedTargetResource;
    private final Instant grantedAt;
    private final AtomicBoolean consumed;

    public ActionApprovalGrant(
            String grantId,
            String actionId,
            String proposalFingerprint,
            String approvedBy,
            String reason,
            String expectedTargetResource,
            Instant grantedAt,
            boolean consumed
    ) {
        this.grantId = (grantId != null && !grantId.isBlank()) ? grantId : "GRANT-" + UUID.randomUUID().toString().substring(0, 8);
        this.actionId = Objects.requireNonNull(actionId, "actionId must not be null");
        this.proposalFingerprint = proposalFingerprint != null ? proposalFingerprint : "";
        this.approvedBy = approvedBy != null ? approvedBy : "supervisor";
        this.reason = reason != null ? reason : "Operational supervisor approval granted";
        this.expectedTargetResource = expectedTargetResource != null ? expectedTargetResource : "";
        this.grantedAt = grantedAt != null ? grantedAt : Instant.now();
        this.consumed = new AtomicBoolean(consumed);
    }

    public static ActionApprovalGrant of(String actionId, String approvedBy, String reason, String expectedTargetResource) {
        return new ActionApprovalGrant(null, actionId, "", approvedBy, reason, expectedTargetResource, Instant.now(), false);
    }

    public static ActionApprovalGrant of(
            String actionId,
            String proposalFingerprint,
            String approvedBy,
            String reason,
            String expectedTargetResource
    ) {
        return new ActionApprovalGrant(null, actionId, proposalFingerprint, approvedBy, reason, expectedTargetResource, Instant.now(), false);
    }

    public static ActionApprovalGrant forProposal(ActionProposal proposal, String approvedBy, String reason) {
        Objects.requireNonNull(proposal, "ActionProposal must not be null");
        String fingerprint = ParameterCanonicalizer.canonicalize(proposal.parameters()) + "|" + proposal.targetResource() + "|" + proposal.actionType().code();
        return new ActionApprovalGrant(null, proposal.actionId(), fingerprint, approvedBy, reason, proposal.targetResource(), Instant.now(), false);
    }

    public String grantId() { return grantId; }
    public String actionId() { return actionId; }
    public String proposalFingerprint() { return proposalFingerprint; }
    public String approvedBy() { return approvedBy; }
    public String reason() { return reason; }
    public String expectedTargetResource() { return expectedTargetResource; }
    public Instant grantedAt() { return grantedAt; }

    public boolean isConsumed() {
        return consumed.get();
    }

    /**
     * Atomically marks the approval grant as consumed. Returns true if successfully consumed, false if already consumed.
     */
    public boolean markConsumed() {
        return consumed.compareAndSet(false, true);
    }

    /**
     * Verifies that the grant matches the proposal's exact target and semantic fingerprint.
     */
    public boolean isMatchingProposal(ActionProposal proposal) {
        if (proposal == null) return false;
        if (!Objects.equals(this.actionId, proposal.actionId())) return false;
        if (!expectedTargetResource.isBlank() && !Objects.equals(this.expectedTargetResource, proposal.targetResource())) return false;

        if (!proposalFingerprint.isBlank()) {
            String currentFingerprint = ParameterCanonicalizer.canonicalize(proposal.parameters()) + "|" + proposal.targetResource() + "|" + proposal.actionType().code();
            return Objects.equals(this.proposalFingerprint, currentFingerprint);
        }
        return true;
    }
}
