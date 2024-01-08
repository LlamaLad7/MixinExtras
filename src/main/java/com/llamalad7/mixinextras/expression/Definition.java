package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(Definitions.class)
public @interface Definition {
    String id();

    At[] at() default {};

    Class<?>[] type() default {};

    Local[] local() default {};
}
