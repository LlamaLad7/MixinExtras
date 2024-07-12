package com.llamalad7.mixinextras.expression;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.*;

/**
 * Allows you to use java-like strings to target complex pieces of bytecode.
 * <p>
 * Your injector annotation should use {@code @At("MIXINEXTRAS:EXPRESSION")}, and your handler method should have an
 * {@code @Expression} annotation attached.
 * <p>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/Expressions">the wiki article</a> for more info.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(Expressions.class)
public @interface Expression {
    /**
     * The expression to match.
     */
    String[] value();

    /**
     * If you want to attach multiple {@code @Expression}s to an injector, for example to use one of them in a
     * {@link Slice @Slice}, you can give them each {@code id}s to differentiate between them. You can then select
     * which to reference by specifying {@link At#id()}.
     */
    String id() default "";
}
