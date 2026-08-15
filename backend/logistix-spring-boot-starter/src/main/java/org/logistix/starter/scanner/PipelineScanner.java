package org.logistix.starter.scanner;

import org.logistix.engine.pipeline.DecisionPipeline;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scans the Spring ApplicationContext for declared DecisionPipelines.
 */
public class PipelineScanner {

    private final ApplicationContext applicationContext;

    public PipelineScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<DecisionPipeline> scanPipelines() {
        List<DecisionPipeline> discovered = new ArrayList<>();

        // Discover directly defined DecisionPipeline beans
        Map<String, DecisionPipeline> pipelineBeans = applicationContext.getBeansOfType(DecisionPipeline.class);
        discovered.addAll(pipelineBeans.values());

        return List.copyOf(discovered);
    }
}
