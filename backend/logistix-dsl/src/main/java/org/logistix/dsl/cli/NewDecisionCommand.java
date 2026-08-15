package org.logistix.dsl.cli;

/**
 * CLI command interface for scaffolding new decision pipelines: {@code logistix new decision <name>}.
 */
public interface NewDecisionCommand extends LogistixCliCommand {

    @Override
    default String getName() {
        return "new decision";
    }

    @Override
    default String getDescription() {
        return "Scaffold a new decision pipeline with sample constraints, rules, and tests";
    }
}
