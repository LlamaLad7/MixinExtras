package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

// This class must only be instantiated via unsafe. It exists solely for Fabric compatibility.
class DummyInjectionInfo extends InjectionInfo {
    private DummyInjectionInfo() {
        super(null, null, null);
        throw new UnsupportedOperationException();
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        throw new UnsupportedOperationException();
    }

    static InjectionInfo create(IMixinInfo mixin) {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            DummyInjectionInfo info = (DummyInjectionInfo) unsafe.allocateInstance(DummyInjectionInfo.class);
            Field contextField = Class.forName("org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo").getDeclaredField("context");
            contextField.setAccessible(true);
            contextField.set(info, new IMixinContext() {
                @Override
                public IMixinInfo getMixin() {
                    return mixin;
                }

                @Override
                public Extensions getExtensions() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getClassName() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getClassRef() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getTargetClassRef() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public IReferenceMapper getReferenceMapper() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean getOption(MixinEnvironment.Option option) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int getPriority() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Target getTargetMethod(MethodNode method) {
                    throw new UnsupportedOperationException();
                }
            });
            return info;
        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
