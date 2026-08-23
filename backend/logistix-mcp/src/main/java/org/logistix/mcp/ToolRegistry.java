package org.logistix.mcp;

import org.logistix.domain.action.ActionType;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlled registry of authorized MCP enterprise tools.
 * AI models are strictly prohibited from selecting or invoking arbitrary unregistered MCP tools.
 */
public class ToolRegistry {

    private final Map<String, McpToolDefinition> toolsByName = new ConcurrentHashMap<>();
    private final Map<ActionType, McpToolDefinition> toolsByActionType = new ConcurrentHashMap<>();

    public static ToolRegistry withStandardLogisticsTools() {
        ToolRegistry registry = new ToolRegistry();

        registry.registerTool(new McpToolDefinition(
                "changeDeliveryAppointment",
                "Updates the delivery appointment time window for a shipment in TMS",
                ActionType.CHANGE_DELIVERY_APPOINTMENT,
                Set.of("shipmentId", "newAppointmentTime"),
                Set.of("reason", "rescheduleWindowHours"),
                true,
                "1.0"
        ));

        registry.registerTool(new McpToolDefinition(
                "assignDriver",
                "Assigns a commercial driver to an active shipment dispatch in Fleet Management",
                ActionType.ASSIGN_DRIVER,
                Set.of("shipmentId", "driverId"),
                Set.of("notes", "assignedBy"),
                true,
                "1.0"
        ));

        registry.registerTool(new McpToolDefinition(
                "updateShipmentStatus",
                "Updates the operational tracking status of a shipment",
                ActionType.UPDATE_SHIPMENT_STATUS,
                Set.of("shipmentId", "status"),
                Set.of("location", "timestamp"),
                true,
                "1.0"
        ));

        return registry;
    }

    public static ToolRegistry empty() {
        return new ToolRegistry();
    }

    public void registerTool(McpToolDefinition tool) {
        Objects.requireNonNull(tool, "McpToolDefinition must not be null");
        toolsByName.put(tool.toolName(), tool);
        toolsByActionType.put(tool.actionType(), tool);
    }

    public Optional<McpToolDefinition> findToolByName(String toolName) {
        if (toolName == null) return Optional.empty();
        return Optional.ofNullable(toolsByName.get(toolName));
    }

    public Optional<McpToolDefinition> findToolByActionType(ActionType actionType) {
        if (actionType == null) return Optional.empty();
        return Optional.ofNullable(toolsByActionType.get(actionType));
    }

    public boolean isToolAllowed(String toolName) {
        if (toolName == null) return false;
        return toolsByName.containsKey(toolName);
    }

    public Set<String> getRegisteredToolNames() {
        return Collections.unmodifiableSet(toolsByName.keySet());
    }
}
