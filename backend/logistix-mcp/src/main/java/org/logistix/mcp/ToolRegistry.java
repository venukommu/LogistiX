package org.logistix.mcp;

import org.logistix.domain.action.ActionType;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controlled registry of authorized MCP enterprise tools.
 * Enforces a strict configuration lifecycle: configure -> freeze -> immutable execution.
 * AI models are strictly prohibited from selecting or invoking arbitrary unregistered MCP tools.
 */
public class ToolRegistry {

    private final Map<String, McpToolDefinition> toolsByName = new ConcurrentHashMap<>();
    private final Map<ActionType, McpToolDefinition> toolsByActionType = new ConcurrentHashMap<>();
    private final AtomicBoolean frozen = new AtomicBoolean(false);

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

        registry.freeze();
        return registry;
    }

    public static ToolRegistry empty() {
        return new ToolRegistry();
    }

    /**
     * Registers an MCP tool definition. Fails if registry has already been frozen or if action type is already bound.
     */
    public synchronized void registerTool(McpToolDefinition tool) {
        Objects.requireNonNull(tool, "McpToolDefinition must not be null");
        if (frozen.get()) {
            throw new IllegalStateException("Security Guardrail: Cannot register tool [" + tool.toolName() + "]. ToolRegistry is frozen and immutable.");
        }
        if (toolsByActionType.containsKey(tool.actionType())) {
            throw new IllegalArgumentException("ActionType [" + tool.actionType().code() + "] is already bound to tool [" +
                    toolsByActionType.get(tool.actionType()).toolName() + "]. Ambiguous tool bindings are prohibited.");
        }
        toolsByName.put(tool.toolName(), tool);
        toolsByActionType.put(tool.actionType(), tool);
    }

    /**
     * Freezes the registry, preventing any further tool additions, modifications, or replacements.
     */
    public void freeze() {
        frozen.set(true);
    }

    public boolean isFrozen() {
        return frozen.get();
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
