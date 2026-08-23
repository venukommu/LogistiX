package org.logistix.examples.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.logistix.domain.action.ActionApprovalGrant;
import org.logistix.domain.action.ActionApprovalIssuer;
import org.logistix.domain.action.ActionAuthorizationIssuer;
import org.logistix.domain.action.ActionDecision;
import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionProposalSource;
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionStatus;
import org.logistix.domain.action.ActionTelemetry;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.AuthorizedAction;
import org.logistix.domain.action.AuthorizedActionTestFactory;
import org.logistix.domain.action.DefaultActionApprovalIssuer;
import org.logistix.domain.action.DefaultActionAuthorizationIssuer;
import org.logistix.domain.action.TrustedApproverRegistry;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10.2.1: Hardened Action Governance Test Suite with Trusted Issuers, Provenance, and Concurrency Checks.
 */
public class ActionGovernanceTest {

    private InMemoryActionAuditStore auditStore;
    private ActionAuthorizationIssuer authorizationIssuer;
    private ActionApprovalIssuer approvalIssuer;
    private DefaultActionGovernanceEngine governanceEngine;
    private MockMcpToolServer mockToolServer;
    private McpActionExecutor mcpExecutor;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        auditStore = new InMemoryActionAuditStore();
        authorizationIssuer = new DefaultActionAuthorizationIssuer("LogistiX-Governance-Authority");
        approvalIssuer = new DefaultActionApprovalIssuer(TrustedApproverRegistry.withStandardLogisticsApprovers());
        governanceEngine = new DefaultActionGovernanceEngine(
                ActionPolicy.standardOperationalPolicy(),
                auditStore,
                authorizationIssuer,
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
        @DisplayName("1. Low-risk valid proposal is APPROVED and executes successfully via MCP")
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
        @DisplayName("2. Unpermitted action type is REJECTED and produces 0 MCP calls")
        void testRejectedActionNeverExecutes() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-002")
                    .actionType(ActionType.CANCEL_SHIPMENT)
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
        @DisplayName("3. High-risk action produces APPROVAL_REQUIRED and produces 0 MCP calls without human grant")
        void testApprovalRequiredActionNeverExecutesWithoutApproval() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-003")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-9903")
                    .parameter("shipmentId", "SHIP-9903")
                    .parameter("newAppointmentTime", "2026-08-25T14:00:00Z")
                    .riskLevel("HIGH")
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
        @DisplayName("4. Approval Grant lifecycle successfully revalidates and authorizes action with exact proposal binding")
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

            ActionApprovalGrant grant = approvalIssuer.issueApproval(
                    highRiskProposal,
                    "Supervisor-Jane",
                    "Approved critical route reschedule due to highway closure"
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
        @DisplayName("5. Approval grant reuse is rejected (single-use consumption enforcement)")
        void testApprovalGrantReuseRejected() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-GRANT-REUSE")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-REUSE")
                    .parameter("shipmentId", "SHIP-REUSE")
                    .parameter("newAppointmentTime", "2026-08-25T16:00:00Z")
                    .riskLevel("HIGH")
                    .confidence(0.92)
                    .build();

            ActionApprovalGrant grant = approvalIssuer.issueApproval(
                    proposal,
                    "Supervisor-Bob",
                    "One-time emergency approval"
            );

            // First revalidation succeeds and consumes the grant
            ActionDecision dec1 = governanceEngine.revalidateAndAuthorize(proposal, grant, ActionPolicy.standardOperationalPolicy());
            assertThat(dec1.isApproved()).isTrue();

            // Second revalidation attempt using same grant is rejected
            ActionDecision dec2 = governanceEngine.revalidateAndAuthorize(proposal, grant, ActionPolicy.standardOperationalPolicy());
            assertThat(dec2.isRejected()).isTrue();
            assertThat(dec2.reason()).contains("already consumed");
        }
    }

    @Nested
    @DisplayName("2. Hardened Boundary, Expiration & Concurrency Tests")
    class HardenedBoundaryTests {

        @Test
        @DisplayName("6. Expiration boundary: now >= expiresAt is strictly treated as EXPIRED")
        void testExactBoundaryExpiration() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-EXP-BOUNDARY")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-EXP-01")
                    .parameter("shipmentId", "SHIP-EXP-01")
                    .parameter("newAppointmentTime", "2026-08-24T12:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            ActionDecision decision = governanceEngine.evaluate(proposal);
            AuthorizedAction auth = decision.authorizedAction().get();
            Instant exactExpiresAt = auth.expiresAt();

            // 1. Before expiration (expiresAt - 1 second) -> Valid
            assertThat(auth.isExpired(exactExpiresAt.minusSeconds(1))).isFalse();

            // 2. Exactly at expiration instant (now == expiresAt) -> EXPIRED
            assertThat(auth.isExpired(exactExpiresAt)).isTrue();

            // 3. Past expiration (expiresAt + 1 second) -> EXPIRED
            assertThat(auth.isExpired(exactExpiresAt.plusSeconds(1))).isTrue();
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
            AuthorizedAction tamperedAuth = AuthorizedActionTestFactory.forgedAction(
                    validAuth.actionId(),
                    validAuth.actionType(),
                    validAuth.targetResource(),
                    Map.of("shipmentId", "SHIP-TAMP-01", "newAppointmentTime", "2026-08-24T23:59:59Z"),
                    validAuth.authorizationToken(),
                    validAuth.authorizationFingerprint(),
                    validAuth.provenance(),
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
        @DisplayName("8. Atomic Idempotency: Multi-threaded concurrent requests execute MCP exactly ONCE")
        void testConcurrentAtomicIdempotency() throws Exception {
            int threadCount = 8;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-CONCURRENT-001")
                    .idempotencyKey("CONCURRENT-IDEMP-KEY-888")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-CONCURRENT")
                    .parameter("shipmentId", "SHIP-CONCURRENT")
                    .parameter("newAppointmentTime", "2026-08-24T10:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            List<Callable<ActionResult>> tasks = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                tasks.add(() -> governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor));
            }

            List<Future<ActionResult>> futures = executorService.invokeAll(tasks);
            executorService.shutdown();

            for (Future<ActionResult> future : futures) {
                ActionResult res = future.get();
                assertThat(res.isSuccess()).isTrue();
            }

            // Invariant: Exactly one MCP call executed despite 8 concurrent submissions
            assertThat(mockToolServer.getInvocationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("9. Deep immutability: Nested collections in AuthorizedAction cannot be modified")
        void testDeepImmutability() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-IMMUTABLE-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-IMMUTABLE")
                    .parameter("shipmentId", "SHIP-IMMUTABLE")
                    .parameter("newAppointmentTime", "2026-08-24T10:00:00Z")
                    .parameter("nestedOptions", Map.of("key1", "val1"))
                    .build();

            AuthorizedAction action = authorizationIssuer.issue(
                    proposal,
                    "POLICY",
                    "Governance",
                    Duration.ofMinutes(5),
                    fixedClock.instant()
            );

            assertThatThrownBy(() -> action.parameters().put("illegalKey", "illegalVal"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
