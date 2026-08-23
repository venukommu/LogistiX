package org.logistix.domain.ports;

import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.AuthorizedAction;

/**
 * Technology-neutral outbound execution port for invoking an AuthorizedAction against an external enterprise system.
 * The implementation must ONLY accept pre-authorized actions.
 */
public interface ActionExecutor {

    /**
     * Executes an AuthorizedAction against the target enterprise tool or system.
     *
     * @param action The validated, authorized action containing valid authorization tokens
     * @return Strongly typed ActionResult with execution status, operation ID, and latency
     */
    ActionResult execute(AuthorizedAction action);

    /**
     * Human-readable identifier for the executor implementation.
     */
    default String getExecutorType() {
        return getClass().getSimpleName();
    }
}
