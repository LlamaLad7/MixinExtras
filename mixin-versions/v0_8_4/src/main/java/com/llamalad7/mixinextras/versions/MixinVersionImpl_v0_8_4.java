package com.llamalad7.mixinextras.versions;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;

@SuppressWarnings("unused")
public class MixinVersionImpl_v0_8_4 extends MixinVersionImpl_v0_8_3 {
    @Override
    public RuntimeException makeInvalidInjectionException(InjectionInfo info, String message) {
        return new InvalidInjectionException(info, message);
    }

    @Override
    public Context makeLvtContext(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
        return new Context(info, returnType, argsOnly, target, node);
    }

    @Override
    public <T> T getDecoration(IMixinConfig config, String key, T defaultValue) {
        return config.hasDecoration(key) ? config.getDecoration(key) : defaultValue;
    }
}
