package org.logistix.domain.action;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Production-grade trusted implementation of ActionApprovalIssuer.
 * Verifies approver credentials against TrustedApproverRegistry before issuing an ActionApprovalGrant.
 */
public class DefaultActionApprovalIssuer implements ActionApprovalIssuer {

    private final TrustedApproverRegistry approverRegistry;
    private final String issuerAuthorityId;

    public DefaultActionApprovalIssuer() {
        this(TrustedApproverRegistry.withStandardLogisticsApprovers(), "LogistiX-Approval-Authority");
    }

    public DefaultActionApprovalIssuer(TrustedApproverRegistry approverRegistry) {
        this(approverRegistry, "LogistiX-Approval-Authority");
    }

    public DefaultActionApprovalIssuer(TrustedApproverRegistry approverRegistry, String issuerAuthorityId) {
        this.approverRegistry = approverRegistry != null ? approverRegistry : TrustedApproverRegistry.withStandardLogisticsApprovers();
        this.issuerAuthorityId = (issuerAuthorityId != null && !issuerAuthorityId.isBlank())
                ? issuerAuthorityId : "LogistiX-Approval-Authority";
    }

    public TrustedApproverRegistry getApproverRegistry() {
        return approverRegistry;
    }

    public String getIssuerAuthorityId() {
        return issuerAuthorityId;
    }

    @Override
    public ActionApprovalGrant issueApproval(ActionProposal proposal, String approverId, String reason) {
        Objects.requireNonNull(proposal, "ActionProposal must not be null");
        Objects.requireNonNull(approverId, "approverId must not be null");

        // Validate approver authority
        if (!approverRegistry.isAuthorizedApprover(approverId, proposal.actionType())) {
            throw new SecurityException(String.format(
                    "Security Guardrail: Approver [%s] is not registered or authorized to approve action type [%s]",
                    approverId, proposal.actionType().code()));
        }

        String proposalFingerprint = ParameterCanonicalizer.canonicalize(proposal.parameters()) + "|" +
                proposal.targetResource() + "|" + proposal.actionType().code();

        ApprovalProvenance provenance = ApprovalProvenance.of(approverId, issuerAuthorityId, proposalFingerprint);
        String grantId = "GRANT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        return ActionApprovalGrant.createInternal(
                grantId,
                proposal.actionId(),
                proposalFingerprint,
                approverId,
                reason != null ? reason : "Operational supervisor approval granted",
                proposal.targetResource(),
                provenance,
                Instant.now(),
                false
        );
    }
}
