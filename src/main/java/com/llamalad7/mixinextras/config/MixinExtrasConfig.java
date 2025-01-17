package com.llamalad7.mixinextras.config;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

/**
 * Our custom properties for a Mixin Config, declared inside the {@code "mixinextras"} JSON object.
 * E.g.
 * <pre>
 * {@code
 * {
 *     ...
 *     "mixinextras": {
 *         "minVersion": "0.0.1"
 *     },
 *     ...
 * }
 * }
 * </pre>
 */
public class MixinExtrasConfig {
    /**
     * The minimum required version of MixinExtras.
     * This is used to gate behavioural changes.
     * Bumping this version may opt you into new behaviours.
     * <br>
     * Should be specified as SemVer, e.g. {@code "0.0.1-beta.1"}
     */
    @SerializedName("minVersion")
    private final String minVersionString;

    private final transient String configName;
    public final transient MixinExtrasVersion minVersion;

    public MixinExtrasConfig(IMixinConfig config, String minVersion) {
        this.configName = config.getName();
        this.minVersionString = minVersion;
        this.minVersion = determineMinVersion();
    }

    private MixinExtrasVersion determineMinVersion() {
        if (minVersionString == null) {
            return null;
        }
        Version min = Version.tryParse(minVersionString).orElseThrow(
                () -> new IllegalArgumentException(
                        String.format(
                                "'%s' is not valid SemVer!",
                                minVersionString
                        )
                )
        );
        MixinExtrasVersion[] versions = MixinExtrasVersion.values();
        if (min.isHigherThan(MixinExtrasVersion.LATEST.getSemver())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Mixin Config %s requires MixinExtras >=%s but %s is present!",
                            configName, min, MixinExtrasVersion.LATEST
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
    }
}
