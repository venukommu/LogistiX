package org.logistix.api.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response payload representing an AI-generated dispatch plan.
 */
@Schema(description = "AI-generated dispatch plan with recommendation score and explainability summary")
public record DispatchPlanResponse(
        @Schema(description = "Unique dispatch identifier", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        UUID dispatchId,

        @Schema(description = "Associated shipment UUID", example = "a3f0e0c8-472d-4c3d-8e43-157973c734b4")
        UUID shipmentId,

        @Schema(description = "Selected/recommended driver UUID", example = "d8e7855e-1494-4d8d-905c-e7a9b0c15647")
        UUID selectedDriverId,

        @Schema(description = "Assigned vehicle UUID (if matched)", example = "7e1b5f10-1845-4c02-a1f9-03c6218d6e32")
        UUID selectedVehicleId,

        @Schema(description = "AI match confidence score (0.0 to 1.0)", example = "0.94")
        double matchScore,

        @Schema(description = "Explainable reasoning and key decision factors justifying this assignment", example = "Driver is located 12km away with 6.5 remaining driving hours and verified reefer endorsement.")
        String explanation,

        @Schema(description = "Flag indicating whether the dispatch plan has been confirmed and locked", example = "true")
        boolean isConfirmed
) {}
