package org.logistix.dsl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a LogistiX DecisionPlugin extension.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DecisionPlugin {

    /**
     * Unique plugin identifier.
     */
    String id();

    /**
     * Human-readable plugin name.
     */
    String name() default "";

    /**
     * Plugin version.
     */
    String version() default "1.0.0";
}
