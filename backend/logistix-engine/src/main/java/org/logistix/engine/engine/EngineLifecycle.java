package org.logistix.engine.engine;

/**
 * Standard lifecycle contract for starting and stopping the LogistiX execution runtime.
 */
public interface EngineLifecycle {

    void start();

    void stop();

    boolean isRunning();
}
