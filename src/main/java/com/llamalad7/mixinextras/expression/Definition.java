package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.sugar.Local;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(Definitions.class)
public @interface Definition {
    String id();

    String[] method() default {};

    String[] field() default {};

    Class<?>[] type() default {};

    Local[] local() default {};

    boolean remap() default true;
}
