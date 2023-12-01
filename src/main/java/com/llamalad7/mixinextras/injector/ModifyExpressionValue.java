package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to tweak the resultant value of a
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeInvoke method call},
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess field get},
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeConstant constant} or
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeNew object instantiation}.
 * <p>
 * Your handler method receives the expression's resultant value (optionally followed by the enclosing method's
 * parameters), and should return the adjusted value:
 * <blockquote><pre>
 * {@code private (static) ExpressionType handler(ExpressionType original)}
 * </pre></blockquote>
 * This chains when used by multiple people, unlike
 * {@link org.spongepowered.asm.mixin.injection.Redirect @Redirect} and
 * {@link org.spongepowered.asm.mixin.injection.ModifyConstant @ModifyConstant}.
 * <p>
 * <b>If you never use the {@code original} then you risk other people's changes being silently ignored.</b>
 * <p>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/ModifyExpressionValue">the wiki article</a> for more info.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyExpressionValue {
    String[] method();

    At[] at();

    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;
}
