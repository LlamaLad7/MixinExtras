package com.llamalad7.mixinextras.versions;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.VersionNumber;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public abstract class MixinVersion {
    private static final List<String> VERSIONS = Arrays.asList("0.8.4", "0.8.3", "0.8");
    private static final MixinVersion INSTANCE;

    static {
        VersionNumber currentVersion = VersionNumber.parse(MixinEnvironment.getCurrentEnvironment().getVersion());
        MixinVersion current = null;
        for (String version : VERSIONS) {
            if (VersionNumber.parse(version).compareTo(currentVersion) > 0) {
                continue;
            }
            try {
                Class<?> implClass = Class.forName(
                        MixinVersion.class.getPackage().getName() + ".MixinVersionImpl" + "_v" + version.replace('.', '_')
                );
                current = (MixinVersion) implClass.getConstructor().newInstance();
                break;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        INSTANCE = current;
    }

    public static MixinVersion getInstance() {
        return INSTANCE;
    }

    public abstract RuntimeException makeInvalidInjectionException(InjectionInfo info, String message);

    public abstract IMixinContext getMixin(InjectionInfo info);

    public abstract LocalVariableDiscriminator.Context makeLvtContext(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node);

    public abstract void preInject(InjectionInfo info);

    public abstract AnnotationNode getAnnotation(InjectionInfo info);
}
