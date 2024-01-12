package com.llamalad7.mixinextras.utils.info;

import com.llamalad7.mixinextras.utils.MixinInternals;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

public class ExtraMixinInfoManager {
    private static final Map<ClassNode, ExtraMixinInfo> cache = new WeakHashMap<>();

    public static ExtraMixinInfo getInfo(IMixinInfo mixin) {
        return cache.computeIfAbsent(MixinInternals.getClassNode(mixin), k -> {
            InputStream stream = MixinService.getService().getResourceAsStream("META-INF/mixinextras/" + mixin.getClassRef() + ".info");
            if (stream == null) {
                throw new IllegalStateException("Failed to read extra mixin info for " + mixin.getClassName());
            }
            try (InputStream input = stream) {
                return new ExtraMixinInfo(input);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
