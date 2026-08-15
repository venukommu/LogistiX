package org.logistix.model.node;

/**
 * Node performing memory lookup, contextual retrieval, or conversational history recall.
 */
public interface MemoryNode extends DecisionNode {

    @Override
    default NodeType getNodeType() {
        return NodeType.MEMORY;
    }

    String getQueryKey();

    int getMaxResults();
}
