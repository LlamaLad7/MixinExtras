package com.llamalad7.mixinextras.expression;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(Expressions.class)
public @interface Expression {
    String[] value();

    String id() default "";
}