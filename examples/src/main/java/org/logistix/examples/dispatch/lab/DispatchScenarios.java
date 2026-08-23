package org.logistix.examples.dispatch.lab;

import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.examples.dispatch.model.Certification;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.DriverTier;
import org.logistix.examples.dispatch.model.Shipment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Standardized Golden Demonstration Scenarios for the Driver Dispatch Decision Lab.
 * All scenarios are fully deterministic, self-contained, and run offline.
 */
public final class DispatchScenarios {

    private DispatchScenarios() {}

    /**
     * Scenario 1: Baseline Clear Operational Corridor.
     * AI confirms the deterministic choice while adding operational reassurance.
     */
    public static DispatchScenario scenario1AiConfirms(Instant now) {
        Shipment shipment = Shipment.builder()
                .shipmentId(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .origin(Coordinates.of(37.7749, -122.4194)) // San Francisco
                .destination(Coordinates.of(34.0522, -118.2437)) // Los Angeles
                .weightKg(10000.0)
                .volumeM3(30.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .priority(PriorityLevel.HIGH)
                .destinationRegion("US-WEST")
                .build();

        Driver alex = Driver.builder()
                .name("Alex 'Swift' Rivera")
                .currentLocation(Coordinates.of(37.8044, -122.2712)) // Oakland (13.4 km)
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .rating(5.0)
                .historicalOnTimeRate(0.98)
                .remainingHos(Duration.ofHours(11))
                .build();

        Driver bob = Driver.builder()
                .name("Bob Vance")
                .currentLocation(Coordinates.of(37.3382, -121.8863)) // San Jose (67.6 km)
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.GOLD)
                .rating(4.7)
                .historicalOnTimeRate(0.94)
                .remainingHos(Duration.ofHours(9))
                .build();

        return new DispatchScenario(
                "baseline-clear",
                "Scenario 1: Baseline Clear Corridor (AI Confirms)",
                "Clear weather corridor between SF and LA. Both drivers are feasible; AI confirms deterministic recommendation and adds route reassurance.",
                shipment,
                List.of(alex, bob),
                "CLEAR",
                "LOW",
                "I-5 South corridor in optimal driving condition with light traffic.",
                now,
                "Both RULES_ONLY and HYBRID_AI recommend Alex 'Swift' Rivera. AI adds contextual confidence."
        );
    }

    /**
     * Scenario 2: Corridor Weather & Traffic Risk.
     * Demonstrates AI identifying contextual risk in a severe storm scenario among feasible candidates.
     */
    public static DispatchScenario scenario2AiAddsContext(Instant now) {
        Shipment shipment = Shipment.builder()
                .shipmentId(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .origin(Coordinates.of(39.5296, -119.8138)) // Reno, NV
                .destination(Coordinates.of(38.5816, -121.4944)) // Sacramento, CA (via Donner Pass)
                .weightKg(14000.0)
                .volumeM3(35.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(8)))
                .priority(PriorityLevel.CRITICAL)
                .destinationRegion("US-WEST")
                .build();

        Driver standardDriver = Driver.builder()
                .name("Sam 'Speedy' Miller")
                .currentLocation(Coordinates.of(39.5300, -119.8100)) // Reno Downtown (1 km deadhead)
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.STANDARD)
                .rating(4.5)
                .historicalOnTimeRate(0.89)
                .remainingHos(Duration.ofHours(8))
                .build();

        Driver veteranDriver = Driver.builder()
                .name("Elena 'Mountain' Rostova")
                .currentLocation(Coordinates.of(39.3280, -120.1833)) // Truckee Mountain Base (45 km deadhead)
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .rating(4.98)
                .historicalOnTimeRate(0.99)
                .remainingHos(Duration.ofHours(11))
                .build();

        return new DispatchScenario(
                "corridor-weather-risk",
                "Scenario 2: Mountain Pass Blizzard Risk (AI Adds Context)",
                "Donner Pass blizzard advisory active. Sam has shorter deadhead, but Elena has Platinum winter reliability and substantial HOS buffer.",
                shipment,
                List.of(standardDriver, veteranDriver),
                "BLIZZARD_WARNING_DONNER_PASS",
                "HIGH",
                "Chain controls active on I-80 West. Significant delay risk for standard equipment.",
                now,
                "AI identifies elevated mountain weather risk, highlighting Elena's HOS buffer and winter track record."
        );
    }

    /**
     * Scenario 3: Safety Guardrail & Constraint Inviolability.
     * Demonstrates that AI cannot override HARD constraints (e.g. uncertified driver or HOS violation).
     */
    public static DispatchScenario scenario3SafetyGuardrail(Instant now) {
        Shipment shipment = Shipment.builder()
                .shipmentId(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .weightKg(18000.0)
                .volumeM3(40.0)
                .requiredCertifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .deliveryDeadline(now.plus(Duration.ofHours(10)))
                .priority(PriorityLevel.CRITICAL)
                .destinationRegion("US-WEST")
                .build();

        Driver uncertifiedVip = Driver.builder()
                .name("Charlie Davis (No HazMat/TWIC)")
                .currentLocation(Coordinates.of(37.7750, -122.4190)) // 0.1 km deadhead
                .certifications(Set.of(Certification.REEFER)) // MISSING REQUIRED CERTS
                .tier(DriverTier.PLATINUM)
                .rating(5.0)
                .historicalOnTimeRate(0.99)
                .remainingHos(Duration.ofHours(11))
                .build();

        Driver exhaustedDriver = Driver.builder()
                .name("Danielle Evans (Low HOS)")
                .currentLocation(Coordinates.of(37.7750, -122.4190))
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.GOLD)
                .rating(4.8)
                .historicalOnTimeRate(0.95)
                .remainingHos(Duration.ofHours(2)) // FAILS HOS (Needs ~8h)
                .build();

        Driver compliantDriver = Driver.builder()
                .name("Alex 'Swift' Rivera (Compliant)")
                .currentLocation(Coordinates.of(37.8044, -122.2712)) // 13.4 km deadhead
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .rating(5.0)
                .historicalOnTimeRate(0.98)
                .remainingHos(Duration.ofHours(11))
                .build();

        return new DispatchScenario(
                "safety-constraint-protection",
                "Scenario 3: Safety Guardrail Enforcement",
                "High-priority HazMat shipment. VIP driver lacks certifications; second driver lacks HOS. Compliant driver must be assigned regardless of AI preference.",
                shipment,
                List.of(uncertifiedVip, exhaustedDriver, compliantDriver),
                "CLEAR",
                "LOW",
                "Strict regulatory HazMat compliance checkpoint.",
                now,
                "Both modes reject uncertified and exhausted drivers; compliant driver is assigned with 100% regulatory safety."
        );
    }

    public static List<DispatchScenario> allScenarios(Instant now) {
        return List.of(
                scenario1AiConfirms(now),
                scenario2AiAddsContext(now),
                scenario3SafetyGuardrail(now)
        );
    }

    public static Optional<DispatchScenario> findById(String scenarioId, Instant now) {
        return allScenarios(now).stream()
                .filter(s -> s.scenarioId().equalsIgnoreCase(scenarioId) || s.name().toLowerCase().contains(scenarioId.toLowerCase()))
                .findFirst();
    }
}
