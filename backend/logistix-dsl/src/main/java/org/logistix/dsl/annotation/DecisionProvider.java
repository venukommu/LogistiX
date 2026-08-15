package org.logistix.dsl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a component as an external provider SPI (AIProvider, KnowledgeProvider, RuleProvider, etc.).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DecisionProvider {

    /**
     * Provider identifier or provider category name.
     */
    String value() default "";
}
