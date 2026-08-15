package org.logistix.benchmark.evaluator;

import org.logistix.benchmark.report.BenchmarkReport;

/**
 * Benchmark evaluator contract for Rule Engine performance, throughput, and conflict resolution.
 */
public interface RuleEngineBenchmark {

    String getEngineIdentifier();

    BenchmarkReport evaluateThroughput(int ruleCount, int candidateCount);

    BenchmarkReport evaluateRuleConflictDetection(int ruleCount);
}
