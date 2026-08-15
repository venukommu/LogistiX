package org.logistix.dsl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or bean definition as a declarative LogistiX DecisionPipeline.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DecisionPipeline {

    /**
     * Unique decision type identifier handled by this pipeline (e.g., "driver-dispatch", "carrier-selection", "pricing").
     */
    String value();

    /**
     * Human-readable name of the decision pipeline.
     */
    String name() default "";

    /**
     * Semantic version of this pipeline definition.
     */
    String version() default "1.0.0";
}
