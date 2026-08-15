package org.logistix.api.v1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating an AI-assisted dispatch plan.
 */
@Schema(description = "Request payload to initiate AI-assisted driver dispatch planning")
public record CreateDispatchRequest(

        @NotNull(message = "Shipment ID is mandatory")
        @Schema(description = "Unique UUID of the shipment to dispatch", example = "a3f0e0c8-472d-4c3d-8e43-157973c734b4", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID shipmentId,

        @Schema(description = "Optional list of preferred candidate driver UUIDs", example = "[\"d8e7855e-1494-4d8d-905c-e7a9b0c15647\"]")
        List<UUID> preferredDriverIds,

        @Schema(description = "Whether to automatically confirm and reserve upon recommendation", defaultValue = "false")
        boolean autoConfirm,

        @Schema(description = "Optional dynamic natural language or business constraints passed to the decision engine", example = "Must have refrigerated trailer and hazmat certification")
        String customConstraints
) {}
