package org.logistix.domain.action;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain record establishing the issuance provenance for an ActionApprovalGrant.
 * Proves that an approval was granted by a registered, trusted approval authority for a specific proposal.
 */
public record ApprovalProvenance(
        String approverId,
        String approvalId,
        String issuerAuthorityId,
        String proposalFingerprint,
        String scope,
        Instant issuedAt
) {
    public ApprovalProvenance {
        Objects.requireNonNull(approverId, "approverId must not be null");
        Objects.requireNonNull(approvalId, "approvalId must not be null");
        Objects.requireNonNull(issuerAuthorityId, "issuerAuthorityId must not be null");
        Objects.requireNonNull(proposalFingerprint, "proposalFingerprint must not be null");
        scope = scope != null ? scope : "OPERATIONAL_APPROVAL";
        issuedAt = issuedAt != null ? issuedAt : Instant.now();
    }

    public static ApprovalProvenance of(String approverId, String issuerAuthorityId, String proposalFingerprint) {
        String approvalId = "APPRV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return new ApprovalProvenance(
                approverId,
                approvalId,
                issuerAuthorityId != null ? issuerAuthorityId : "LogistiX-Approval-Authority",
                proposalFingerprint,
                "OPERATIONAL_APPROVAL",
                Instant.now()
        );
    }

    public boolean isValid() {
        return approverId != null && !approverId.isBlank() &&
                approvalId != null && approvalId.startsWith("APPRV-") &&
                issuerAuthorityId != null && !issuerAuthorityId.isBlank() &&
                proposalFingerprint != null && !proposalFingerprint.isBlank();
    }
}
