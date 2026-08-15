package org.logistix.dsl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method as a deterministic business policy rule.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DecisionRule {

    /**
     * Unique rule identifier.
     */
    String id();

    /**
     * Human-readable rule name.
     */
    String name() default "";

    /**
     * Priority order for rule evaluation (lower integer = executed earlier).
     */
    int priority() default 0;

    /**
     * Associated decision types this rule applies to.
     */
    String[] appliesTo() default {};
}
