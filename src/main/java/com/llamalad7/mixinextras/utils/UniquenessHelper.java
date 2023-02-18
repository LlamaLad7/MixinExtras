package com.llamalad7.mixinextras.utils;

import org.spongepowered.asm.util.Counter;

import java.util.HashMap;
import java.util.Map;

public class UniquenessHelper {
    private static final Map<String, Counter> TARGET_TO_COUNTER = new HashMap<>();

    public static int getNextId(String classRef) {
        return TARGET_TO_COUNTER.computeIfAbsent(classRef, k -> new Counter()).value++;
    }

    public static void clear(String classRef) {
        TARGET_TO_COUNTER.remove(classRef);
    }
}
