package com.llamalad7.mixinextras.utils;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.MixinService;

/**
 * Helpers for the Mixin property service.
 */
public class Blackboard {
    private static final IGlobalPropertyService SERVICE = MixinService.getGlobalPropertyService();

    public static <T> T get(String key) {
        return SERVICE.getProperty(SERVICE.resolveKey(key));
    }

    public static void put(String key, Object value) {
        SERVICE.setProperty(SERVICE.resolveKey(key), value);
    }
}
