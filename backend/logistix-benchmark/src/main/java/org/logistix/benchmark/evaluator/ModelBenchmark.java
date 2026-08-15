package org.logistix.benchmark.evaluator;

import org.logistix.benchmark.report.BenchmarkReport;

/**
 * Benchmark evaluator contract for Base Foundation Models and Fine-Tuned Domain Models.
 */
public interface ModelBenchmark {

    String getModelIdentifier();

    BenchmarkReport evaluateModelAccuracy(String datasetPath);

    BenchmarkReport evaluateInferenceLatency(int concurrentRequests);

    BenchmarkReport evaluateStructuredOutputReliability(int sampleCount);
}
