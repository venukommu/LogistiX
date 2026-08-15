package org.logistix.api.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.logistix.api.v1.dto.response.ApiResponseWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller for basic system telemetry and health status verification.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System Telemetry", description = "Liveness and diagnostic endpoints")
public class HealthCheckApiController {

    @GetMapping("/health")
    @Operation(summary = "Get platform status", description = "Returns active platform runtime status and timestamp.")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> checkHealth() {
        var statusPayload = Map.<String, Object>of(
                "status", "UP",
                "platform", "LogistiX AI Platform",
                "version", "0.1.0-SNAPSHOT",
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(ApiResponseWrapper.ok(statusPayload));
    }
}
