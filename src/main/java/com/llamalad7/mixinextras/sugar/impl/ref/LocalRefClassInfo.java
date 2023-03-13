package com.llamalad7.mixinextras.sugar.impl.ref;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.SyntheticClassInfo;

public class LocalRefClassInfo extends SyntheticClassInfo {
    private final String interfaceName;
    private final String desc;

    private boolean isLoaded;

    LocalRefClassInfo(IMixinInfo mixin, Class<?> refInterface, String desc) {
        super(mixin, refInterface.getName().replace('.', '/') + "Impl");
        this.interfaceName = refInterface.getName().replace('.', '/');
        this.desc = desc;
    }

    void markAsLoaded() {
        isLoaded = true;
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    String getInterfaceName() {
        return interfaceName;
    }

    String getDesc() {
        return desc;
    }
}
