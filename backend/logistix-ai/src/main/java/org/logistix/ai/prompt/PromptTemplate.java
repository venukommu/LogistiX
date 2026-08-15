package org.logistix.ai.prompt;

import java.util.List;
import java.util.Map;

/**
 * Contract for parameterized prompt rendering.
 */
public interface PromptTemplate {

    String getTemplateId();

    String render(Map<String, Object> variables);

    List<PromptMessage> renderMessages(Map<String, Object> variables);
}
