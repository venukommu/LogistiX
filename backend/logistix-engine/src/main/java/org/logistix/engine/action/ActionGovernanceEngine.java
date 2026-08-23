package org.logistix.engine.action;

import org.logistix.domain.action.ActionApprovalGrant;
import org.logistix.domain.action.ActionDecision;
import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionTelemetry;
import org.logistix.domain.ports.ActionExecutor;

/**
 * Deterministic Governance Engine governing enterprise action authorization and execution.
 * Ensures AI and external decision producers can NEVER execute actions without explicit LogistiX authorization.
 */
public interface ActionGovernanceEngine {

    /**
     * Evaluates an ActionProposal against standard operational policies and constraints.
     */
    ActionDecision evaluate(ActionProposal proposal);

    /**
     * Evaluates an ActionProposal against a specific ActionPolicy and constraints.
     */
    ActionDecision evaluate(ActionProposal proposal, ActionPolicy policy);

    /**
     * Revalidates an APPROVAL_REQUIRED proposal with an authorized supervisor approval grant.
     * Issues a fresh, deterministic AuthorizedAction upon successful revalidation.
     */
    ActionDecision revalidateAndAuthorize(ActionProposal proposal, ActionApprovalGrant grant, ActionPolicy policy);

    /**
     * Evaluates and, ONLY IF APPROVED, executes the authorized action via the provided ActionExecutor.
     */
    ActionResult executeIfAuthorized(ActionProposal proposal, ActionPolicy policy, ActionExecutor executor);

    /**
     * Retrieves the latest ActionTelemetry from the last evaluation/execution.
     */
    ActionTelemetry getLastTelemetry();
}
