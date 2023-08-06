package com.llamalad7.mixinextras.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
interface InternalMethod<O, R> {
    R call(O owner, Object... args);

    static <O, R> InternalMethod<O, R> of(Class<?> clazz, String name, Class<?>... argTypes) {
        Method impl;
        try {
            impl = clazz.getDeclaredMethod(name, argTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to find method %s::%s(%s)! Please report to LlamaLad7!",
                            clazz, name,
                            Arrays.stream(argTypes).map(Class::getName).collect(Collectors.joining(", "))
                    ), e
            );
        }
        impl.setAccessible(true);
        return (owner, args) -> {
            try {
                return (R) impl.invoke(owner, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(
                        String.format(
                                "Failed to call %s::%s(%s) with args [%s]! Please report to LlamaLad7!",
                                clazz, name,
                                Arrays.stream(argTypes).map(Class::getName).collect(Collectors.joining(", ")),
                                Arrays.stream(args).map(Object::toString).collect(Collectors.joining(", "))
                        ), e
                );
            }
        };
    }

    static <O, R> InternalMethod<O, R> of(String clazz, String name, Class<?>... argTypes) {
        try {
            return of(Class.forName(clazz), name, argTypes);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Failed to find class %s! Please report to LlamaLad7!", clazz), e);
        }
    }
}
