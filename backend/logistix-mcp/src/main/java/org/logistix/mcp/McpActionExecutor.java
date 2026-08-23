package org.logistix.mcp;

import org.logistix.domain.action.ActionResult;
import org.logistix.domain.action.AuthorizedAction;
import org.logistix.domain.ports.ActionExecutor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Model Context Protocol (MCP) Infrastructure Adapter for LogistiX.
 *
 * Implements the outbound technology-neutral ActionExecutor port with hardened security checks:
 * 1. Accepts ONLY an AuthorizedAction that has passed deterministic LogistiX governance.
 * 2. Enforces cryptographic SHA-256 fingerprint verification (tamper-evident).
 * 3. Enforces authorization TTL expiration validity.
 * 4. Resolves tools ONLY from the controlled ToolRegistry, prohibiting arbitrary tool execution.
 * 5. Strictly validates parameters against tool schemas.
 */
public class McpActionExecutor implements ActionExecutor {

    private final ToolRegistry toolRegistry;
    private final MockMcpToolServer toolServer;
    private final Clock clock;

    public McpActionExecutor() {
        this(ToolRegistry.withStandardLogisticsTools(), new MockMcpToolServer(), Clock.systemUTC());
    }

    public McpActionExecutor(ToolRegistry toolRegistry, MockMcpToolServer toolServer) {
        this(toolRegistry, toolServer, Clock.systemUTC());
    }

    public McpActionExecutor(ToolRegistry toolRegistry, MockMcpToolServer toolServer, Clock clock) {
        this.toolRegistry = toolRegistry != null ? toolRegistry : ToolRegistry.withStandardLogisticsTools();
        this.toolServer = toolServer != null ? toolServer : new MockMcpToolServer();
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public MockMcpToolServer getToolServer() {
        return toolServer;
    }

    public Clock getClock() {
        return clock;
    }

    @Override
    public ActionResult execute(AuthorizedAction action) {
        Objects.requireNonNull(action, "AuthorizedAction must not be null");

        // 1. Authorization Token Invariant Check
        if (action.authorizationToken() == null || !action.authorizationToken().startsWith("AUTH-")) {
            return ActionResult.failure(
                    action.actionId(),
                    "AUTH-ERR",
                    "Missing or invalid LogistiX authorization token on action",
                    "Security Violation: Attempted to execute action without valid LogistiX authorization token",
                    Duration.ZERO
            );
        }

        // 2. Authorization Expiration Check
        if (action.isExpired(clock)) {
            return ActionResult.failure(
                    action.actionId(),
                    "EXPIRED",
                    "AuthorizedAction expired before execution",
                    "Security Violation: Attempted to execute expired authorization",
                    Duration.ZERO
            );
        }

        // 3. Exact Action Fingerprint Verification Check
        if (!action.matchesFingerprint()) {
            return ActionResult.failure(
                    action.actionId(),
                    "TAMPERED",
                    "AuthorizedAction fingerprint mismatch (parameters or target modified after authorization)",
                    "Security Violation: Parameter or target tampering detected",
                    Duration.ZERO
            );
        }

        // 4. Resolve Allowed MCP Tool from Registry
        Optional<McpToolDefinition> toolOpt = toolRegistry.findToolByActionType(action.actionType());
        if (toolOpt.isEmpty()) {
            return ActionResult.failure(
                    action.actionId(),
                    "UNREGISTERED-TOOL",
                    String.format("Action type [%s] is not mapped to any registered MCP tool", action.actionType().code()),
                    "Security Guardrail: AI or caller cannot invoke unapproved external MCP tools",
                    Duration.ZERO
            );
        }

        McpToolDefinition tool = toolOpt.get();

        // 5. Verify Required Tool Parameters
        List<String> missingParams = new ArrayList<>();
        for (String param : tool.requiredParameters()) {
            if (!action.parameters().containsKey(param) || action.parameters().get(param) == null) {
                missingParams.add(param);
            }
        }
        if (!missingParams.isEmpty()) {
            return ActionResult.failure(
                    action.actionId(),
                    "INVALID-PARAMS",
                    "Missing required tool parameters: " + String.join(", ", missingParams),
                    "Validation Error: Required parameters not provided in authorized action",
                    Duration.ZERO
            );
        }

        // 6. Verify Strict Schema (No unexpected/unauthorized parameters)
        List<String> unexpectedParams = new ArrayList<>();
        for (String param : action.parameters().keySet()) {
            if (!tool.isParameterAllowed(param)) {
                unexpectedParams.add(param);
            }
        }
        if (!unexpectedParams.isEmpty()) {
            return ActionResult.failure(
                    action.actionId(),
                    "UNEXPECTED-PARAMS",
                    "Unexpected parameters rejected by strict tool schema: " + String.join(", ", unexpectedParams),
                    "Validation Error: Unexpected parameters provided for strict tool definition",
                    Duration.ZERO
            );
        }

        // 7. Invoke MCP Tool via Tool Server / Transport
        Instant start = clock.instant();
        try {
            Map<String, Object> response = toolServer.invokeTool(tool.toolName(), action.parameters());
            Duration latency = Duration.between(start, clock.instant());
            String opId = (String) response.getOrDefault("operationId", "MCP-" + action.actionId());
            String msg = (String) response.getOrDefault("confirmationMessage", "MCP Tool executed successfully");

            return ActionResult.success(action.actionId(), opId, msg, response, latency);
        } catch (Exception ex) {
            Duration latency = Duration.between(start, clock.instant());
            return ActionResult.failure(
                    action.actionId(),
                    "MCP-ERR-" + action.actionId(),
                    "MCP Tool execution failed: " + ex.getMessage(),
                    ex.toString(),
                    latency
            );
        }
    }

    @Override
    public String getExecutorType() {
        return "MCP-ActionExecutor";
    }
}
