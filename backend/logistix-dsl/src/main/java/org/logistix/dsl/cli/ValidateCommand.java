package org.logistix.dsl.cli;

/**
 * CLI command interface for statically validating decision pipelines: {@code logistix validate}.
 */
public interface ValidateCommand extends LogistixCliCommand {

    @Override
    default String getName() {
        return "validate";
    }

    @Override
    default String getDescription() {
        return "Validate pipeline configurations, constraint completeness, and rule precedence";
    }
}
