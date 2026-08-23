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
import org.logistix.examples.dispatch.lab.DispatchComparisonEngine;
import org.logistix.examples.dispatch.lab.DispatchComparisonResult;
import org.logistix.examples.dispatch.lab.DispatchLabReporter;
import org.logistix.examples.dispatch.lab.DispatchScenario;
import org.logistix.examples.dispatch.lab.DispatchScenarios;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Interactive Reference Application & Decision Lab demonstrating production-grade AI-Assisted Driver Dispatch
 * on the LogistiX Decision Intelligence Framework.
 */
public class DriverDispatchReferenceApp {

    public static void main(String[] args) {
        Map<String, String> cliArgs = parseArgs(args);

        if (cliArgs.containsKey("compare") || cliArgs.containsKey("scenario") || "compare".equalsIgnoreCase(cliArgs.get("mode"))) {
            runDecisionLabCli(cliArgs);
            return;
        }

        runStandardGoldenDemo();
    }

    private static void runDecisionLabCli(Map<String, String> args) {
        String scenarioTarget = args.getOrDefault("scenario", "all");
        String format = args.getOrDefault("format", "text");
        boolean isJson = "json".equalsIgnoreCase(format);

        Instant now = Instant.now();
        List<DispatchScenario> scenariosToRun;

        if ("all".equalsIgnoreCase(scenarioTarget)) {
            scenariosToRun = DispatchScenarios.allScenarios(now);
        } else {
            Optional<DispatchScenario> found = DispatchScenarios.findById(scenarioTarget, now);
            scenariosToRun = found.map(List::of).orElseGet(() -> DispatchScenarios.allScenarios(now));
        }

        DispatchComparisonEngine engine = new DispatchComparisonEngine(new MockDispatchAIProvider());
        List<DispatchComparisonResult> results = new ArrayList<>();

        for (DispatchScenario scenario : scenariosToRun) {
            results.add(engine.compare(scenario));
        }

        if (isJson) {
            for (DispatchComparisonResult r : results) {
                System.out.println(DispatchLabReporter.formatJson(r));
            }
        } else {
            if ("all".equalsIgnoreCase(scenarioTarget) || results.size() > 1) {
                System.out.println(DispatchLabReporter.formatScenarioSummary(results));
            }
            for (DispatchComparisonResult r : results) {
                System.out.println(DispatchLabReporter.formatSideBySideBox(r));
            }
        }

        if (args.containsKey("benchmark")) {
            runTransparentBenchmark();
        }
    }

    private static void runStandardGoldenDemo() {
        System.out.println("================================================================================");
        System.out.println("   LogistiX Framework Reference Capability: AI-Assisted Driver Dispatch");
        System.out.println("================================================================================\n");

        DecisionGraph model = DispatchDecisionModelFactory.createModel();
        System.out.println("[1] Declarative Decision Graph Topology:");
        System.out.println("    Model ID: " + model.getModelId() + " (" + model.getName() + ")");
        System.out.println("    Nodes (" + model.getNodes().size() + "): " + model.getNodes().stream().map(n -> n.getName() + " [" + n.getNodeType() + "]").toList());
        System.out.println("    Edges (" + model.getEdges().size() + "): " + model.getEdges().stream().map(e -> e.sourceNodeId() + " -> " + e.targetNodeId()).toList());
        System.out.println();

        Instant now = Instant.now();
        DispatchScenario baseline = DispatchScenarios.scenario1AiConfirms(now);

        System.out.println("[2] Candidate Fleet Initialized (" + baseline.candidateDrivers().size() + " drivers evaluated for HazMat shipment):");
        for (var driver : baseline.candidateDrivers()) {
            System.out.printf("    • %-28s | Loc: (%.2f, %.2f) | HOS: %2dh | Certs: %-16s | Tier: %-8s\n",
                    driver.name(), driver.currentLocation().latitude(), driver.currentLocation().longitude(),
                    driver.remainingHos().toHours(), driver.certifications(), driver.tier());
        }
        System.out.println();

        DecisionExecutor executor = LogistiX.getContext().getExecutor();
        List<DispatchCandidate> preparedCandidates = baseline.candidateDrivers().stream()
                .map(d -> DispatchCandidate.from(d, baseline.shipment(), now, 0.10, "MODERATE_RAIN_CENTRAL_VALLEY"))
                .toList();

        DecisionContext context = DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", preparedCandidates),
                        Fact.of("shipment", baseline.shipment())
                ),
                Map.of("weatherAdvisory", "MODERATE_RAIN_CENTRAL_VALLEY"),
                Map.of("executionMode", "HYBRID")
        );

        // 3. RULES_ONLY Execution
        System.out.println("[3] Executing Mode: RULES_ONLY (Deterministic Constraints & Scoring)...");
        DecisionPipeline rulesPipeline = DispatchDecisionPipelineFactory.createRulesOnlyPipeline();
        DecisionResult<DispatchAssignment> rulesResult = executor.execute(rulesPipeline, context);
        System.out.printf("    -> Assigned: %s | Score: %.4f | Execution Time: %d ms\n\n",
                rulesResult.recommendation().item().driverName(),
                rulesResult.score().value(),
                rulesResult.executionTime().toMillis());

        // 4. HYBRID Execution
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
        System.out.println("AI Provider Type       : " + (telemetry != null ? telemetry.providerType() : "MOCK"));
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

        if (!exp.tradeOffsConsidered().isEmpty()) {
            System.out.println("\n[Trade-Offs & Alternatives Considered]:");
            for (String tradeOff : exp.tradeOffsConsidered()) {
                System.out.println("   ↔ " + tradeOff);
            }
        }

        // 5. Resilience Test
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

        // 6. Benchmark
        runTransparentBenchmark();
    }

    private static void runTransparentBenchmark() {
        DecisionExecutor executor = LogistiX.getContext().getExecutor();
        System.out.println("\n=========================================================================================================");
        System.out.println("   HIGH-THROUGHPUT DECISION BENCHMARK (100 ITERATIONS, 20 DRIVERS)");
        System.out.println("=========================================================================================================");
        DispatchBenchmarkRunner.BenchmarkResult benchRules = DispatchBenchmarkRunner.runBenchmark(
                "RULES_ONLY", "Deterministic JVM", DispatchDecisionPipelineFactory.createRulesOnlyPipeline(), executor, 100, 20);

        DispatchBenchmarkRunner.BenchmarkResult benchHybridMock = DispatchBenchmarkRunner.runBenchmark(
                "HYBRID_MOCK", "MOCK", DispatchDecisionPipelineFactory.createHybridAiPipeline(), executor, 100, 20, benchRules.p50Millis());

        System.out.printf("%-12s | %-16s | %-6s | %-12s | %-10s | %-10s | %-10s | %-32s\n",
                "Mode", "Provider Type", "Calls", "Throughput", "Total Time", "p50 (ms)", "p95 (ms)", "Benchmark Semantics");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-12s | %-16s | %-6d | %7.1f ops/s | %8d ms | %8.2f ms | %8.2f ms | %-32s\n",
                benchRules.modeName(), benchRules.providerType(), benchRules.aiCallsPerDecision(),
                benchRules.opsPerSec(), benchRules.totalDuration().toMillis(),
                benchRules.p50Millis(), benchRules.p95Millis(), benchRules.benchmarkSemantics());
        System.out.printf("%-12s | %-16s | %-6d | %7.1f ops/s | %8d ms | %8.2f ms | %8.2f ms | %-32s\n",
                benchHybridMock.modeName(), benchHybridMock.providerType(), benchHybridMock.aiCallsPerDecision(),
                benchHybridMock.opsPerSec(), benchHybridMock.totalDuration().toMillis(),
                benchHybridMock.p50Millis(), benchHybridMock.p95Millis(), benchHybridMock.benchmarkSemantics());
        System.out.println("===================================================================================================================================\n");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    map.put(key, args[i + 1]);
                    i++;
                } else {
                    map.put(key, "true");
                }
            }
        }
        return map;
    }
}
