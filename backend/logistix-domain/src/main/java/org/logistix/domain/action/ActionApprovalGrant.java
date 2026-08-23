package org.logistix.domain.action;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Domain entity capturing a human operational supervisor approval grant for an APPROVAL_REQUIRED action.
 * Encapsulates verified ApprovalProvenance, binds the grant to the exact proposal fingerprint,
 * and enforces single-use atomic consumption.
 */
public final class ActionApprovalGrant {

    private final String grantId;
    private final String actionId;
    private final String proposalFingerprint;
    private final String approvedBy;
    private final String reason;
    private final String expectedTargetResource;
    private final ApprovalProvenance provenance;
    private final Instant grantedAt;
    private final AtomicBoolean consumed;

    ActionApprovalGrant(
            String grantId,
            String actionId,
            String proposalFingerprint,
            String approvedBy,
            String reason,
            String expectedTargetResource,
            ApprovalProvenance provenance,
            Instant grantedAt,
            boolean consumed
    ) {
        this.grantId = (grantId != null && !grantId.isBlank()) ? grantId : "GRANT-" + UUID.randomUUID().toString().substring(0, 8);
        this.actionId = Objects.requireNonNull(actionId, "actionId must not be null");
        this.proposalFingerprint = Objects.requireNonNull(proposalFingerprint, "proposalFingerprint must not be null");
        this.approvedBy = Objects.requireNonNull(approvedBy, "approvedBy must not be null");
        this.reason = reason != null ? reason : "Operational supervisor approval granted";
        this.expectedTargetResource = expectedTargetResource != null ? expectedTargetResource : "";
        this.provenance = Objects.requireNonNull(provenance, "provenance must not be null");
        this.grantedAt = grantedAt != null ? grantedAt : Instant.now();
        this.consumed = new AtomicBoolean(consumed);
    }

    /**
     * Package-private factory method accessible strictly to trusted ActionApprovalIssuer implementations.
     */
    static ActionApprovalGrant createInternal(
            String grantId,
            String actionId,
            String proposalFingerprint,
            String approvedBy,
            String reason,
            String expectedTargetResource,
            ApprovalProvenance provenance,
            Instant grantedAt,
            boolean consumed
    ) {
        return new ActionApprovalGrant(
                grantId, actionId, proposalFingerprint, approvedBy, reason,
                expectedTargetResource, provenance, grantedAt, consumed
        );
    }

    public String grantId() { return grantId; }
    public String actionId() { return actionId; }
    public String proposalFingerprint() { return proposalFingerprint; }
    public String approvedBy() { return approvedBy; }
    public String reason() { return reason; }
    public String expectedTargetResource() { return expectedTargetResource; }
    public ApprovalProvenance provenance() { return provenance; }
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

        String currentFingerprint = ParameterCanonicalizer.canonicalize(proposal.parameters()) + "|" +
                proposal.targetResource() + "|" + proposal.actionType().code();
        return Objects.equals(this.proposalFingerprint, currentFingerprint);
    }
}
