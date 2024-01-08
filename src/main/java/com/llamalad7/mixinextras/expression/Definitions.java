package com.llamalad7.mixinextras.expression;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Definitions {
    Definition[] value();
}
