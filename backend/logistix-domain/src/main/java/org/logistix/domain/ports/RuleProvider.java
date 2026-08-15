package org.logistix.domain.ports;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.rule.Rule;

import java.util.List;

/**
 * Outbound SPI for dynamically loading domain business rules from external catalogs, repositories, or scripts.
 */
public interface RuleProvider {

    <T> List<Rule<T>> getRulesForContext(DecisionContext context, Class<T> candidateType);
}
