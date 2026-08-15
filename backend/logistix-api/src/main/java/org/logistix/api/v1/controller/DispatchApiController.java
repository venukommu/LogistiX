package org.logistix.api.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.logistix.api.v1.dto.request.CreateDispatchRequest;
import org.logistix.api.v1.dto.response.ApiResponseWrapper;
import org.logistix.api.v1.dto.response.DispatchPlanResponse;
import org.logistix.api.v1.dto.response.ErrorResponse;
import org.logistix.common.exception.EntityNotFoundException;
import org.logistix.common.model.EntityId;
import org.logistix.core.port.inbound.DispatchUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for AI-assisted driver dispatch intelligence operations.
 * Pure orchestration delegating exclusively to inbound use cases (Hexagonal Architecture).
 */
@RestController
@RequestMapping("/api/v1/dispatches")
@Tag(name = "Dispatch Management", description = "AI-powered explainable driver dispatching & recommendation APIs")
public class DispatchApiController {

    private final DispatchUseCase dispatchUseCase;

    public DispatchApiController(DispatchUseCase dispatchUseCase) {
        this.dispatchUseCase = Objects.requireNonNull(dispatchUseCase, "DispatchUseCase must not be null");
    }

    @PostMapping("/plan")
    @Operation(
            summary = "Plan AI-assisted dispatch",
            description = "Evaluates candidate drivers using multi-criteria explainable decision models and returns an optimized dispatch recommendation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dispatch plan successfully computed",
                    content = @Content(schema = @Schema(implementation = DispatchPlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Shipment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseWrapper<DispatchPlanResponse>> planDispatch(
            @Valid @RequestBody CreateDispatchRequest request
    ) {
        List<EntityId<UUID>> preferredDrivers = request.preferredDriverIds() != null
                ? request.preferredDriverIds().stream().map(EntityId::of).toList()
                : List.of();

        var command = new DispatchUseCase.DispatchCommand(
                EntityId.of(request.shipmentId()),
                preferredDrivers,
                request.autoConfirm(),
                request.customConstraints()
        );

        var plan = dispatchUseCase.planDispatch(command);
        var response = toResponse(plan);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseWrapper.ok(response));
    }

    @GetMapping("/{dispatchId}")
    @Operation(
            summary = "Get dispatch plan by ID",
            description = "Retrieves an existing dispatch plan along with its explainability breakdown and confirmation status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispatch plan found"),
            @ApiResponse(responseCode = "404", description = "Dispatch plan not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseWrapper<DispatchPlanResponse>> getDispatch(
            @Parameter(description = "UUID of the dispatch plan", required = true)
            @PathVariable UUID dispatchId
    ) {
        var plan = dispatchUseCase.getDispatchPlan(EntityId.of(dispatchId))
                .orElseThrow(() -> new EntityNotFoundException("DispatchPlan", dispatchId.toString()));

        return ResponseEntity.ok(ApiResponseWrapper.ok(toResponse(plan)));
    }

    @PostMapping("/{dispatchId}/confirm")
    @Operation(
            summary = "Confirm dispatch plan",
            description = "Confirms and locks an AI-recommended dispatch assignment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispatch confirmed successfully"),
            @ApiResponse(responseCode = "404", description = "Dispatch plan not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponseWrapper<DispatchPlanResponse>> confirmDispatch(
            @Parameter(description = "UUID of the dispatch plan to confirm", required = true)
            @PathVariable UUID dispatchId
    ) {
        var confirmed = dispatchUseCase.confirmDispatch(EntityId.of(dispatchId));
        return ResponseEntity.ok(ApiResponseWrapper.ok(toResponse(confirmed)));
    }

    private DispatchPlanResponse toResponse(DispatchUseCase.DispatchPlan plan) {
        return new DispatchPlanResponse(
                plan.dispatchId().value(),
                plan.shipmentId().value(),
                plan.selectedDriverId() != null ? plan.selectedDriverId().value() : null,
                plan.selectedVehicleId() != null ? plan.selectedVehicleId().value() : null,
                plan.matchScore(),
                plan.explanation(),
                plan.isConfirmed()
        );
    }
}
