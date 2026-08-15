package org.logistix.engine.plugins;

/**
 * State lifecycle of a dynamic DecisionPlugin.
 */
public enum PluginLifecycle {
    UNINITIALIZED,
    INITIALIZED,
    ACTIVE,
    STOPPED,
    FAILED
}
