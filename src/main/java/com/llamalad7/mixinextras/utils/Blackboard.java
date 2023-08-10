package com.llamalad7.mixinextras.utils;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.MixinService;

/**
 * Helpers for the Mixin property service.
 * We have to use arrays as the backing values because on ModLauncher the properties are immutable, for some reason.
 */
@SuppressWarnings("unchecked")
public class Blackboard {
    private static final IGlobalPropertyService SERVICE = MixinService.getGlobalPropertyService();

    public static <T> T get(String key) {
        Object[] impl = SERVICE.getProperty(SERVICE.resolveKey(key));
        return impl == null ? null : (T) impl[0];
    }

    public static void put(String key, Object value) {
        SERVICE.setProperty(SERVICE.resolveKey(key), new Object[1]);
        SERVICE.<Object[]>getProperty(SERVICE.resolveKey(key))[0] = value;
    }
}
