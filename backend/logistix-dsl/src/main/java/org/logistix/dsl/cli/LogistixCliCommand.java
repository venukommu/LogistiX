package org.logistix.dsl.cli;

/**
 * Base contract for LogistiX Command-Line Interface (CLI) commands.
 */
public interface LogistixCliCommand {

    String getName();

    String getDescription();

    CliExecutionResult execute(CliExecutionContext context);
}
