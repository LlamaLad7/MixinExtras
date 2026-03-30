package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.service.MixinExtrasService;
import org.spongepowered.asm.service.MixinService;

import java.io.InputStream;

public class ResourceUtils {
    static InputStream getResourceAsStream(String name) {
        InputStream result = MixinService.getService().getResourceAsStream(name);
        if (result != null) {
            // Happy fast path
            return result;
        }
        // MixinServiceModLauncher is very poorly designed and relies heavily on the current TCCL
        // This might not be what we expect if we're running e.g. on a common worker thread
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(MixinExtrasService.getInstance().getMixinConfigsFinder().getClassLoader());

        try {
            // See if we get a result with the transforming classloader
            return MixinService.getService().getResourceAsStream(name);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
    }
    
    public interface ConfigsFinder {
        ClassLoader getClassLoader();
        
        static ConfigsFinder defaultConfigsFinder() {
            return ResourceUtils.class::getClassLoader;
        }
    }
}
