package org.logistix.domain.action;

/**
 * Domain port for issuing authentic, single-use ActionApprovalGrant instances.
 * Verifies that approval is issued strictly by an authorized human supervisor authority.
 */
public interface ActionApprovalIssuer {

    /**
     * Evaluates approver credentials and issues a bound, single-use ActionApprovalGrant for an APPROVAL_REQUIRED proposal.
     *
     * @param proposal the proposed action requiring approval
     * @param approverId the identifier of the human approver
     * @param reason operational rationale for granting approval
     * @return authentic ActionApprovalGrant
     * @throws SecurityException if the approverId is not recognized or authorized
     */
    ActionApprovalGrant issueApproval(ActionProposal proposal, String approverId, String reason);
}
