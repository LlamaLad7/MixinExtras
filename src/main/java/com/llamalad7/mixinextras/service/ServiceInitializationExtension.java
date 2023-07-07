package com.llamalad7.mixinextras.service;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

class ServiceInitializationExtension implements IExtension {
    private final MixinExtrasService service;
    private boolean initialized;

    public ServiceInitializationExtension(MixinExtrasService service) {
        this.service = service;
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
        if (!initialized) {
            service.initialize();
            initialized = true;
        }
    }

    @Override
    public void postApply(ITargetClassContext context) {
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }
}
