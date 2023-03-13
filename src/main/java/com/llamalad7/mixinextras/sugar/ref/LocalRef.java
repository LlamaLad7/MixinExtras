package com.llamalad7.mixinextras.sugar.ref;

/**
 * Represents a reference to a local variable.
 * Specialised variants are provided in the same package for all primitive types.
 * Do not store {@link LocalRef} instances - their lifecycle is an implementation detail.
 * They will be valid throughout the handler method they are passed to, but no other guarantees are made.
 * Do not create custom {@link LocalRef} implementations - methods may be added in the future.
 * @param <T> the type of the local variable - MUST be concrete
 */
public interface LocalRef<T> {
    /**
     * Gets the current value of the variable. This may change between calls, even in the same handler method.
     * @return the variable's current value
     */
    T get();

    /**
     * Sets the value of the variable. This value will be written back to the target method.
     * @param value a new value for the variable
     */
    void set(T value);
}
