package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Pool {
    Entry[] value();

    @Target({})
    @interface Entry {
        String id();

        At[] at() default {};

        Class<?>[] type() default {};

        Local[] local() default {};
    }
}
