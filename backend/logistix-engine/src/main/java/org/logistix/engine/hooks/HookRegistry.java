package org.logistix.engine.hooks;

import java.util.List;

/**
 * Registry for managing and discovering lifecycle DecisionHooks.
 */
public interface HookRegistry {

    void register(DecisionHook hook);

    List<DecisionHook> getHooks();
}
