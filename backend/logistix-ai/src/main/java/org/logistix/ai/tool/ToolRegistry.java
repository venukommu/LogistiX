package org.logistix.ai.tool;

import java.util.List;
import java.util.Optional;

/**
 * Registry for discovering and dispatching AI tools.
 */
public interface ToolRegistry {

    void register(AiTool<?, ?> tool);

    Optional<AiTool<?, ?>> getTool(String name);

    List<AiTool<?, ?>> getAllTools();
}
