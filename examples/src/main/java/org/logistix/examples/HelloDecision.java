package org.logistix.examples;

import org.logistix.domain.decision.DecisionResult;
import org.logistix.domain.fact.FactSource;
import org.logistix.dsl.LogistiX;

/**
 * <h3>Hello World: LogistiX Fluent Decision</h3>
 * Demonstrates the simplest way to execute an operational decision using LogistiX.
 */
public class HelloDecision {

    public static void main(String[] args) {
        // Run a decision with arbitrary operational facts in 4 lines of code!
        DecisionResult<String> result = LogistiX.<String>decision("driver-dispatch")
                .fact("shipmentId", "SHIP-9901", FactSource.SYSTEM)
                .fact("origin", "Chicago, IL")
                .fact("destination", "Detroit, MI")
                .fact("cargoWeightLbs", 18500)
                .execute();

        System.out.println("Decision Completed!");
        System.out.println("Type: " + result.decisionType());
        System.out.println("Recommended Option: " + result.recommendation().item());
        System.out.println("Score: " + result.score().value());
        System.out.println("Confidence: " + result.confidence());
        System.out.println("Explanation: " + result.explanation().summary());
        System.out.println("Duration: " + result.executionTime().toMillis() + "ms");
    }
}
