package org.logistix.dsl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * General marker annotation for any LogistiX decision framework component.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DecisionComponent {

    /**
     * Optional component name or identifier.
     */
    String value() default "";
}
