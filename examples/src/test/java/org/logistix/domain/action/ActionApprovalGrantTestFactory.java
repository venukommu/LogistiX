package org.logistix.domain.action;

import java.time.Instant;

/**
 * Test-only factory for synthesizing ActionApprovalGrant test doubles, tampered grants, and forged approval scenarios.
 * Strictly restricted to test sources; never exposed in production distributions.
 */
public final class ActionApprovalGrantTestFactory {

    private ActionApprovalGrantTestFactory() {}

    public static ActionApprovalGrant validGrant(ActionProposal proposal, String approverId, String reason) {
        ActionApprovalIssuer issuer = new DefaultActionApprovalIssuer();
        return issuer.issueApproval(proposal, approverId, reason);
    }

    public static ActionApprovalGrant forgedGrant(
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
        return ActionApprovalGrant.createInternal(
                grantId, actionId, proposalFingerprint, approvedBy, reason,
                expectedTargetResource, provenance, grantedAt, consumed
        );
    }

    public static ActionApprovalGrant forgedSelfIssuedGrant(ActionProposal proposal, String spoofedApprover) {
        String fingerprint = ParameterCanonicalizer.canonicalize(proposal.parameters()) + "|" +
                proposal.targetResource() + "|" + proposal.actionType().code();
        ApprovalProvenance fakeProv = new ApprovalProvenance(
                spoofedApprover,
                "APPRV-FORGED",
                "Fake-Authority",
                fingerprint,
                "OPERATIONAL_APPROVAL",
                Instant.now()
        );
        return ActionApprovalGrant.createInternal(
                "GRANT-FORGED-001",
                proposal.actionId(),
                fingerprint,
                spoofedApprover,
                "Self-issued spoofed approval",
                proposal.targetResource(),
                fakeProv,
                Instant.now(),
                false
        );
    }
}
