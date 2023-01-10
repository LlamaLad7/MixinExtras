package com.llamalad7.mixinextras.sugar.impl;

import org.spongepowered.asm.mixin.throwables.MixinException;

public class SugarApplicationException extends MixinException {
    public SugarApplicationException(String message) {
        super(message);
    }

    public SugarApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
