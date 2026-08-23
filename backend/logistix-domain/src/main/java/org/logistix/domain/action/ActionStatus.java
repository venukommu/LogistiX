package org.logistix.domain.action;

/**
 * Status of an Action in the LogistiX governance and execution lifecycle.
 */
public enum ActionStatus {
    PROPOSED,
    APPROVED,
    REJECTED,
    APPROVAL_REQUIRED,
    EXECUTED,
    FAILED
}
