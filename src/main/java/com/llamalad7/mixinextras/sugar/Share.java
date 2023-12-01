package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to share values between handler methods in the same target method. This is generally preferable to storing
 * the value in a field because this is thread-safe and does not consume any long-term memory.
 * <p>
 * You must provide an ID for the shared value and the annotated parameter's type must be one of the
 * {@link com.llamalad7.mixinextras.sugar.ref.LocalRef LocalRef}
 * family.
 * The parameter's name is irrelevant and only the ID in the annotation is used to share matching values.
 * <p>
 * The same reference objects will be passed to all handler methods requesting a given ID within a given target method
 * invocation. IDs are per-mixin, and it is impossible to share a value between two mixins, so you don't need to worry
 * about including your modid or anything similar in them.
 * <p>
 * Note: If a {@code @Share}d value is read from before it has been written to, no exception is thrown and it will simply return
 * the default value for that type (0, 0f, null, etc), much like a field.
 * <p>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/Share">the wiki article</a> for more info.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Share {
    /**
     * The id for this shared value.
     */
    String value();
}
