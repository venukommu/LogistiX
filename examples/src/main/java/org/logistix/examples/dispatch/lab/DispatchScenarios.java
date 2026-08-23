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
import java.util.UUID;

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
                .shipmentId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
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
                .driverId(UUID.fromString("11111111-1111-1111-1111-000000000001"))
                .name("Alex 'Swift' Rivera")
                .currentLocation(Coordinates.of(37.8044, -122.2712)) // Oakland (13.4 km)
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .rating(5.0)
                .historicalOnTimeRate(0.98)
                .remainingHos(Duration.ofHours(11))
                .build();

        Driver bob = Driver.builder()
                .driverId(UUID.fromString("11111111-1111-1111-1111-000000000002"))
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
                .shipmentId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .origin(Coordinates.of(37.7749, -122.4194))
                .destination(Coordinates.of(34.0522, -118.2437))
                .weightKg(12000.0)
                .volumeM3(30.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .priority(PriorityLevel.HIGH)
                .destinationRegion("US-WEST")
                .build();

        Driver driver1 = Driver.builder()
                .driverId(UUID.fromString("22222222-2222-2222-2222-000000000001"))
                .name("Alex 'Swift' Rivera")
                .currentLocation(Coordinates.of(37.8044, -122.2712))
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .rating(5.0)
                .historicalOnTimeRate(0.98)
                .remainingHos(Duration.ofHours(11))
                .build();

        Driver driver2 = Driver.builder()
                .driverId(UUID.fromString("22222222-2222-2222-2222-000000000002"))
                .name("Bob Vance")
                .currentLocation(Coordinates.of(37.3382, -121.8863))
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.GOLD)
                .rating(4.7)
                .historicalOnTimeRate(0.92)
                .remainingHos(Duration.ofHours(9))
                .build();

        return new DispatchScenario(
                "corridor-weather-risk",
                "Scenario 2: Corridor Rain & Traffic (AI Adds Context)",
                "Moderate rain on Central Valley corridor. AI enriches explainability with wet-road delay telemetry while confirming top driver.",
                shipment,
                List.of(driver1, driver2),
                "MODERATE_RAIN_CENTRAL_VALLEY",
                "MEDIUM",
                "Central Valley corridor experiencing localized rain slowdowns.",
                now,
                "AI identifies wet conditions, adds operational telemetry, and confirms Alex 'Swift' Rivera."
        );
    }

    /**
     * Scenario 3: Safety Guardrail & Constraint Inviolability.
     * Demonstrates that AI cannot override HARD constraints (e.g. uncertified driver or HOS violation).
     */
    public static DispatchScenario scenario3SafetyGuardrail(Instant now) {
        Shipment shipment = Shipment.builder()
                .shipmentId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
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
                .driverId(UUID.fromString("33333333-3333-3333-3333-000000000001"))
                .name("Charlie Davis (No HazMat/TWIC)")
                .currentLocation(Coordinates.of(37.7750, -122.4190)) // 0.1 km deadhead
                .certifications(Set.of(Certification.REEFER)) // MISSING REQUIRED CERTS
                .tier(DriverTier.PLATINUM)
                .rating(5.0)
                .historicalOnTimeRate(0.99)
                .remainingHos(Duration.ofHours(11))
                .build();

        Driver exhaustedDriver = Driver.builder()
                .driverId(UUID.fromString("33333333-3333-3333-3333-000000000002"))
                .name("Danielle Evans (Low HOS)")
                .currentLocation(Coordinates.of(37.7750, -122.4190))
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.GOLD)
                .rating(4.8)
                .historicalOnTimeRate(0.95)
                .remainingHos(Duration.ofHours(2)) // FAILS HOS (Needs ~8h)
                .build();

        Driver compliantDriver = Driver.builder()
                .driverId(UUID.fromString("33333333-3333-3333-3333-000000000003"))
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

    /**
     * Scenario 4: AI Contextual Decision (AI Influences Policy).
     * Demonstrates AI identifying severe blizzard risk on a standard driver, leading the deterministic policy
     * to safely select the Platinum winter veteran among two 100% HARD-feasible candidates.
     */
    public static DispatchScenario scenario4AiContextualDecision(Instant now) {
        Shipment shipment = Shipment.builder()
                .shipmentId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .origin(Coordinates.of(39.5296, -119.8138)) // Reno, NV
                .destination(Coordinates.of(38.5816, -121.4944)) // Sacramento, CA (via Donner Pass)
                .weightKg(14000.0)
                .volumeM3(35.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .deliveryDeadline(now.plus(Duration.ofHours(8)))
                .priority(PriorityLevel.CRITICAL)
                .destinationRegion("US-WEST")
                .build();

        // Driver A: Low deadhead, standard tier. FEASIBLE. Higher deterministic base score.
        Driver standardDriver = Driver.builder()
                .driverId(UUID.fromString("44444444-4444-4444-4444-000000000001"))
                .name("Sam 'Speedy' Miller")
                .currentLocation(Coordinates.of(39.5300, -119.8100)) // Reno Downtown (1.0 km deadhead)
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.STANDARD)
                .rating(4.5)
                .historicalOnTimeRate(0.89)
                .remainingHos(Duration.ofHours(8))
                .build();

        // Driver B: Moderate deadhead, Platinum winter veteran. FEASIBLE. Robust HOS & rating.
        Driver mountainVeteran = Driver.builder()
                .driverId(UUID.fromString("44444444-4444-4444-4444-000000000002"))
                .name("Elena 'Mountain' Rostova")
                .currentLocation(Coordinates.of(39.3280, -120.1833)) // Truckee Mountain Base (45.0 km deadhead)
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .rating(4.98)
                .historicalOnTimeRate(0.99)
                .remainingHos(Duration.ofHours(11))
                .build();

        return new DispatchScenario(
                "ai-contextual-decision",
                "Scenario 4: AI Contextual Differentiation",
                "Donner Pass blizzard warning active. Sam is slightly closer in deadhead; Elena has Platinum winter experience and 11h HOS buffer. AI flags Sam with HIGH risk, shifting deterministic policy to Elena.",
                shipment,
                List.of(standardDriver, mountainVeteran),
                "BLIZZARD_WARNING_DONNER_PASS",
                "HIGH",
                "Severe mountain blizzard on I-80. Standard equipment faces chain controls and multi-hour delays.",
                now,
                "RULES_ONLY selects Sam 'Speedy' Miller based on deadhead. HYBRID_AI selects Elena 'Mountain' Rostova due to AI contextual risk mitigation."
        );
    }

    /**
     * Scenario 5: Knowledge-Aware Dispatch (Grounded in Enterprise Policies).
     * Demonstrates AI retrieving DOC-WINTER-001 (Winter Operations Policy) and grounding its advisory
     * to favor the certified winter veteran with full policy traceability.
     */
    public static DispatchScenario scenario5KnowledgeAwareDispatch(Instant now) {
        Shipment shipment = Shipment.builder()
                .shipmentId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .origin(Coordinates.of(39.5296, -119.8138)) // Reno, NV
                .destination(Coordinates.of(38.5816, -121.4944)) // Sacramento, CA (via Donner Pass)
                .weightKg(15000.0)
                .volumeM3(38.0)
                .requiredCertifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .deliveryDeadline(now.plus(Duration.ofHours(8)))
                .priority(PriorityLevel.CRITICAL)
                .destinationRegion("US-WEST")
                .build();

        Driver standardDriver = Driver.builder()
                .driverId(UUID.fromString("55555555-5555-5555-5555-000000000001"))
                .name("Sam 'Speedy' Miller")
                .currentLocation(Coordinates.of(39.5300, -119.8100))
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.STANDARD)
                .rating(4.5)
                .historicalOnTimeRate(0.89)
                .remainingHos(Duration.ofHours(8))
                .build();

        Driver mountainVeteran = Driver.builder()
                .driverId(UUID.fromString("55555555-5555-5555-5555-000000000002"))
                .name("Elena 'Mountain' Rostova")
                .currentLocation(Coordinates.of(39.3280, -120.1833))
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .rating(4.98)
                .historicalOnTimeRate(0.99)
                .remainingHos(Duration.ofHours(11))
                .build();

        return new DispatchScenario(
                "knowledge-aware-dispatch",
                "Scenario 5: Knowledge-Aware Grounded Dispatch",
                "High-priority HazMat shipment across Donner Pass during blizzard. AI retrieves DOC-WINTER-001 policy guidelines, grounding its advisory to recommend Elena with verified enterprise policy compliance.",
                shipment,
                List.of(standardDriver, mountainVeteran),
                "BLIZZARD_WARNING_DONNER_PASS",
                "HIGH",
                "Severe mountain blizzard with chain control inspections on I-80. Enterprise DOC-WINTER-001 mandates Tier-1 equipment readiness.",
                now,
                "RULES_ONLY selects Sam on proximity. KNOWLEDGE_AI retrieves DOC-WINTER-001, grounds advisory, and selects Elena under deterministic policy."
        );
    }

    public static List<DispatchScenario> allScenarios(Instant now) {
        return List.of(
                scenario1AiConfirms(now),
                scenario2AiAddsContext(now),
                scenario3SafetyGuardrail(now),
                scenario4AiContextualDecision(now),
                scenario5KnowledgeAwareDispatch(now)
        );
    }

    public static Optional<DispatchScenario> findById(String scenarioId, Instant now) {
        return allScenarios(now).stream()
                .filter(s -> s.scenarioId().equalsIgnoreCase(scenarioId) || s.name().toLowerCase().contains(scenarioId.toLowerCase()))
                .findFirst();
    }
}
