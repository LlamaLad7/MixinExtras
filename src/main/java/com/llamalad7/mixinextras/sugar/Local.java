package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Local {
    boolean print() default false;

    int ordinal() default -1;

    int index() default -1;

    String[] name() default {};

    boolean argsOnly() default false;
}
