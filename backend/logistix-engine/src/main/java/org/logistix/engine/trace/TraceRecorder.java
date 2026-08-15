package org.logistix.engine.trace;

import org.logistix.domain.decision.DecisionContext;

/**
 * Recorder contract for building a DecisionTrace throughout pipeline execution.
 */
public interface TraceRecorder {

    void recordStepEntry(DecisionTraceEntry entry);

    DecisionTrace complete(DecisionContext finalContext);
}
