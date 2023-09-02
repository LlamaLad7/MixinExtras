package com.llamalad7.mixinextras.service;

public enum MixinExtrasVersion {
    V0_2_0_RC_1("0.2.0-rc.1", 2);

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
