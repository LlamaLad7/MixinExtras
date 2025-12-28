package com.llamalad7.mixinextras.utils;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.llamalad7.mixinextras.config.MixinExtrasConfig;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.service.MixinService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.WeakHashMap;

public class MixinConfigUtils {
    private static final String KEY_TOP_LEVEL_MIN_VERSION = "minMixinExtrasVersion";
    private static final String KEY_SUBCONFIG = "mixinextras";
    private static final String KEY_MIN_VERSION = "minVersion";
    private static final Map<IMixinConfig, MixinExtrasConfig> CONFIG_CACHE = new WeakHashMap<>();

    public static void requireMinVersion(IMixinConfig config, MixinExtrasVersion desiredVersion, String featureName) {
        MixinExtrasVersion min = extraConfigFor(config).minVersion;
        if (min == null || min.getNumber() < desiredVersion.getNumber()) {
            throw new UnsupportedOperationException(
                    String.format(
                            "In order to use %s, Mixin Config '%s' needs to declare a reliance on " +
                                    "MixinExtras >=%s! E.g. `\"%s\": {\"%s\": \"%s\"}`",
                            featureName, config, desiredVersion,
                            KEY_SUBCONFIG,
                            KEY_MIN_VERSION,
                            MixinExtrasVersion.LATEST
                    )
            );
        }
    }

    private static MixinExtrasConfig extraConfigFor(IMixinConfig config) {
        return CONFIG_CACHE.computeIfAbsent(config, k ->
                new MixinExtrasConfig(config, readMinString(config))
        );
    }



    private static String readMinString(IMixinConfig config) {
        return readConfig(config, reader -> {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                switch (key) {
                    case KEY_SUBCONFIG: {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String innerKey = reader.nextName();
                            if (innerKey.equals(KEY_MIN_VERSION)) {
                                return reader.nextString();
                            }
                            reader.skipValue();
                        }
                        reader.endObject();
                        break;
                    }
                    case KEY_TOP_LEVEL_MIN_VERSION: {
                        return reader.nextString();
                    }
                    default: {
                        reader.skipValue();
                    }
                }
            }
            return null;
        });
    }

    private static <T> T readConfig(IMixinConfig config, JsonProcessor<T> compute) {
        try (JsonReader reader =
                     new JsonReader(new BufferedReader(new InputStreamReader(
                             MixinService.getService().getResourceAsStream(config.getName()), StandardCharsets.UTF_8)
                     ))
        ) {
            reader.setStrictness(Strictness.LENIENT);
            return compute.process(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read mixin config " + config.getName(), e);
        }
    }

    @FunctionalInterface
    private interface JsonProcessor<T> {
        T process(JsonReader reader) throws IOException;
    }
}
