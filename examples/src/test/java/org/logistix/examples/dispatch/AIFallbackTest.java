package org.logistix.examples.dispatch;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.fact.Fact;
import org.logistix.domain.fact.FactBag;
import org.logistix.dsl.LogistiX;
import org.logistix.engine.executor.DecisionExecutor;
import org.logistix.engine.pipeline.DecisionPipeline;
import org.logistix.examples.dispatch.ai.DispatchAIAdvisor;
import org.logistix.examples.dispatch.model.DispatchAssignment;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.Shipment;
import org.logistix.examples.dispatch.pipeline.DispatchDecisionPipelineFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AIFallbackTest {

    @Test
    @DisplayName("AI failure or timeout must gracefully degrade and never break the decision pipeline")
    void testAiGracefulDegradationOnFailure() {
        Instant now = Instant.now();
        Shipment shipment = Shipment.builder()
                .deliveryDeadline(now.plus(Duration.ofHours(12)))
                .build();

        Driver driver = Driver.builder()
                .name("Resilient Driver")
                .remainingHos(Duration.ofHours(10))
                .build();

        List<DispatchCandidate> candidates = List.of(
                DispatchCandidate.from(driver, shipment, now, 0.1, "CLEAR")
        );

        DecisionContext context = DecisionContext.of(
                DispatchDecisionPipelineFactory.DECISION_TYPE,
                FactBag.of(
                        Fact.of("candidates", candidates),
                        Fact.of("shipment", shipment)
                ),
                Map.of("weatherAdvisory", "BLIZZARD_ALERT"),
                Map.of("test", true)
        );

        // Faulty AI provider throwing unexpected exception
        DispatchAIAdvisor faultyProvider = new DispatchAIAdvisor("Faulty-Test-LLM", true);
        DecisionPipeline pipeline = DispatchDecisionPipelineFactory.createHybridAiPipeline(faultyProvider);

        DecisionExecutor executor = LogistiX.getContext().getExecutor();

        assertThatCode(() -> {
            DecisionResult<DispatchAssignment> result = executor.execute(pipeline, context);

            assertThat(result).isNotNull();
            assertThat(result.recommendation().item()).isNotNull();
            assertThat(result.recommendation().item().driverName()).isEqualTo("Resilient Driver");
            assertThat(result.score().value()).isGreaterThan(0.0);
            assertThat(result.explanation()).isNotNull();
        }).doesNotThrowAnyException();
    }
}
