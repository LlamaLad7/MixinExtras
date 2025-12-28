package com.llamalad7.mixinextras.utils;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.llamalad7.mixinextras.config.MixinExtrasConfig;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.service.MixinService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MixinConfigUtils {
    private static final String KEY_TOP_LEVEL_MIN_VERSION = "minMixinExtrasVersion";
    private static final String KEY_SUBCONFIG = "mixinextras";
    private static final String KEY_MIN_VERSION = "minVersion";
    private static final String KEY_PARENT = "parent";
    private static final Map<String, MixinExtrasConfig> CONFIG_CACHE = new HashMap<>();

    public static void requireMinVersion(IMixinConfig config, MixinExtrasVersion desiredVersion, String featureName) {
        MixinExtrasVersion min = extraConfigFor(config.getName()).minVersion;
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

    private static MixinExtrasConfig extraConfigFor(String configName) {
        return CONFIG_CACHE.computeIfAbsent(configName, MixinConfigUtils::readMixinExtrasConfig);
    }

    private static MixinExtrasConfig readMixinExtrasConfig(String configName) {
        MutableObject<MixinExtrasConfig> parent = new MutableObject<>();
        MutableObject<String> minVersion = new MutableObject<>();

        readConfig(configName, reader -> {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                switch (key) {
                    case KEY_SUBCONFIG: {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String innerKey = reader.nextName();
                            if (innerKey.equals(KEY_MIN_VERSION) && minVersion.getValue() == null) {
                                minVersion.setValue(reader.nextString());
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                        break;
                    }
                    case KEY_TOP_LEVEL_MIN_VERSION: {
                        if (minVersion.getValue() == null) {
                            minVersion.setValue(reader.nextString());
                        } else {
                            reader.skipValue();
                        }
                        break;
                    }
                    case KEY_PARENT: {
                        String parentName = reader.nextString();
                        parent.setValue(extraConfigFor(parentName));
                        break;
                    }
                    default: {
                        reader.skipValue();
                    }
                }
            }
        });

        return new MixinExtrasConfig(configName, parent.getValue(), minVersion.getValue());
    }

    private static void readConfig(String configName, JsonProcessor compute) {
        try (JsonReader reader =
                     new JsonReader(new BufferedReader(new InputStreamReader(
                             MixinService.getService().getResourceAsStream(configName), StandardCharsets.UTF_8)
                     ))
        ) {
            reader.setStrictness(Strictness.LENIENT);
            compute.process(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read mixin config " + configName, e);
        }
    }

    @FunctionalInterface
    private interface JsonProcessor {
        void process(JsonReader reader) throws IOException;
    }
}
