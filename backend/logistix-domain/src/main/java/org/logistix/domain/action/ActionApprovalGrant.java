package org.logistix.domain.action;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity capturing a human operational supervisor approval grant for an APPROVAL_REQUIRED action.
 * Binds the grant to the exact action ID and expected parameters/target.
 */
public record ActionApprovalGrant(
        String grantId,
        String actionId,
        String approvedBy,
        String reason,
        String expectedTargetResource,
        Instant grantedAt
) {
    public ActionApprovalGrant {
        grantId = (grantId != null && !grantId.isBlank()) ? grantId : "GRANT-" + UUID.randomUUID().toString().substring(0, 8);
        Objects.requireNonNull(actionId, "actionId must not be null");
        approvedBy = approvedBy != null ? approvedBy : "supervisor";
        reason = reason != null ? reason : "Operational supervisor approval granted";
        grantedAt = grantedAt != null ? grantedAt : Instant.now();
    }

    public static ActionApprovalGrant of(String actionId, String approvedBy, String reason, String expectedTargetResource) {
        return new ActionApprovalGrant(null, actionId, approvedBy, reason, expectedTargetResource, Instant.now());
    }
}
