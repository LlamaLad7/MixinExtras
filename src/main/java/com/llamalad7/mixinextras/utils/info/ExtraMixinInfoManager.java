package com.llamalad7.mixinextras.utils.info;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.io.InputStream;

public class ExtraMixinInfoManager {
    public static ExtraMixinInfo getInfo(IMixinInfo mixin) {
        InputStream stream = MixinService.getService().getResourceAsStream("META-INF/mixinextras/" + mixin.getClassRef() + ".info");
        if (stream == null) {
            throw new IllegalStateException("Failed to read extra mixin info for " + mixin.getClassName());
        }
        try (InputStream input = stream) {
            return ExtraMixinInfoSerializer.deSerialize(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
