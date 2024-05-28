package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to wrap a whole method. To wrap individual operations within a method, including method calls, see
 * {@link com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation @WrapOperation}.
 * <p>
 * Your handler method receives the target method's arguments and an {@link Operation} representing the method
 * being wrapped.
 * You should return the same type as the wrapped method does:
 * <blockquote><pre>
 * {@code private (static) ReturnType handler(&lt;params of the original method&gt;,Operation&lt;ReturnType&gt; original)}
 * </pre></blockquote>
 * When {@code call}ing the {@code original}, you must pass everything before the {@code original} in your handler's
 * parameters. You can optionally pass different values to change what the {@code original} uses.
 * <p>
 * This chains when used by multiple people, unlike
 * {@link org.spongepowered.asm.mixin.Overwrite @Overwrite}.
 * <p>
 * <b>If you never use the {@code original} then you risk other people's changes being silently ignored.</b>
 * <p>
 * <b>NOTE:</b> While this injector in general does not support Sugar, it does have special support for using
 * {@link com.llamalad7.mixinextras.sugar.Share @Share} to share values between wrappers and the target method.
 * <p>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/WrapMethod">the wiki article</a> for more info.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapMethod {
    String[] method();

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;
}

