package org.logistix.model.node;

import java.time.Duration;

/**
 * Node introducing a deliberate execution pause or rate-limiting interval.
 */
public interface DelayNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.DELAY;
    }

    Duration getDelayDuration();
}
