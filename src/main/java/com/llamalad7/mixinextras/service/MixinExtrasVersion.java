package com.llamalad7.mixinextras.service;

public enum MixinExtrasVersion {
    V0_2_0_BETA_11("0.2.0-beta.11", 1);

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
