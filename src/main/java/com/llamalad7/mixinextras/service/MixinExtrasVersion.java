package com.llamalad7.mixinextras.service;

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
    V0_3_5("0.3.5", 214);

    public static final MixinExtrasVersion LATEST = values()[values().length - 1];

    private final String prettyName;
    private final int versionNumber;

    MixinExtrasVersion(String prettyName, int versionNumber) {
        this.prettyName = prettyName;
        this.versionNumber = versionNumber;
    }

    @Override
    public String toString() {
        return prettyName;
    }

    public int getNumber() {
        return versionNumber;
    }
}
