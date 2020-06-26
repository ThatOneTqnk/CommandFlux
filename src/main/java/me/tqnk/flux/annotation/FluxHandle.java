package me.tqnk.flux.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by MSH on 3/25/2020
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FluxHandle {
    String[] aliases();
    String[] paramNames() default {};
    String permission() default "";
    int min() default 0;
    boolean op() default false;
}
