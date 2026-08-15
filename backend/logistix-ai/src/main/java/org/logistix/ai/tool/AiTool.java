package org.logistix.ai.tool;

/**
 * Functional contract for tools exposed to AI models for function/tool calling.
 *
 * @param <I> Input argument type
 * @param <O> Output result type
 */
public interface AiTool<I, O> {

    String getName();

    String getDescription();

    Class<I> getInputType();

    O execute(I input);
}
