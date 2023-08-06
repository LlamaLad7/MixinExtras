package com.llamalad7.mixinextras.service;

public enum MixinExtrasVersion {
    V0_2_0_BETA_10("0.2.0-beta.10");

    public static final MixinExtrasVersion LATEST = values()[values().length - 1];

    private final String prettyName;

    MixinExtrasVersion(String prettyName) {
        this.prettyName = prettyName;
    }

    @Override
    public String toString() {
        return prettyName;
    }
}
