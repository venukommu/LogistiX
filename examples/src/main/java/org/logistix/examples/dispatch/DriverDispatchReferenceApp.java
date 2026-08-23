package org.logistix.examples.dispatch;

import org.logistix.ai.dispatch.AITelemetry;
import org.logistix.ai.dispatch.MockDispatchAIProvider;
import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.explanation.Explanation;
import org.logistix.domain.explanation.FeatureContribution;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.model.Certification;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.DriverTier;
import org.logistix.examples.dispatch.model.Shipment;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionModelFactory;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;
import org.logistix.examples.dispatch.simulation.DispatchBenchmarkRunner;
import org.logistix.model.graph.DecisionGraph;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interactive Reference Application demonstrating production-grade AI-Assisted Driver Dispatch
 * on the LogistiX Decision Intelligence Framework.
 */
public class DriverDispatchReferenceApp {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   LogistiX Framework Reference Capability: AI-Assisted Driver Dispatch");
        System.out.println("================================================================================\n");

        // 1. Render Declarative Decision Graph Topology
        DecisionGraph model = DispatchDecisionModelFactory.createModel();
        System.out.println("[1] Declarative Decision Graph Topology:");
        System.out.println("    Model ID: " + model.getModelId() + " (" + model.getName() + ")");
        System.out.println("    Nodes (" + model.getNodes().size() + "): " + model.getNodes().stream().map(n -> n.getName() + " [" + n.getNodeType() + "]").toList());
        System.out.println("    Edges (" + model.getEdges().size() + "): " + model.getEdges().stream().map(e -> e.sourceNodeId() + " -> " + e.targetNodeId()).toList());
        System.out.println();

        // 2. Prepare Sample Operational Fleet & Urgent HazMat Shipment
        Instant now = Instant.now();

        Shipment urgentHazMatShipment = Shipment.builder()
                .shipmentId(UUID.randomUUID())
                .origin(Coordinates.of(37.7749, -122.4194)) // San Francisco DC
                .destination(Coordinates.of(34.0522, -118.2437)) // Los Angeles FC (~615 km)
                .weightKg(12500.0)
                .volumeM3(35.0)
                .requiredCertifications(Set.of(Certification.HAZMAT))
                .pickupTimeWindowStart(now)
                .pickupTimeWindowEnd(now.plus(Duration.ofHours(2)))
                .deliveryDeadline(now.plus(Duration.ofHours(12))) // 12-hour strict SLA
                .priority(PriorityLevel.HIGH)
                .destinationRegion("US-WEST")
                .build();

        // Candidate Driver Pool with varying feasibility and tier states
        Driver driverA = Driver.builder()
                .name("Alex 'Swift' Rivera")
                .currentLocation(Coordinates.of(37.8044, -122.2712)) // Oakland (~15 km deadhead)
                .remainingHos(Duration.ofHours(11))
                .vehicleWeightCapacityKg(22000.0)
                .vehicleVolumeCapacityM3(65.0)
                .certifications(Set.of(Certification.HAZMAT, Certification.TWIC))
                .tier(DriverTier.PLATINUM)
                .timeUntilMandatoryRest(Duration.ofHours(6))
                .homeRegion("US-WEST")
                .rating(4.95)
                .historicalOnTimeRate(0.98)
                .build();

        Driver driverB = Driver.builder()
                .name("Bob Vance")
                .currentLocation(Coordinates.of(37.3382, -121.8863)) // San Jose (~75 km deadhead)
                .remainingHos(Duration.ofHours(9))
                .vehicleWeightCapacityKg(18000.0)
                .vehicleVolumeCapacityM3(50.0)
                .certifications(Set.of(Certification.HAZMAT))
                .tier(DriverTier.GOLD)
                .timeUntilMandatoryRest(Duration.ofHours(4))
                .homeRegion("US-WEST")
                .rating(4.70)
                .historicalOnTimeRate(0.92)
                .build();

        Driver driverC_NoHazMat = Driver.builder()
                .name("Charlie Davis (No Hazmat)")
                .currentLocation(Coordinates.of(37.7749, -122.4194)) // In SF (0 km deadhead)
                .remainingHos(Duration.ofHours(10))
                .vehicleWeightCapacityKg(25000.0)
                .vehicleVolumeCapacityM3(70.0)
                .certifications(Set.of(Certification.REEFER)) // Missing HAZMAT!
                .tier(DriverTier.PLATINUM)
                .build();

        Driver driverD_LowHos = Driver.builder()
                .name("Danielle Evans (Low HOS)")
                .currentLocation(Coordinates.of(37.7749, -122.4194))
                .remainingHos(Duration.ofHours(3)) // Insufficient HOS for 8.5h transit!
                .vehicleWeightCapacityKg(20000.0)
                .certifications(Set.of(Certification.HAZMAT))
                .build();

        List<Driver> fleet = List.of(driverA, driverB, driverC_NoHazMat, driverD_LowHos);
        List<DispatchCandidate> candidates = fleet.stream()
                .map(d -> DispatchCandidate.from(d, urgentHazMatShipment, now, 0.15, "MODERATE_RAIN"))
                .toList();

        System.out.println("[2] Candidate Fleet Initialized (" + fleet.size() + " drivers evaluated for HazMat shipment):");
        for (DispatchCandidate c : candidates) {
            System.out.printf("    • %-28s | Loc: (%.2f, %.2f) | HOS: %2dh | Certs: %-16s | Tier: %-8s\n",
                    c.driver().name(), c.driver().currentLocation().latitude(), c.driver().currentLocation().longitude(),
                    c.driver().remainingHos().toHours(), c.driver().certifications(), c.driver().tier());
        }
        System.out.println();

        DecisionExecutor executor = LogistiX.getContext().getExecutor();
        DecisionContext context = DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", candidates),
                        Fact.of("shipment", urgentHazMatShipment)
                ),
                Map.of("weatherAdvisory", "MODERATE_RAIN_CENTRAL_VALLEY"),
                Map.of("executionMode", "HYBRID")
        );

        // 3. Execution in RULES_ONLY mode
        System.out.println("[3] Executing Mode: RULES_ONLY (Deterministic Constraints & Scoring)...");
        DecisionPipeline rulesPipeline = DispatchDecisionPipelineFactory.createRulesOnlyPipeline();
        DecisionResult<DispatchAssignment> rulesResult = executor.execute(rulesPipeline, context);
        System.out.printf("    -> Assigned: %s | Score: %.4f | Execution Time: %d ms\n\n",
                rulesResult.recommendation().item().driverName(),
                rulesResult.score().value(),
                rulesResult.executionTime().toMillis());

        // 4. Execution in HYBRID mode (Rules + Mock / Spring AI Advisor)
        System.out.println("[4] Executing Mode: HYBRID (Rules + AI Contextual Advisor [MOCK])...");
        MockDispatchAIProvider mockProvider = new MockDispatchAIProvider("Mock-Dispatch-AI", false);
        DecisionPipeline hybridPipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(mockProvider);
        DecisionResult<DispatchAssignment> hybridResult = executor.execute(hybridPipeline, context);

        DispatchAssignment assignment = hybridResult.recommendation().item();
        Explanation exp = hybridResult.explanation();
        Map<String, Object> meta = hybridResult.recommendation().metadata();
        AITelemetry telemetry = (AITelemetry) meta.get("aiTelemetry");

        System.out.println("\n================================================================================");
        System.out.println("   DISPATCH DECISION OUTCOME & EXPLAINABILITY REPORT");
        System.out.println("================================================================================");
        System.out.println("Decision Type          : " + hybridResult.decisionType());
        System.out.println("AI Provider            : " + (telemetry != null ? telemetry.providerName() : "NONE"));
        System.out.println("AI Provider Type       : MOCK");
        System.out.println("AI Invocations Count   : " + (telemetry != null ? telemetry.invocationCount() : 0));
        System.out.println("AI Prompt Version      : " + (telemetry != null ? telemetry.promptVersion() : "N/A"));
        System.out.println("AI Advisory Confidence : " + String.format("%.2f%%", (telemetry != null && telemetry.advisoryConfidence() != null ? telemetry.advisoryConfidence() * 100.0 : 92.0)));
        System.out.println("Decision Confidence    : " + String.format("%.2f%%", hybridResult.confidence() * 100.0));
        System.out.println("Execution Duration     : " + hybridResult.executionTime().toMillis() + " ms");
        System.out.println("Recommendation         : ASSIGN -> " + assignment.driverName());
        System.out.println("Composite Score        : " + String.format("%.4f", hybridResult.score().value()));
        System.out.println("Deadhead Distance      : " + String.format("%.1f km", assignment.deadheadDistanceKm()));
        System.out.println("Linehaul Distance      : " + String.format("%.1f km", assignment.mainDistanceKm()));
        System.out.println("Scheduled Delivery     : " + assignment.scheduledDeliveryTime());
        System.out.println("Estimated Trip Cost    : $" + String.format("%.2f", assignment.estimatedCostUsd()));
        System.out.println("\nRationale: \n\"" + exp.summary() + "\"");

        System.out.println("\n[Deterministic Feature Contributions]:");
        for (FeatureContribution fc : exp.featureContributions()) {
            System.out.printf("   [%-8s] %-30s (Weight: %.2f, Score: %.2f) -> %s\n",
                    fc.impactDirection(), fc.featureName(), fc.weight(), fc.contributionScore(), fc.rationale());
        }

        System.out.println("\n[Key Decision Factors & AI Insights]:");
        for (String factor : exp.keyFactors()) {
            System.out.println("   ✔ " + factor);
        }

        System.out.println("\n[Trade-Offs & Alternatives Considered]:");
        for (String tradeOff : exp.tradeOffsConsidered()) {
            System.out.println("   ↔ " + tradeOff);
        }

        // 5. Demonstrate Graceful AI Fallback when AI Provider is offline
        System.out.println("\n================================================================================");
        System.out.println("   RESILIENCE TEST: SIMULATED AI PROVIDER OUTAGE (GRACEFUL FALLBACK)");
        System.out.println("================================================================================");
        DecisionPipeline fallbackPipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(
                MockDispatchAIProvider.offline()
        );

        DecisionResult<DispatchAssignment> fallbackResult = executor.execute(fallbackPipeline, context);
        AITelemetry fbTelemetry = (AITelemetry) fallbackResult.recommendation().metadata().get("aiTelemetry");
        System.out.println("Execution with Offline AI : " + (fallbackResult.recommendation().item().isAssigned() ? "SUCCESS (Graceful Fallback)" : "FAILED"));
        System.out.println("Fallback Status           : " + (fbTelemetry != null ? fbTelemetry.status() : "UNKNOWN"));
        System.out.println("Fallback Recommendation   : " + fallbackResult.recommendation().item().driverName());
        System.out.println("Fallback Score            : " + String.format("%.4f", fallbackResult.score().value()));
        System.out.println("Execution Duration        : " + fallbackResult.executionTime().toMillis() + " ms");

        // 6. High-Throughput In-Memory Benchmark Demonstration
        System.out.println("\n================================================================================");
        System.out.println("   HIGH-THROUGHPUT DECISION BENCHMARK (100 ITERATIONS, 20 DRIVERS)");
        System.out.println("================================================================================");
        DispatchBenchmarkRunner.BenchmarkResult benchRules = DispatchBenchmarkRunner.runBenchmark(
                "RULES_ONLY", "Deterministic JVM", DispatchDecisionPipelineFactory.createRulesOnlyPipeline(), executor, 100, 20);

        DispatchBenchmarkRunner.BenchmarkResult benchHybridMock = DispatchBenchmarkRunner.runBenchmark(
                "HYBRID_MOCK", "Mock AI (In-Memory)", DispatchDecisionPipelineFactory.createHybridAiPipeline(), executor, 100, 20);

        System.out.printf("%-14s | %-22s | %-12s | %-10s | %-10s | %-10s | %-10s\n",
                "Mode", "Provider Type", "Throughput", "Total Time", "p50 (ms)", "p95 (ms)", "Avg Score");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.printf("%-14s | %-22s | %7.1f ops/s | %8d ms | %8.2f ms | %8.2f ms | %8.3f\n",
                benchRules.modeName(), benchRules.providerType(), benchRules.opsPerSec(), benchRules.totalDuration().toMillis(),
                benchRules.p50Millis(), benchRules.p95Millis(), benchRules.avgScore());
        System.out.printf("%-14s | %-22s | %7.1f ops/s | %8d ms | %8.2f ms | %8.2f ms | %8.3f\n",
                benchHybridMock.modeName(), benchHybridMock.providerType(), benchHybridMock.opsPerSec(), benchHybridMock.totalDuration().toMillis(),
                benchHybridMock.p50Millis(), benchHybridMock.p95Millis(), benchHybridMock.avgScore());
        System.out.println("=====================================================================================================\n");
    }
}
