package com.llamalad7.mixinextras.utils;

import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Used to pass data between different relocated versions of MixinExtras.
 */
public class Blackboard {
    private static final Map<String, Object> IMPL = BlackboardMarkerExtension.getImpl();

    public static boolean has(String key) {
        return IMPL.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) IMPL.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOrPut(String key, Supplier<T> supplier) {
        return (T) IMPL.computeIfAbsent(key, k -> supplier.get());
    }

    public static void put(String key, Object value) {
        IMPL.put(key, value);
    }

    public static <T> void modify(String key, UnaryOperator<T> operator) {
        IMPL.put(key, operator.apply(get(key)));
    }

    public static void remove(String key) {
        IMPL.remove(key);
    }
}
