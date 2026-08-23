package org.logistix.examples.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.domain.action.ActionAuthorizationIssuer;
import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionStatus;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.AuthorizationProvenance;
import org.logistix.domain.action.AuthorizedAction;
import org.logistix.domain.action.AuthorizedActionTestFactory;
import org.logistix.domain.action.DefaultActionAuthorizationIssuer;
import org.logistix.mcp.AuthorizationAuthorityRegistry;
import org.logistix.mcp.McpActionExecutor;
import org.logistix.mcp.McpToolDefinition;
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
 * Hardened Test Suite for Model Context Protocol (MCP) Adapter execution, schema validation, and provenance checks.
 */
public class McpAdapterTest {

    private ToolRegistry toolRegistry;
    private MockMcpToolServer toolServer;
    private AuthorizationAuthorityRegistry authorityRegistry;
    private ActionAuthorizationIssuer authorizationIssuer;
    private McpActionExecutor executor;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        toolRegistry = ToolRegistry.withStandardLogisticsTools();
        toolServer = new MockMcpToolServer();
        authorityRegistry = AuthorizationAuthorityRegistry.withStandardAuthorities();
        authorizationIssuer = new DefaultActionAuthorizationIssuer("LogistiX-Governance-Authority");
        executor = new McpActionExecutor(toolRegistry, toolServer, authorityRegistry, fixedClock);
    }

    @Test
    @DisplayName("1. AuthorizedAction translates cleanly to registered MCP tool invocation")
    void testAuthorizedActionExecution() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-001")
                .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                .targetResource("SHIP-5501")
                .parameter("shipmentId", "SHIP-5501")
                .parameter("newAppointmentTime", "2026-08-24T18:00:00Z")
                .correlationId("CORR-5501")
                .build();

        AuthorizedAction action = authorizationIssuer.issue(
                proposal,
                "STANDARD-POLICY",
                "LogistiX-Governance",
                Duration.ofMinutes(5),
                fixedClock.instant()
        );

        ActionResult result = executor.execute(action);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.status()).isEqualTo(ActionStatus.EXECUTED);
        assertThat(toolServer.getInvocationCount("changeDeliveryAppointment")).isEqualTo(1);
        assertThat(toolServer.getLastArguments("changeDeliveryAppointment")).containsEntry("shipmentId", "SHIP-5501");
    }

    @Test
    @DisplayName("2. Driver assignment action maps to assignDriver tool in Fleet Management")
    void testAssignDriverToolExecution() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-002")
                .actionType(ActionType.ASSIGN_DRIVER)
                .targetResource("SHIP-5502")
                .parameter("shipmentId", "SHIP-5502")
                .parameter("driverId", "DRV-ALEX-01")
                .correlationId("CORR-5502")
                .build();

        AuthorizedAction action = authorizationIssuer.issue(
                proposal,
                "STANDARD-POLICY",
                "LogistiX-Governance",
                Duration.ofMinutes(5),
                fixedClock.instant()
        );

        ActionResult result = executor.execute(action);

        assertThat(result.isSuccess()).isTrue();
        assertThat(toolServer.getInvocationCount("assignDriver")).isEqualTo(1);
        assertThat(toolServer.getLastArguments("assignDriver")).containsEntry("driverId", "DRV-ALEX-01");
    }

    @Test
    @DisplayName("3. Missing or invalid AuthorizationProvenance is rejected with AUTH-PROVENANCE-ERR")
    void testInvalidProvenanceRejected() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-PROV")
                .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                .targetResource("SHIP-5503")
                .parameter("shipmentId", "SHIP-5503")
                .parameter("newAppointmentTime", "2026-08-24T18:00:00Z")
                .build();

        AuthorizationProvenance fakeProv = new AuthorizationProvenance("Unregistered-Attacker", "ISSUE-FAKE", "INVALID_TOKEN", "ACTION_EXECUTION", fixedClock.instant());
        AuthorizedAction invalidAction = AuthorizedActionTestFactory.forgedAction(
                proposal.actionId(),
                proposal.actionType(),
                proposal.targetResource(),
                proposal.parameters(),
                "AUTH-LGX-FAKE",
                "fingerprint",
                fakeProv,
                "Attacker",
                "POLICY",
                "CORR",
                "IDEMP",
                fixedClock.instant(),
                fixedClock.instant().plus(Duration.ofMinutes(5))
        );

        ActionResult result = executor.execute(invalidAction);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.operationId()).isEqualTo("AUTH-PROVENANCE-ERR");
        assertThat(toolServer.getInvocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("4. Frozen ToolRegistry rejects runtime modifications")
    void testFrozenRegistryRejectsModifications() {
        assertThat(toolRegistry.isFrozen()).isTrue();

        McpToolDefinition newTool = McpToolDefinition.of(
                "rogueTool",
                "Unauthorized tool",
                ActionType.of("ROGUE"),
                Set.of("param1")
        );

        assertThatThrownBy(() -> toolRegistry.registerTool(newTool))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen and immutable");
    }

    @Test
    @DisplayName("5. Missing required parameters returns INVALID-PARAMS error without calling tool")
    void testMissingRequiredParametersRejected() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-004")
                .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                .targetResource("SHIP-5504")
                .parameter("shipmentId", "SHIP-5504") // Missing 'newAppointmentTime'
                .build();

        AuthorizedAction action = authorizationIssuer.issue(
                proposal,
                "STANDARD-POLICY",
                "LogistiX-Governance",
                Duration.ofMinutes(5),
                fixedClock.instant()
        );

        ActionResult result = executor.execute(action);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.operationId()).isEqualTo("INVALID-PARAMS");
        assertThat(toolServer.getInvocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("6. Unexpected parameters rejected by strict tool schema with UNEXPECTED-PARAMS error")
    void testUnexpectedParametersRejectedByStrictSchema() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-005")
                .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                .targetResource("SHIP-5505")
                .parameter("shipmentId", "SHIP-5505")
                .parameter("newAppointmentTime", "2026-08-24T18:00:00Z")
                .parameter("unauthorizedPrivilegedField", "DROP_DATABASE")
                .build();

        AuthorizedAction action = authorizationIssuer.issue(
                proposal,
                "STANDARD-POLICY",
                "LogistiX-Governance",
                Duration.ofMinutes(5),
                fixedClock.instant()
        );

        ActionResult result = executor.execute(action);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.operationId()).isEqualTo("UNEXPECTED-PARAMS");
        assertThat(result.message()).contains("unauthorizedPrivilegedField");
        assertThat(toolServer.getInvocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("7. Server timeout is mapped to failure ActionResult with latency captured")
    void testServerTimeoutHandling() {
        toolServer.setSimulateTimeout(true);

        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-007")
                .actionType(ActionType.UPDATE_SHIPMENT_STATUS)
                .targetResource("SHIP-5507")
                .parameter("shipmentId", "SHIP-5507")
                .parameter("status", "DELIVERED")
                .build();

        AuthorizedAction action = authorizationIssuer.issue(
                proposal,
                "STANDARD-POLICY",
                "LogistiX-Governance",
                Duration.ofMinutes(5),
                fixedClock.instant()
        );

        ActionResult result = executor.execute(action);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.message()).contains("Timeout");
    }
}
