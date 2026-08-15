package org.logistix.dsl.annotation;

import org.logistix.domain.constraint.ConstraintSeverity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method as an operational feasibility guardrail constraint.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DecisionConstraint {

    /**
     * Unique constraint identifier.
     */
    String id();

    /**
     * Human-readable constraint name.
     */
    String name() default "";

    /**
     * Constraint enforcement rigidity (HARD = aborts candidate, SOFT = penalty score).
     */
    ConstraintSeverity severity() default ConstraintSeverity.HARD;

    /**
     * Target decision types this constraint applies to.
     */
    String[] appliesTo() default {};
}
