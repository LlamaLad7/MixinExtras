package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;

@SuppressWarnings("unused")
public class MixinExtrasBootstrap {
    private static boolean initialized = false;

    @Deprecated
    public static String getVersion() {
        return MixinExtrasVersion.LATEST.toString();
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        MixinExtrasService.setup();
    }
}
