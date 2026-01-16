package com.llamalad7.mixinextras.service;

import com.github.zafarkhaja.semver.Version;

public enum MixinExtrasVersion {
    V0_2_0_BETA_1("0.2.0-beta.1", -9),
    V0_2_0_BETA_2("0.2.0-beta.2", -8),
    V0_2_0_BETA_3("0.2.0-beta.3", -7),
    V0_2_0_BETA_4("0.2.0-beta.4", -6),
    V0_2_0_BETA_5("0.2.0-beta.5", -5),
    V0_2_0_BETA_6("0.2.0-beta.6", -4),
    V0_2_0_BETA_7("0.2.0-beta.7", -3),
    V0_2_0_BETA_8("0.2.0-beta.8", -2),
    V0_2_0_BETA_9("0.2.0-beta.9", -1),
    V0_3_4("0.3.4", 213),
    V0_4_0_BETA_1("0.4.0-beta.1", 315),
    V0_5_0_BETA_1("0.5.0-beta.1", 417),
    V0_5_3("0.5.3", 429),
    ;

    public static final MixinExtrasVersion LATEST = values()[values().length - 1];

    private final String prettyName;
    private final int versionNumber;
    private final Version semver;

    MixinExtrasVersion(String prettyName, int versionNumber) {
        this.prettyName = prettyName;
        this.versionNumber = versionNumber;
        this.semver = Version.parse(prettyName);
    }

    @Override
    public String toString() {
        return prettyName;
    }

    public int getNumber() {
        return versionNumber;
    }

    public Version getSemver() {
        return semver;
    }
}
