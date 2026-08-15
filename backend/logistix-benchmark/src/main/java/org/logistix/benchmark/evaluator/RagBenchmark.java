package org.logistix.benchmark.evaluator;

import org.logistix.benchmark.report.BenchmarkReport;

/**
 * Benchmark evaluator contract for Retrieval-Augmented Generation (RAG) precision, recall, and grounding faithfulness.
 */
public interface RagBenchmark {

    String getRagPipelineIdentifier();

    BenchmarkReport evaluateRetrievalPrecisionRecall(String evaluationQuestionsDataset);

    BenchmarkReport evaluateHallucinationRate(int sampleCount);
}
