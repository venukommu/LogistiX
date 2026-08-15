package org.logistix.model.plan;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ordered phase in an ExecutionPlan containing units that can execute sequentially or in parallel.
 */
public record ExecutionStage(
        int stageIndex,
        String name,
        List<ExecutionUnit> units,
        boolean parallel
) {
    public ExecutionStage {
        Objects.requireNonNull(name, "Stage name must not be null");
        units = units != null ? List.copyOf(units) : Collections.emptyList();
    }

    public static ExecutionStage sequential(int stageIndex, String name, List<ExecutionUnit> units) {
        return new ExecutionStage(stageIndex, name, units, false);
    }

    public static ExecutionStage parallel(int stageIndex, String name, List<ExecutionUnit> units) {
        return new ExecutionStage(stageIndex, name, units, true);
    }
}
