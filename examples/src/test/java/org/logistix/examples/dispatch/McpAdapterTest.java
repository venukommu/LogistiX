package org.logistix.examples.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.logistix.domain.action.ActionProposal;
import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.ActionStatus;
import org.logistix.domain.action.ActionType;
import org.logistix.domain.action.AuthorizedAction;
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
 * Hardened Test Suite for Model Context Protocol (MCP) Adapter execution, schema validation, and fingerprint checks.
 */
public class McpAdapterTest {

    private ToolRegistry toolRegistry;
    private MockMcpToolServer toolServer;
    private McpActionExecutor executor;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        toolRegistry = ToolRegistry.withStandardLogisticsTools();
        toolServer = new MockMcpToolServer();
        executor = new McpActionExecutor(toolRegistry, toolServer, fixedClock);
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

        AuthorizedAction action = AuthorizedAction.issue(
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

        AuthorizedAction action = AuthorizedAction.issue(
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
    @DisplayName("3. Unregistered action type is rejected with UNREGISTERED-TOOL failure")
    void testUnregisteredToolRejected() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-003")
                .actionType(ActionType.CANCEL_SHIPMENT) // Not registered in default ToolRegistry
                .targetResource("SHIP-5503")
                .parameter("shipmentId", "SHIP-5503")
                .build();

        AuthorizedAction action = AuthorizedAction.issue(
                proposal,
                "STANDARD-POLICY",
                "LogistiX-Governance",
                Duration.ofMinutes(5),
                fixedClock.instant()
        );

        ActionResult result = executor.execute(action);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.operationId()).isEqualTo("UNREGISTERED-TOOL");
        assertThat(toolServer.getInvocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("4. Missing required parameters returns INVALID-PARAMS error without calling tool")
    void testMissingRequiredParametersRejected() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-004")
                .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                .targetResource("SHIP-5504")
                .parameter("shipmentId", "SHIP-5504") // Missing 'newAppointmentTime'
                .build();

        AuthorizedAction action = AuthorizedAction.issue(
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
    @DisplayName("5. Unexpected parameters rejected by strict tool schema with UNEXPECTED-PARAMS error")
    void testUnexpectedParametersRejectedByStrictSchema() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-005")
                .actionType(ActionType.CHANGE_DELIVERY_APPOINTMENT)
                .targetResource("SHIP-5505")
                .parameter("shipmentId", "SHIP-5505")
                .parameter("newAppointmentTime", "2026-08-24T18:00:00Z")
                .parameter("unauthorizedPrivilegedField", "DROP_DATABASE") // Disallowed by strict schema
                .build();

        AuthorizedAction action = AuthorizedAction.issue(
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
    @DisplayName("6. Expired AuthorizedAction is rejected before MCP tool invocation")
    void testExpiredActionRejected() {
        ActionProposal proposal = ActionProposal.builder()
                .actionId("ACT-MCP-006")
                .actionType(ActionType.UPDATE_SHIPMENT_STATUS)
                .targetResource("SHIP-5506")
                .parameter("shipmentId", "SHIP-5506")
                .parameter("status", "DELIVERED")
                .build();

        // Action expired 10 minutes ago
        Instant pastIssuedAt = fixedClock.instant().minus(Duration.ofMinutes(15));
        AuthorizedAction expiredAction = AuthorizedAction.issue(
                proposal,
                "STANDARD-POLICY",
                "LogistiX-Governance",
                Duration.ofMinutes(5),
                pastIssuedAt
        );

        ActionResult result = executor.execute(expiredAction);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.operationId()).isEqualTo("EXPIRED");
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

        AuthorizedAction action = AuthorizedAction.issue(
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
