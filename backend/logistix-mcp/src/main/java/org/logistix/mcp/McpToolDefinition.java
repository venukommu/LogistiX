package org.logistix.mcp;

import org.logistix.domain.action.ActionType;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Definition of an allowed, registered MCP enterprise tool with schema parameter validation rules.
 */
public record McpToolDefinition(
        String toolName,
        String description,
        ActionType actionType,
        Set<String> requiredParameters,
        Set<String> optionalParameters,
        boolean strictParametersOnly,
        String schemaVersion
) {
    public McpToolDefinition {
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        description = description != null ? description : toolName;
        requiredParameters = requiredParameters != null ? Set.copyOf(requiredParameters) : Collections.emptySet();
        optionalParameters = optionalParameters != null ? Set.copyOf(optionalParameters) : Collections.emptySet();
        schemaVersion = schemaVersion != null ? schemaVersion : "1.0";
    }

    public static McpToolDefinition of(String toolName, String description, ActionType actionType, Set<String> requiredParameters) {
        return new McpToolDefinition(toolName, description, actionType, requiredParameters, Collections.emptySet(), true, "1.0");
    }

    public static McpToolDefinition of(
            String toolName,
            String description,
            ActionType actionType,
            Set<String> requiredParameters,
            Set<String> optionalParameters,
            boolean strictParametersOnly
    ) {
        return new McpToolDefinition(toolName, description, actionType, requiredParameters, optionalParameters, strictParametersOnly, "1.0");
    }

    public boolean isParameterAllowed(String paramName) {
        if (paramName == null) return false;
        if (!strictParametersOnly) return true;
        return requiredParameters.contains(paramName) || optionalParameters.contains(paramName);
    }
}
