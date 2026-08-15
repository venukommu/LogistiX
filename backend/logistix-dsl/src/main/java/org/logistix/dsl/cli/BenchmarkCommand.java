package org.logistix.dsl.cli;

/**
 * CLI command interface for executing scenario benchmarks: {@code logistix benchmark}.
 */
public interface BenchmarkCommand extends LogistixCliCommand {

    @Override
    default String getName() {
        return "benchmark";
    }

    @Override
    default String getDescription() {
        return "Run high-throughput latency and accuracy benchmarks against synthetic scenarios";
    }
}
