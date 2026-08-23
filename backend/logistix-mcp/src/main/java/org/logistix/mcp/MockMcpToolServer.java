package org.logistix.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic local in-memory Mock MCP Tool Server for offline testing, demo verification, and CI execution.
 * Simulates enterprise system response contracts for TMS, Fleet Management, and Dispatch without external network dependencies.
 */
public class MockMcpToolServer {

    private final AtomicInteger invocationCount = new AtomicInteger(0);
    private final Map<String, AtomicInteger> perToolInvocationCount = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> lastToolArguments = new ConcurrentHashMap<>();
    private volatile boolean simulateTimeout = false;
    private volatile boolean simulateServerError = false;

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    public void setSimulateServerError(boolean simulateServerError) {
        this.simulateServerError = simulateServerError;
    }

    public int getInvocationCount() {
        return invocationCount.get();
    }

    public int getInvocationCount(String toolName) {
        AtomicInteger count = perToolInvocationCount.get(toolName);
        return count != null ? count.get() : 0;
    }

    public Map<String, Object> getLastArguments(String toolName) {
        return lastToolArguments.getOrDefault(toolName, Collections.emptyMap());
    }

    public void reset() {
        invocationCount.set(0);
        perToolInvocationCount.clear();
        lastToolArguments.clear();
        simulateTimeout = false;
        simulateServerError = false;
    }

    /**
     * Executes the requested tool on the mock server.
     */
    public Map<String, Object> invokeTool(String toolName, Map<String, Object> arguments) {
        Objects.requireNonNull(toolName, "toolName must not be null");
        invocationCount.incrementAndGet();
        perToolInvocationCount.computeIfAbsent(toolName, k -> new AtomicInteger(0)).incrementAndGet();
        lastToolArguments.put(toolName, arguments != null ? new LinkedHashMap<>(arguments) : Collections.emptyMap());

        if (simulateTimeout) {
            throw new RuntimeException("MCP Tool Invocation Timeout (simulated 504 Gateway Timeout)");
        }

        if (simulateServerError) {
            throw new RuntimeException("MCP Server Error (simulated 500 Internal Server Error)");
        }

        String opId = "MCP-OP-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("operationId", opId);
        response.put("tool", toolName);
        response.put("status", "SUCCESS");

        switch (toolName) {
            case "changeDeliveryAppointment" -> {
                response.put("shipmentId", arguments.get("shipmentId"));
                response.put("updatedAppointmentTime", arguments.get("newAppointmentTime"));
                response.put("confirmationMessage", "Delivery appointment rescheduled successfully in Enterprise TMS.");
            }
            case "assignDriver" -> {
                response.put("shipmentId", arguments.get("shipmentId"));
                response.put("driverId", arguments.get("driverId"));
                response.put("confirmationMessage", "Driver assigned and dispatched in Fleet Management System.");
            }
            case "updateShipmentStatus" -> {
                response.put("shipmentId", arguments.get("shipmentId"));
                response.put("status", arguments.get("status"));
                response.put("confirmationMessage", "Shipment tracking status updated in Core Logistics Database.");
            }
            default -> {
                response.put("message", "Tool execution completed successfully.");
                response.put("receivedArguments", arguments);
            }
        }

        return response;
    }
}
