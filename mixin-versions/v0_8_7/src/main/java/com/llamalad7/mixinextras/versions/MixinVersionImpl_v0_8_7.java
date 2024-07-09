package com.llamalad7.mixinextras.versions;

import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

@SuppressWarnings("unused")
public class MixinVersionImpl_v0_8_7 extends MixinVersionImpl_v0_8_6 {
    @Override
    public int getOrder(InjectionInfo info) {
        return info.getOrder();
    }
}
