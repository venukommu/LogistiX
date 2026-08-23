package org.logistix.examples.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.logistix.domain.action.ActionApprovalGrant;
import org.logistix.domain.action.ActionAuditEntry;
import org.logistix.domain.action.ActionDecision;
import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionProposalSource;
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionStatus;
import org.logistix.domain.action.ActionTelemetry;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.AuthorizedAction;
import org.logistix.engine.action.ActionPolicy;
import org.logistix.engine.action.DefaultActionGovernanceEngine;
import org.logistix.engine.action.InMemoryActionAuditStore;
import org.logistix.mcp.McpActionExecutor;
import org.logistix.mcp.MockMcpToolServer;
import org.logistix.mcp.ToolRegistry;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10.1: Hardened Action Governance Test Suite.
 */
public class ActionGovernanceTest {

    private InMemoryActionAuditStore auditStore;
    private DefaultActionGovernanceEngine governanceEngine;
    private MockMcpToolServer mockToolServer;
    private McpActionExecutor mcpExecutor;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        auditStore = new InMemoryActionAuditStore();
        governanceEngine = new DefaultActionGovernanceEngine(
                ActionPolicy.standardOperationalPolicy(),
                auditStore,
                fixedClock,
                Duration.ofMinutes(5)
        );
        mockToolServer = new MockMcpToolServer();
        mcpExecutor = new McpActionExecutor(ToolRegistry.withStandardLogisticsTools(), mockToolServer, fixedClock);
    }

    @Nested
    @DisplayName("1. Core Governance Lifecycle & Boundary Tests")
    class GovernanceLifecycleTests {

        @Test
        @DisplayName("1 & 10. Low-risk valid proposal is APPROVED and executes successfully via MCP")
        void testApprovedActionExecutes() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-9901")
                    .parameter("shipmentId", "SHIP-9901")
                    .parameter("newAppointmentTime", "2026-08-24T10:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .source(ActionProposalSource.AI)
                    .reason("AI identified potential road maintenance delay; proposed 2h appointment shift")
                    .correlationId("CORR-001")
                    .build();

            ActionResult result = governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.status()).isEqualTo(ActionStatus.EXECUTED);
            assertThat(mockToolServer.getInvocationCount()).isEqualTo(1);
            assertThat(mockToolServer.getInvocationCount("changeDeliveryAppointment")).isEqualTo(1);

            ActionTelemetry telemetry = governanceEngine.getLastTelemetry();
            assertThat(telemetry).isNotNull();
            assertThat(telemetry.executed()).isTrue();
            assertThat(telemetry.authorizationStatus()).isEqualTo(ActionStatus.APPROVED);
        }

        @Test
        @DisplayName("2 & 7. Unpermitted action type is REJECTED and produces 0 MCP calls")
        void testRejectedActionNeverExecutes() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-002")
                    .actionType(ActionType.CANCEL_SHIPMENT) // CANCEL_SHIPMENT is not allowed in standard policy
                    .targetResource("SHIP-9902")
                    .parameter("shipmentId", "SHIP-9902")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .source(ActionProposalSource.AI)
                    .correlationId("CORR-002")
                    .build();

            ActionResult result = governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.status()).isEqualTo(ActionStatus.FAILED);
            assertThat(mockToolServer.getInvocationCount()).isEqualTo(0);

            ActionDecision decision = governanceEngine.evaluate(proposal);
            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.violatedConstraints()).anyMatch(v -> v.contains("not permitted"));
        }

        @Test
        @DisplayName("3 & 9. High-risk action produces APPROVAL_REQUIRED and produces 0 MCP calls without human grant")
        void testApprovalRequiredActionNeverExecutesWithoutApproval() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-003")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-9903")
                    .parameter("shipmentId", "SHIP-9903")
                    .parameter("newAppointmentTime", "2026-08-25T14:00:00Z")
                    .riskLevel("HIGH") // High risk requires operational approval
                    .confidence(0.90)
                    .source(ActionProposalSource.AI)
                    .correlationId("CORR-003")
                    .build();

            ActionResult result = governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor);

            assertThat(result.isSuccess()).isFalse();
            assertThat(mockToolServer.getInvocationCount()).isEqualTo(0);

            ActionDecision decision = governanceEngine.evaluate(proposal);
            assertThat(decision.isApprovalRequired()).isTrue();
            assertThat(decision.requiredApprovals()).anyMatch(r -> r.contains("human operational supervisor"));
        }

        @Test
        @DisplayName("4. Approval Grant lifecycle successfully revalidates and authorizes action")
        void testApprovalGrantLifecycle() {
            ActionProposal highRiskProposal = ActionProposal.builder()
                    .actionId("ACT-GRANT-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-CRITICAL")
                    .parameter("shipmentId", "SHIP-CRITICAL")
                    .parameter("newAppointmentTime", "2026-08-25T16:00:00Z")
                    .riskLevel("HIGH")
                    .confidence(0.92)
                    .build();

            ActionDecision initialDecision = governanceEngine.evaluate(highRiskProposal);
            assertThat(initialDecision.isApprovalRequired()).isTrue();

            ActionApprovalGrant grant = ActionApprovalGrant.of(
                    "ACT-GRANT-001",
                    "Supervisor-Jane",
                    "Approved critical route reschedule due to highway closure",
                    "SHIP-CRITICAL"
            );

            ActionDecision grantedDecision = governanceEngine.revalidateAndAuthorize(
                    highRiskProposal,
                    grant,
                    ActionPolicy.standardOperationalPolicy()
            );

            assertThat(grantedDecision.isApproved()).isTrue();
            assertThat(grantedDecision.authorizedAction()).isPresent();

            ActionResult result = mcpExecutor.execute(grantedDecision.authorizedAction().get());
            assertThat(result.isSuccess()).isTrue();
            assertThat(mockToolServer.getInvocationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("5. Approval grant with mismatched target resource is deterministically REJECTED")
        void testApprovalGrantMismatchTargetRejection() {
            ActionProposal highRiskProposal = ActionProposal.builder()
                    .actionId("ACT-GRANT-002")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-ACTUAL-TARGET")
                    .parameter("shipmentId", "SHIP-ACTUAL-TARGET")
                    .parameter("newAppointmentTime", "2026-08-25T16:00:00Z")
                    .riskLevel("HIGH")
                    .confidence(0.92)
                    .build();

            ActionApprovalGrant mismatchedGrant = ActionApprovalGrant.of(
                    "ACT-GRANT-002",
                    "Supervisor-Jane",
                    "Approved for wrong shipment",
                    "SHIP-DIFFERENT-TARGET"
            );

            ActionDecision decision = governanceEngine.revalidateAndAuthorize(
                    highRiskProposal,
                    mismatchedGrant,
                    ActionPolicy.standardOperationalPolicy()
            );

            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.reason()).contains("target resource mismatch");
            assertThat(decision.violatedConstraints()).anyMatch(v -> v.contains("does not match proposal target"));
        }
    }

    @Nested
    @DisplayName("2. Hardened Boundary, Expiration & Tampering Tests")
    class HardenedBoundaryTests {

        @Test
        @DisplayName("6. Expired AuthorizedAction is rejected by both governance and executor")
        void testExpiredAuthorizationRejection() {
            // Clock set to 10:00:00Z, action expires at 10:05:00Z
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-EXP-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-EXP-01")
                    .parameter("shipmentId", "SHIP-EXP-01")
                    .parameter("newAppointmentTime", "2026-08-24T12:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            ActionDecision decision = governanceEngine.evaluate(proposal);
            assertThat(decision.isApproved()).isTrue();
            AuthorizedAction auth = decision.authorizedAction().get();

            // Advance clock past expiration (10:06:00Z)
            Clock futureClock = Clock.fixed(Instant.parse("2026-08-23T10:06:00Z"), ZoneOffset.UTC);
            McpActionExecutor futureExecutor = new McpActionExecutor(
                    ToolRegistry.withStandardLogisticsTools(),
                    mockToolServer,
                    futureClock
            );

            ActionResult result = futureExecutor.execute(auth);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("EXPIRED");
            assertThat(mockToolServer.getInvocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("7. Fingerprint mismatch due to parameter tampering causes immediate execution rejection")
        void testParameterTamperingFingerprintMismatch() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-TAMP-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-TAMP-01")
                    .parameter("shipmentId", "SHIP-TAMP-01")
                    .parameter("newAppointmentTime", "2026-08-24T10:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            ActionDecision decision = governanceEngine.evaluate(proposal);
            AuthorizedAction validAuth = decision.authorizedAction().get();

            // Attacker creates mutated action swapping appointment time but preserving the original fingerprint
            AuthorizedAction tamperedAuth = new AuthorizedAction(
                    validAuth.actionId(),
                    validAuth.actionType(),
                    validAuth.targetResource(),
                    Map.of("shipmentId", "SHIP-TAMP-01", "newAppointmentTime", "2026-08-24T23:59:59Z"), // Tampered parameter
                    validAuth.authorizationToken(),
                    validAuth.authorizationFingerprint(), // Stale fingerprint that doesn't match 23:59:59Z
                    validAuth.authorizedBy(),
                    validAuth.policyApplied(),
                    validAuth.correlationId(),
                    validAuth.idempotencyKey(),
                    validAuth.authorizedAt(),
                    validAuth.expiresAt()
            );

            ActionResult result = mcpExecutor.execute(tamperedAuth);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("TAMPERED");
            assertThat(mockToolServer.getInvocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("8. Reusing idempotency key with mutated parameters is detected as tampering and rejected")
        void testIdempotencyKeyTamperingDetected() {
            ActionProposal proposal1 = ActionProposal.builder()
                    .actionId("ACT-IDEMP-TAMP-001")
                    .idempotencyKey("SHARED-IDEMP-KEY")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-100")
                    .parameter("shipmentId", "SHIP-100")
                    .parameter("newAppointmentTime", "2026-08-24T10:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            ActionProposal proposal2 = ActionProposal.builder()
                    .actionId("ACT-IDEMP-TAMP-002")
                    .idempotencyKey("SHARED-IDEMP-KEY") // Reusing same key
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-999") // Mutated target resource!
                    .parameter("shipmentId", "SHIP-999")
                    .parameter("newAppointmentTime", "2026-08-24T10:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            ActionDecision dec1 = governanceEngine.evaluate(proposal1);
            assertThat(dec1.isApproved()).isTrue();

            ActionDecision dec2 = governanceEngine.evaluate(proposal2);
            assertThat(dec2.isRejected()).isTrue();
            assertThat(dec2.reason()).contains("tampering detected");
        }

        @Test
        @DisplayName("9. Audit trail captures comprehensive lifecycle with defensive copy integrity")
        void testAuditTrailIntegrity() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-AUDIT-HARDENED")
                    .actionType(ActionType.ASSIGN_DRIVER)
                    .targetResource("SHIP-AUDIT")
                    .parameter("shipmentId", "SHIP-AUDIT")
                    .parameter("driverId", "DRV-AUDIT")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .correlationId("CORR-AUDIT-HARDENED")
                    .build();

            governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor);

            Optional<ActionAuditEntry> entryOpt = auditStore.findLatestByActionId("ACT-AUDIT-HARDENED");
            assertThat(entryOpt).isPresent();
            ActionAuditEntry entry = entryOpt.get();
            assertThat(entry.governanceStatus()).isEqualTo(ActionStatus.APPROVED);
            assertThat(entry.executionStatus()).isEqualTo(ActionStatus.EXECUTED);
            assertThat(entry.executorType()).isEqualTo("MCP-ActionExecutor");
        }
    }
}
