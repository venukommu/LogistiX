package org.logistix.model.serialization;

import org.logistix.model.model.DecisionModel;

/**
 * Serialization contract for importing and exporting DecisionModels in JSON, YAML, and XML formats.
 */
public interface DecisionModelSerializer {

    String serializeJson(DecisionModel model);

    DecisionModel deserializeJson(String json);

    String serializeYaml(DecisionModel model);

    DecisionModel deserializeYaml(String yaml);

    String serializeXml(DecisionModel model);

    DecisionModel deserializeXml(String xml);
}
