package org.logistix.model.validation;

import org.logistix.model.pipeline.ModelPipeline;

/**
 * Validates sequential pipeline models for stage consistency and type transitions.
 */
public interface PipelineValidator {

    ValidationResult validatePipeline(ModelPipeline pipeline);
}
