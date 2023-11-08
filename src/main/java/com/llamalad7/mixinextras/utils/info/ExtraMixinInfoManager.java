package com.llamalad7.mixinextras.utils.info;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ExtraMixinInfoManager {
    public static ExtraMixinInfo getInfo(IMixinInfo mixin) {
        InputStream stream = MixinService.getService().getResourceAsStream("META-INF/mixinextras/" + mixin.getClassRef() + ".json");
        if (stream == null) {
            throw new IllegalStateException("Failed to read extra mixin info for " + mixin.getClassName());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return ExtraMixinInfoSerializer.deSerialize(reader.lines().collect(Collectors.joining()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
