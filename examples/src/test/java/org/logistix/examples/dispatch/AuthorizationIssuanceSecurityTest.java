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
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.ApprovalProvenance;
import org.logistix.domain.action.AuthorizationProvenance;
import org.logistix.domain.action.AuthorizedAction;
import org.logistix.domain.action.AuthorizedActionTestFactory;
import org.logistix.domain.action.DefaultActionApprovalIssuer;
import org.logistix.domain.action.DefaultActionAuthorizationIssuer;
import org.logistix.domain.action.TrustedApproverRegistry;
import org.logistix.engine.action.ActionPolicy;
import org.logistix.engine.action.DefaultActionGovernanceEngine;
import org.logistix.engine.action.InMemoryActionAuditStore;
import org.logistix.mcp.AuthorizationAuthorityRegistry;
import org.logistix.mcp.McpActionExecutor;
import org.logistix.mcp.MockMcpToolServer;
import org.logistix.mcp.ToolRegistry;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10.2.2 Architectural Security Test Suite: Trusted Registry Configuration, Freeze Lifecycle & Provenance.
 */
public class AuthorizationIssuanceSecurityTest {

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
    @DisplayName("1. Approval Issuance & Provenance Tests (Tests A through F)")
    class ApprovalIssuanceTests {

        @Test
        @DisplayName("TEST A — Self-Issued Approval: Unregistered caller claiming to be approver is rejected by ApprovalIssuer")
        void testSelfIssuedApprovalRejected() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-UNAUTH-APP")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-UNAUTH")
                    .parameter("shipmentId", "SHIP-UNAUTH")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("HIGH")
                    .build();

            // Unregistered malicious approver attempts to issue approval
            assertThatThrownBy(() -> approvalIssuer.issueApproval(proposal, "Malicious-Caller-999", "Self-approved"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("not registered or authorized");
        }

        @Test
        @DisplayName("TEST B — Forged Approval Provenance: Grant with forged/invalid provenance is rejected during revalidation")
        void testForgedApprovalProvenanceRejected() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-FORGED-APP")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-FORGED")
                    .parameter("shipmentId", "SHIP-FORGED")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("HIGH")
                    .build();

            ApprovalProvenance fakeProvenance = new ApprovalProvenance(
                    "Supervisor-Jane",
                    "INVALID-ID", // Missing APPRV- prefix
                    "Fake-Authority",
                    "fake-fingerprint",
                    "OPERATIONAL_APPROVAL",
                    fixedClock.instant()
            );

            ActionApprovalGrant forgedGrant = ActionApprovalGrantTestFactory.forgedGrant(
                    "GRANT-FORGED",
                    proposal.actionId(),
                    "fake-fingerprint",
                    "Supervisor-Jane",
                    "Forged approval",
                    proposal.targetResource(),
                    fakeProvenance,
                    fixedClock.instant(),
                    false
            );

            ActionDecision decision = governanceEngine.revalidateAndAuthorize(proposal, forgedGrant, ActionPolicy.standardOperationalPolicy());

            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.reason()).containsIgnoringCase("missing valid provenance");
            assertThat(decision.authorizedAction()).isEmpty();
        }

        @Test
        @DisplayName("TEST C — Valid Approval: Trusted approval authority creates valid grant which successfully revalidates")
        void testValidApprovalAccepted() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-VALID-APP")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-VALID")
                    .parameter("shipmentId", "SHIP-VALID")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("HIGH")
                    .confidence(0.95)
                    .build();

            ActionApprovalGrant grant = approvalIssuer.issueApproval(
                    proposal,
                    "SUPERVISOR-001",
                    "Operational route adjustment authorized"
            );

            assertThat(grant.provenance().isValid()).isTrue();

            ActionDecision decision = governanceEngine.revalidateAndAuthorize(proposal, grant, ActionPolicy.standardOperationalPolicy());

            assertThat(decision.isApproved()).isTrue();
            assertThat(decision.authorizedAction()).isPresent();
        }

        @Test
        @DisplayName("TEST D — Approval Action Tampering: Target resource changed after approval grant is rejected")
        void testApprovalTargetTamperingRejected() {
            ActionProposal originalProposal = ActionProposal.builder()
                    .actionId("ACT-TAMP-TARGET")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-TARGET-A")
                    .parameter("shipmentId", "SHIP-TARGET-A")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("HIGH")
                    .build();

            ActionApprovalGrant grant = approvalIssuer.issueApproval(originalProposal, "SUPERVISOR-001", "Approved for Target A");

            ActionProposal tamperedProposal = ActionProposal.builder()
                    .actionId("ACT-TAMP-TARGET")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-TARGET-B") // Swapped target!
                    .parameter("shipmentId", "SHIP-TARGET-B")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("HIGH")
                    .build();

            ActionDecision decision = governanceEngine.revalidateAndAuthorize(tamperedProposal, grant, ActionPolicy.standardOperationalPolicy());

            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.reason()).contains("tampering or mismatch");
        }

        @Test
        @DisplayName("TEST E — Approval Parameter Tampering: Parameters modified after approval grant are rejected")
        void testApprovalParameterTamperingRejected() {
            ActionProposal originalProposal = ActionProposal.builder()
                    .actionId("ACT-TAMP-PARAM")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-PARAM-A")
                    .parameter("shipmentId", "SHIP-PARAM-A")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("HIGH")
                    .build();

            ActionApprovalGrant grant = approvalIssuer.issueApproval(originalProposal, "SUPERVISOR-001", "Approved for 10:00");

            ActionProposal tamperedProposal = ActionProposal.builder()
                    .actionId("ACT-TAMP-PARAM")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-PARAM-A")
                    .parameter("shipmentId", "SHIP-PARAM-A")
                    .parameter("newAppointmentTime", "2026-08-25T18:00:00Z") // Modified time!
                    .riskLevel("HIGH")
                    .build();

            ActionDecision decision = governanceEngine.revalidateAndAuthorize(tamperedProposal, grant, ActionPolicy.standardOperationalPolicy());

            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.reason()).contains("tampering or mismatch");
        }

        @Test
        @DisplayName("TEST F — Approval Reuse: Consuming an approval grant a second time is strictly rejected")
        void testApprovalReuseRejected() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-REUSE-APP")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-REUSE")
                    .parameter("shipmentId", "SHIP-REUSE")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("HIGH")
                    .build();

            ActionApprovalGrant grant = approvalIssuer.issueApproval(proposal, "SUPERVISOR-001", "Single-use approval");

            ActionDecision firstUse = governanceEngine.revalidateAndAuthorize(proposal, grant, ActionPolicy.standardOperationalPolicy());
            assertThat(firstUse.isApproved()).isTrue();

            ActionDecision secondUse = governanceEngine.revalidateAndAuthorize(proposal, grant, ActionPolicy.standardOperationalPolicy());
            assertThat(secondUse.isRejected()).isTrue();
            assertThat(secondUse.reason()).contains("already consumed");
        }
    }

    @Nested
    @DisplayName("2. Authorization Issuance & Boundary Tests (Tests 1 through 5)")
    class AuthorizationIssuanceTests {

        @Test
        @DisplayName("TEST 1 & 2: Forged authorization provenance is rejected by McpActionExecutor")
        void testForgedAuthorizationProvenanceRejected() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-FORGED-AUTH")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-FORGED-AUTH")
                    .parameter("shipmentId", "SHIP-FORGED-AUTH")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .build();

            AuthorizationProvenance unverifiedProv = new AuthorizationProvenance(
                    "Unregistered-Authority-X",
                    "ISSUE-ROGUE",
                    "PROV-LGX-ROGUE000000",
                    "ACTION_EXECUTION",
                    fixedClock.instant()
            );

            AuthorizedAction forged = AuthorizedActionTestFactory.forgedAction(
                    proposal.actionId(),
                    proposal.actionType(),
                    proposal.targetResource(),
                    proposal.parameters(),
                    "AUTH-LGX-ROGUE",
                    "",
                    unverifiedProv,
                    "RogueActor",
                    "POLICY",
                    "CORR",
                    "IDEMP",
                    fixedClock.instant(),
                    fixedClock.instant().plus(Duration.ofMinutes(5))
            );

            ActionResult result = mcpExecutor.execute(forged);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("AUTH-PROVENANCE-ERR");
            assertThat(toolServer.getInvocationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("TEST 3: Token prefix attack (AUTH-fake) with matching fingerprint rejected by McpActionExecutor")
        void testTokenPrefixAttackRejected() {
            AuthorizationProvenance fakeProv = new AuthorizationProvenance(
                    "LogistiX-Governance-Authority",
                    "ISSUE-FAKE",
                    "PROV-LGX-FAKE000000",
                    "ACTION_EXECUTION",
                    fixedClock.instant()
            );

            AuthorizedAction forged = AuthorizedActionTestFactory.forgedAction(
                    "ACT-PREFIX-ATTACK",
                    ActionType.CHANGE_DELIVERY_APPOINTMENT,
                    "SHIP-PREFIX",
                    Map.of("shipmentId", "SHIP-PREFIX", "newAppointmentTime", "2026-08-25T10:00:00Z"),
                    "AUTH-fake-prefix-token", // Missing AUTH-LGX- prefix
                    "valid-fingerprint",
                    fakeProv,
                    "Attacker",
                    "POLICY",
                    "CORR",
                    "IDEMP",
                    fixedClock.instant(),
                    fixedClock.instant().plus(Duration.ofMinutes(5))
            );

            ActionResult result = mcpExecutor.execute(forged);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("AUTH-PROVENANCE-ERR");
        }

        @Test
        @DisplayName("TEST 4: Untrusted issuer authorization rejected by authority registry")
        void testUntrustedIssuerRejected() {
            AuthorizationProvenance untrustedProv = new AuthorizationProvenance(
                    "Untrusted-Shadow-Issuer",
                    "ISSUE-SHADOW",
                    "PROV-LGX-SHADOW00000",
                    "ACTION_EXECUTION",
                    fixedClock.instant()
            );

            AuthorizedAction action = AuthorizedActionTestFactory.forgedAction(
                    "ACT-SHADOW",
                    ActionType.CHANGE_DELIVERY_APPOINTMENT,
                    "SHIP-SHADOW",
                    Map.of("shipmentId", "SHIP-SHADOW", "newAppointmentTime", "2026-08-25T10:00:00Z"),
                    "AUTH-LGX-SHADOW",
                    "",
                    untrustedProv,
                    "ShadowAuthority",
                    "POLICY",
                    "CORR",
                    "IDEMP",
                    fixedClock.instant(),
                    fixedClock.instant().plus(Duration.ofMinutes(5))
            );

            ActionResult result = mcpExecutor.execute(action);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("AUTH-PROVENANCE-ERR");
            assertThat(result.message()).contains("untrusted");
        }

        @Test
        @DisplayName("TEST 5: Parameter tampering detected by canonical SHA-256 fingerprint check")
        void testParameterTamperingDetected() {
            ActionProposal proposal = ActionProposal.builder()
                    .actionId("ACT-AUTH-TAMP")
                    .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                    .targetResource("SHIP-TAMP")
                    .parameter("shipmentId", "SHIP-TAMP")
                    .parameter("newAppointmentTime", "2026-08-25T10:00:00Z")
                    .riskLevel("LOW")
                    .confidence(0.95)
                    .build();

            AuthorizedAction authentic = authorizationIssuer.issue(proposal, "POLICY", "LogistiX-Governance");

            // Mutate parameter while keeping original fingerprint
            AuthorizedAction tampered = AuthorizedActionTestFactory.forgedAction(
                    authentic.actionId(),
                    authentic.actionType(),
                    authentic.targetResource(),
                    Map.of("shipmentId", "SHIP-TAMP", "newAppointmentTime", "2026-08-25T23:59:59Z"),
                    authentic.authorizationToken(),
                    authentic.authorizationFingerprint(),
                    authentic.provenance(),
                    authentic.authorizedBy(),
                    authentic.policyApplied(),
                    authentic.correlationId(),
                    authentic.idempotencyKey(),
                    authentic.authorizedAt(),
                    authentic.expiresAt()
            );

            ActionResult result = mcpExecutor.execute(tampered);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("TAMPERED");
        }
    }

    @Nested
    @DisplayName("3. Registry Freeze Lifecycle & Configuration Validation Tests")
    class RegistryLifecycleAndValidationTests {

        @Test
        @DisplayName("TEST 6 — AuthorizationAuthorityRegistry freeze lifecycle rejects post-freeze mutation")
        void testAuthorityRegistryFreezeRejectsMutation() {
            AuthorizationAuthorityRegistry registry = AuthorizationAuthorityRegistry.empty();
            registry.registerAuthority("Authority-A");
            assertThat(registry.isFrozen()).isFalse();
            assertThat(registry.isRegisteredAuthority("Authority-A")).isTrue();

            registry.freeze();
            assertThat(registry.isFrozen()).isTrue();

            assertThatThrownBy(() -> registry.registerAuthority("Authority-B"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen and immutable");
        }

        @Test
        @DisplayName("TEST 7 — AuthorizationAuthorityRegistry rejects blank and duplicate authority IDs")
        void testAuthorityRegistryValidation() {
            AuthorizationAuthorityRegistry registry = AuthorizationAuthorityRegistry.empty();

            assertThatThrownBy(() -> registry.registerAuthority(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");

            assertThatThrownBy(() -> registry.registerAuthority(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");

            registry.registerAuthority("Auth-Unique-1");

            assertThatThrownBy(() -> registry.registerAuthority("Auth-Unique-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("TEST 8 — TrustedApproverRegistry freeze lifecycle rejects post-freeze mutation")
        void testApproverRegistryFreezeRejectsMutation() {
            TrustedApproverRegistry registry = TrustedApproverRegistry.empty();
            registry.registerApprover("Approver-1", Set.of(ActionType.CHANGE_DELIVERY_APPOINTMENT));
            assertThat(registry.isFrozen()).isFalse();
            assertThat(registry.isAuthorizedApprover("Approver-1")).isTrue();

            registry.freeze();
            assertThat(registry.isFrozen()).isTrue();

            assertThatThrownBy(() -> registry.registerApprover("Approver-2", Set.of(ActionType.ASSIGN_DRIVER)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen and immutable");
        }

        @Test
        @DisplayName("TEST 9 — TrustedApproverRegistry rejects blank, duplicate, and empty allowed actions")
        void testApproverRegistryValidation() {
            TrustedApproverRegistry registry = TrustedApproverRegistry.empty();

            assertThatThrownBy(() -> registry.registerApprover("", Set.of(ActionType.ASSIGN_DRIVER)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or blank");

            assertThatThrownBy(() -> registry.registerApprover("Approver-A", Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or empty");

            registry.registerApprover("Approver-A", Set.of(ActionType.ASSIGN_DRIVER));

            assertThatThrownBy(() -> registry.registerApprover("Approver-A", Set.of(ActionType.ASSIGN_DRIVER)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("TEST 10 — Runtime Trust Escalation Attack: Attacker cannot register rogue authority after freeze to pass execution")
        void testRuntimeTrustEscalationPrevented() {
            AuthorizationAuthorityRegistry registry = AuthorizationAuthorityRegistry.withStandardAuthorities();
            assertThat(registry.isFrozen()).isTrue();

            // Attacker attempts runtime trust escalation
            assertThatThrownBy(() -> registry.registerAuthority("Attacker-Injected-Authority"))
                    .isInstanceOf(IllegalStateException.class);

            // Attempting to execute action with unverified authority is rejected by McpActionExecutor
            McpActionExecutor customExecutor = new McpActionExecutor(
                    ToolRegistry.withStandardLogisticsTools(),
                    toolServer,
                    registry,
                    fixedClock
            );

            AuthorizationProvenance attackerProv = new AuthorizationProvenance(
                    "Attacker-Injected-Authority",
                    "ISSUE-ATTACK",
                    "PROV-LGX-ATTACK00000",
                    "ACTION_EXECUTION",
                    fixedClock.instant()
            );

            AuthorizedAction unverifiedAction = AuthorizedActionTestFactory.forgedAction(
                    "ACT-ESCALATE",
                    ActionType.CHANGE_DELIVERY_APPOINTMENT,
                    "SHIP-ESCALATE",
                    Map.of("shipmentId", "SHIP-ESCALATE", "newAppointmentTime", "2026-08-25T10:00:00Z"),
                    "AUTH-LGX-ESCALATE",
                    "",
                    attackerProv,
                    "Attacker",
                    "POLICY",
                    "CORR",
                    "IDEMP",
                    fixedClock.instant(),
                    fixedClock.instant().plus(Duration.ofMinutes(5))
            );

            ActionResult result = customExecutor.execute(unverifiedAction);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.operationId()).isEqualTo("AUTH-PROVENANCE-ERR");
        }
    }
}
