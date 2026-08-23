package org.logistix.examples.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.logistix.domain.action.ActionApprovalGrant;
import org.logistix.domain.action.ActionApprovalGrantTestFactory;
import org.logistix.domain.action.ActionApprovalIssuer;
import org.logistix.domain.action.ActionAuthorizationIssuer;
import org.logistix.domain.action.ActionDecision;
import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionProposalSource;
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionStatus;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.AuthorizationProvenance;
import org.logistix.domain.action.AuthorizationAuthorityRegistry;
import org.logistix.domain.action.AuthorizedAction;
import org.logistix.domain.action.AuthorizedActionTestFactory;
import org.logistix.domain.action.DefaultActionApprovalIssuer;
import org.logistix.domain.action.DefaultActionAuthorizationIssuer;
import org.logistix.domain.action.TrustedApproverRegistry;
import org.logistix.domain.events.DomainEvent;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 10.2.1: Complete Security Test Matrix for Governed AI Actions with Closed Issuance.
 */
public class GovernedActionSecurityTest {

    private ActionAuthorizationIssuer authorizationIssuer;
    private ActionApprovalIssuer approvalIssuer;
    private DefaultActionGovernanceEngine governanceEngine;
    private MockMcpToolServer toolServer;
    private McpActionExecutor mcpExecutor;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        authorizationIssuer = new DefaultActionAuthorizationIssuer("LogistiX-Governance-Authority");
        approvalIssuer = new DefaultActionApprovalIssuer(TrustedApproverRegistry.withStandardLogisticsApprovers());
        governanceEngine = new DefaultActionGovernanceEngine(
                ActionPolicy.standardOperationalPolicy(),
                new InMemoryActionAuditStore(),
                authorizationIssuer,
                fixedClock,
                Duration.ofMinutes(5)
        );
        toolServer = new MockMcpToolServer();
        mcpExecutor = new McpActionExecutor(
                ToolRegistry.withStandardLogisticsTools(),
                toolServer,
                AuthorizationAuthorityRegistry.withStandardAuthorities(),
                fixedClock
        );
    }

    @Nested
    @DisplayName("1. Three Mandatory Demonstrations (Scenarios A, B, C)")
    class ThreeMandatoryDemonstrations {

        @Test
        @DisplayName("SCENARIO A (Approved Action): AI proposes valid appointment reschedule -> APPROVED -> MCP invoked (1 call)")
        void testScenarioAApprovedAction() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("SCENARIO-A-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-DEN-SFO")
                    .parameter("shipmentId", "SHIP-DEN-SFO")
                    .parameter("newAppointmentTime", "2026-08-24T15:00:00Z")
                    .reason("Severe Donner Pass blizzard delay advisory; rescheduling delivery window to avoid carrier detention")
                    .source(ActionProposalSource.AI)
                    .riskLevel("LOW")
                    .confidence(0.96)
                    .correlationId("CORR-SCENARIO-A")
                    .build();

            ActionResult result = governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.status()).isEqualTo(ActionStatus.EXECUTED);
            assertThat(toolServer.getInvocationCount()).isEqualTo(1);
            assertThat(toolServer.getInvocationCount("changeDeliveryAppointment")).isEqualTo(1);
        }

        @Test
        @DisplayName("SCENARIO B (Rejected Action): AI proposes shipment cancellation -> REJECTED -> 0 MCP calls")
        void testScenarioBRejectedAction() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("SCENARIO-B-001")
                    .actionType(ActionType.CANCEL_SHIPMENT)
                    .targetResource("SHIP-DEN-SFO")
                    .parameter("shipmentId", "SHIP-DEN-SFO")
                    .reason("AI unilaterally decided to cancel shipment due to severe storm")
                    .source(ActionProposalSource.AI)
                    .riskLevel("LOW")
                    .confidence(0.99)
                    .correlationId("CORR-SCENARIO-B")
                    .build();

            ActionResult result = governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor);

            assertThat(result.isSuccess()).isFalse();
            assertThat(toolServer.getInvocationCount()).isEqualTo(0);

            ActionDecision decision = governanceEngine.evaluate(proposal);
            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.violatedConstraints()).anyMatch(c -> c.contains("not permitted"));
        }

        @Test
        @DisplayName("SCENARIO C (Human Approval Required): AI proposes high-risk emergency action -> APPROVAL_REQUIRED -> 0 MCP calls")
        void testScenarioCHumanApprovalRequired() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("SCENARIO-C-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-CRITICAL-PHARMA")
                    .parameter("shipmentId", "SHIP-CRITICAL-PHARMA")
                    .parameter("newAppointmentTime", "2026-08-26T18:00:00Z")
                    .reason("Cold-chain pharmaceutical delivery window change exceeding standard SLA")
                    .source(ActionProposalSource.AI)
                    .riskLevel("HIGH")
                    .confidence(0.92)
                    .correlationId("CORR-SCENARIO-C")
                    .build();

            ActionResult result = governanceEngine.executeIfAuthorized(proposal, ActionPolicy.standardOperationalPolicy(), mcpExecutor);

            assertThat(result.isSuccess()).isFalse();
            assertThat(toolServer.getInvocationCount()).isEqualTo(0);

            ActionDecision decision = governanceEngine.evaluate(proposal);
            assertThat(decision.isApprovalRequired()).isTrue();
            assertThat(decision.requiredApprovals()).anyMatch(a -> a.contains("human operational supervisor"));
        }
    }

    @Nested
    @DisplayName("2. Security Test Matrix (Tests 1 through 13)")
    class SecurityMatrixTests {

        @Test
        @DisplayName("TEST 1 & 2: Forged authorization and token prefix attack (AUTH-fake) is rejected by executor")
        void testForgedTokenRejectedByMcpAdapter() {
            AuthorizationProvenance fakeProv = new AuthorizationProvenance("Unregistered-Attacker", "ISSUE-FAKE", "PROV-LGX-FAKE00000000", "ACTION_EXECUTION", fixedClock.instant());
            AuthorizedAction forged = AuthorizedActionTestFactory.forgedAction(
                    "FORGED-001",
                    ActionType.CHANGE_DELIVERY_APPOINTMENT,
                    "SHIP-9999",
                    Map.of("shipmentId", "SHIP-9999", "newAppointmentTime", "2026-08-25T10:00:00Z"),
                    "AUTH-fake-token", // Invalid prefix (doesn't start with AUTH-LGX-)
                    "fingerprint",
                    fakeProv,
                    "MaliciousActor",
                    "NONE",
                    "CORR-FORGED",
                    "IDEMP-FORGED",
                    fixedClock.instant(),
                    fixedClock.instant().plus(Duration.ofMinutes(5))
            );

            ActionResult result = mcpExecutor.execute(forged);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorDetails()).contains("Security Violation");
            assertThat(toolServer.getInvocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("TEST 3: Target Resource Mutation Attack is detected by SHA-256 fingerprint check and rejected")
        void testTargetResourceMutationDetectedByFingerprint() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-AUTH-TARGET-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-AUTHORIZED-TARGET")
                    .parameter("shipmentId", "SHIP-AUTHORIZED-TARGET")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            ActionDecision decision = governanceEngine.evaluate(proposal);
            AuthorizedAction validAuth = decision.authorizedAction().get();

            AuthorizedAction swappedTargetAuth = AuthorizedActionTestFactory.forgedAction(
                    validAuth.actionId(),
                    validAuth.actionType(),
                    "SHIP-UNAUTHORIZED-VICTIM", // Tampered target
                    validAuth.parameters(),
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

            ActionResult result = mcpExecutor.execute(swappedTargetAuth);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("TAMPERED");
            assertThat(toolServer.getInvocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("TEST 7: Modifying proposal parameters after supervisor approval invalidates approval grant")
        void testModifiedActionInvalidatesApproval() {
            ActionProposal originalProposal = ActionProposal.builder()
                    .actionId("ACT-HIGH-RISK-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-ORIGINAL")
                    .parameter("shipmentId", "SHIP-ORIGINAL")
                    .parameter("newAppointmentTime", "2026-08-25T12:00:00Z")
                    .riskLevel("HIGH")
                    .confidence(0.90)
                    .build();

            ActionApprovalGrant grant = approvalIssuer.issueApproval(
                    originalProposal,
                    "Manager-Alice",
                    "Approved for SHIP-ORIGINAL"
            );

            // Attacker changes parameters after approval grant was issued
            ActionProposal tamperedProposal = ActionProposal.builder()
                    .actionId("ACT-HIGH-RISK-001")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-ORIGINAL")
                    .parameter("shipmentId", "SHIP-ORIGINAL")
                    .parameter("newAppointmentTime", "2026-08-25T23:59:59Z") // Modified time!
                    .riskLevel("HIGH")
                    .confidence(0.90)
                    .build();

            ActionDecision revalDecision = governanceEngine.revalidateAndAuthorize(
                    tamperedProposal,
                    grant,
                    ActionPolicy.standardOperationalPolicy()
            );

            assertThat(revalDecision.isRejected()).isTrue();
            assertThat(revalDecision.reason()).contains("tampering or mismatch");
            assertThat(revalDecision.authorizedAction()).isEmpty();
        }

        @Test
        @DisplayName("TEST 11 & 12: AI proposes arbitrary rogue action code or endpoint -> deterministically REJECTED")
        void testUnknownActionCodeRejected() {
            ActionType rogueActionType = ActionType.of("EXECUTE_ARBITRARY_REMOTE_SHELL", "Rogue command");
            ActionProposal rogueProposal = ActionProposal.builder()
                    .actionId("ROGUE-001")
                    .actionType(rogueActionType)
                    .targetResource("SERVER-001")
                    .parameter("customEndpointUrl", "https://malicious-mcp.attacker.com")
                    .riskLevel("LOW")
                    .confidence(0.99)
                    .build();

            ActionDecision decision = governanceEngine.evaluate(rogueProposal);
            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.violatedConstraints()).anyMatch(v -> v.contains("not permitted"));
        }
    }
}
