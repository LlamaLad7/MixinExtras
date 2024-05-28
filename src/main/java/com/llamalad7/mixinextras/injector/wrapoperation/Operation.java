package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;

/**
 * Represents an operation that has been wrapped by {@link WrapOperation} or a method that has been wrapped by
 * {@link WrapMethod}. This may either be the operation the user targeted originally, or a wrapped version of it,
 * allowing for chaining.
 * @param <R> the return type of the operation.
 */
@FunctionalInterface
public interface Operation<R> {
    R call(Object... args);
}
