package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.sugar.Local;

import java.lang.annotation.*;

/**
 * Defines an identifier used by an {@link Expression}.
 * Each identifier can be defined any number of times, either in one {@link Definition} or across {@link Definition}s.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(Definitions.class)
public @interface Definition {
    /**
     * The identifier to define.
     */
    String id();

    /**
     * Used to define method identifiers. The formats accepted match those for {@code @At("INVOKE")}.
     */
    String[] method() default {};

    /**
     * Used to define field identifiers. The formats accepted match those for {@code @At("FIELD")}.
     */
    String[] field() default {};

    /**
     * Used to define type identifiers, e.g. for instantiations or instanceof checks.
     */
    Class<?>[] type() default {};

    /**
     * Used to define local variable identifiers. You *must* specify a {@code type} in the annotation.
     * The different discriminators have the same behaviour as when {@link Local} is used as a sugar.
     */
    Local[] local() default {};

    /**
     * Whether to remap the method and field identifiers in this annotation.
     * The default is inherited from the attached injector.
     */
    boolean remap() default true;
}
