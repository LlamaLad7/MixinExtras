package com.llamalad7.mixinextras.utils;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.stream.JsonReader;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.WeakHashMap;

public class MixinConfigUtils {
    public static final String KEY_MIN_VERSION = "minMixinExtrasVersion";
    private static final Map<IMixinConfig, MixinExtrasVersion> MIN_CACHE = new WeakHashMap<>();

    public static MixinExtrasVersion minVersionFor(IMixinConfig config) {
        return MIN_CACHE.computeIfAbsent(config, k -> {
            Version min = readMin(config);
            MixinExtrasVersion[] versions = MixinExtrasVersion.values();
            if (min == null) {
                return versions[0];
            }
            if (min.isHigherThan(MixinExtrasVersion.LATEST.getSemver())) {
                throw new IllegalArgumentException(
                        String.format(
                                "Mixin Config %s requires MixinExtras >=%s but %s is present!",
                                config.getName(), min, MixinExtrasVersion.LATEST
                        )
                );
            }
            MixinExtrasVersion result = versions[0];
            for (MixinExtrasVersion version : versions) {
                if (version.getSemver().isHigherThan(min)) {
                    break;
                }
                result = version;
            }
            return result;
        });
    }

    private static Version readMin(IMixinConfig config) {
        return readConfig(config, reader -> {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals(KEY_MIN_VERSION)) {
                    String ver = reader.nextString();
                    return Version.tryParse(ver).orElseThrow(
                            () -> new IllegalArgumentException(
                                    String.format(
                                            "'%s' is not valid SemVer!",
                                            ver
                                    )
                            )
                    );
                }
                reader.skipValue();
            }
            return null;
        });
    }

    private static <T> T readConfig(IMixinConfig config, JsonProcessor<T> compute) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(MixinService.getService().getResourceAsStream(config.getName())))) {
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
