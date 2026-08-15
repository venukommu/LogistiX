package org.logistix.dsl.cli;

/**
 * CLI command interface for health checking the LogistiX environment: {@code logistix doctor}.
 */
public interface DoctorCommand extends LogistixCliCommand {

    @Override
    default String getName() {
        return "doctor";
    }

    @Override
    default String getDescription() {
        return "Check environment compatibility, vector DB connection, and registered pipelines";
    }
}
