package org.logistix.benchmark.evaluator;

import org.logistix.benchmark.report.BenchmarkReport;

/**
 * Benchmark evaluator contract for Decision Engine quality, explainability validity, and decision latency.
 */
public interface DecisionEngineBenchmark {

    String getDecisionEngineIdentifier();

    BenchmarkReport evaluateDecisionQuality(String benchmarkScenarioDataset);

    BenchmarkReport evaluateExplainabilityFaithfulness(int sampleDecisions);
}
