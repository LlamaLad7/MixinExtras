package com.llamalad7.mixinextras.utils;

import java.lang.reflect.Field;

@SuppressWarnings("unchecked")
interface InternalField<O, T> {
    T get(O owner);

    void set(O owner, T newValue);

    static <O, T> InternalField<O, T> of(Class<?> clazz, String name) {
        Field impl;
        try {
            impl = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to find field %s::%s! Please report to LlamaLad7!",
                            clazz, name
                    ), e
            );
        }
        impl.setAccessible(true);
        return new InternalField<O, T>() {
            @Override
            public T get(O owner) {
                try {
                    return (T) impl.get(owner);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                            String.format(
                                    "Failed to get %s::%s on %s! Please report to LlamaLad7!",
                                    clazz, name, owner
                            ), e
                    );
                }
            }

            @Override
            public void set(O owner, T newValue) {
                try {
                    impl.set(owner, newValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                            String.format(
                                    "Failed to set %s::%s to %s on %s! Please report to LlamaLad7!",
                                    clazz, name, newValue, owner
                            ), e
                    );
                }
            }
        };
    }

    static <O, T> InternalField<O, T> of(String clazz, String name) {
        try {
            return of(Class.forName(clazz), name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Failed to find class %s! Please report to LlamaLad7!", clazz), e);
        }
    }
}
