package org.logistix.examples.dispatch.simulation;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.fact.Fact;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.Shipment;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Benchmark runner evaluating operational dispatch pipelines across deterministic rules-only,
 * mock AI, and live Spring AI modes with accurate latency and throughput accounting.
 */
public class DispatchBenchmarkRunner {

    public record BenchmarkResult(
            String modeName,
            String providerType,
            int totalIterations,
            int successfulDecisions,
            int zeroFeasibilityCount,
            Duration totalDuration,
            double opsPerSec,
            double p50Millis,
            double p95Millis,
            double p99Millis,
            double avgScore
    ) {}

    public static BenchmarkResult runBenchmark(
            String modeName,
            String providerType,
            DecisionPipeline pipeline,
            DecisionExecutor executor,
            int iterations,
            int fleetSize
    ) {
        DispatchScenarioGenerator generator = new DispatchScenarioGenerator();
        List<Driver> drivers = generator.generateDrivers(fleetSize);

        List<Long> latenciesNanos = new ArrayList<>(iterations);
        int successCount = 0;
        int zeroFeasibleCount = 0;
        double totalScore = 0.0;

        Instant benchStart = Instant.now();

        for (int i = 0; i < iterations; i++) {
            Instant now = Instant.now();
            Shipment shipment = generator.generateShipment(now, i % 5 == 0);
            List<DispatchCandidate> candidates = generator.buildCandidatePairings(drivers, shipment, now);

            DecisionContext context = DecisionContext.of(
                    DispatchDecisionPipelineFactory.DECISION_TYPE,
                    org.logistix.domain.fact.FactBag.of(
                            Fact.of("candidates", candidates),
                            Fact.of("shipment", shipment)
                    ),
                    Map.of("weatherAdvisory", i % 10 == 0 ? "HEAVY_RAIN_WARNING" : "CLEAR"),
                    Map.of("iteration", i)
            );

            long start = System.nanoTime();
            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);
            long durationNanos = System.nanoTime() - start;

            latenciesNanos.add(durationNanos);

            if (result.recommendation().item() != null && result.recommendation().item().isAssigned()) {
                successCount++;
                totalScore += result.recommendation().score().value();
            } else {
                zeroFeasibleCount++;
            }
        }

        Duration totalDuration = Duration.between(benchStart, Instant.now());
        Collections.sort(latenciesNanos);

        double p50Ms = latenciesNanos.get((int) (iterations * 0.50)) / 1_000_000.0;
        double p95Ms = latenciesNanos.get((int) (iterations * 0.95)) / 1_000_000.0;
        double p99Ms = latenciesNanos.get((int) (iterations * 0.99)) / 1_000_000.0;
        double opsPerSec = iterations / Math.max(totalDuration.toMillis() / 1000.0, 0.001);
        double avgScore = successCount > 0 ? (totalScore / successCount) : 0.0;

        return new BenchmarkResult(
                modeName,
                providerType,
                iterations,
                successCount,
                zeroFeasibleCount,
                totalDuration,
                opsPerSec,
                p50Ms,
                p95Ms,
                p99Ms,
                avgScore
        );
    }
}
