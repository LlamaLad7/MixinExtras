package com.llamalad7.mixinextras;

import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import com.llamalad7.mixinextras.utils.ResourceUtils;

@SuppressWarnings("unused")
public class MixinExtrasBootstrap {
    private static boolean initialized = false;

    @Deprecated
    public static String getVersion() {
        return MixinExtrasVersion.LATEST.toString();
    }

    public static void init() {
        init(ResourceUtils.ConfigsFinder.defaultConfigsFinder());
    }
    
    public static void init(ResourceUtils.ConfigsFinder configsFinder) {
        if (initialized) {
            return;
        }
        initialized = true;
        MixinExtrasService.setup(configsFinder);
    }
}
