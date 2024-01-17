package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use {@link com.llamalad7.mixinextras.injector.v2.WrapWithCondition} instead.
 * This injector occasionally wraps {@code @Inject} handlers if it applies before them.
 * The new version applies at a later stage, however in the unlikely case that you rely on the old ordering, make sure
 * to test your mixin after converting.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface WrapWithCondition {
    String[] method();

    At[] at();

    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;
}
