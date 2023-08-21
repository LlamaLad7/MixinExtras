package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Helpers for writing code that is compatible with all variants of Mixin 0.8+
 */
@SuppressWarnings("unchecked")
public class CompatibilityHelper {
    private static final Constructor<InvalidInjectionException> INVALID_INJECTION_EXCEPTION_CONSTRUCTOR;
    private static final Method INJECTION_INFO_GET_MIXIN_METHOD;
    private static final Constructor<Context> LVT_CONTEXT_CONSTRUCTOR;
    private static final Method INJECTION_INFO_PRE_INJECT_METHOD;
    private static final Method INJECTION_INFO_GET_ANNOTATION_METHOD;

    static {
        INVALID_INJECTION_EXCEPTION_CONSTRUCTOR =
                (Constructor<InvalidInjectionException>) Arrays.stream(InvalidInjectionException.class.getConstructors())
                        .filter(it -> {
                            Class<?>[] parameters = it.getParameterTypes();
                            return parameters.length == 2 && parameters[0].isAssignableFrom(InjectionInfo.class) && parameters[1] == String.class;
                        })
                        .findAny()
                        .orElse(null);

        INJECTION_INFO_GET_MIXIN_METHOD =
                Arrays.stream(InjectionInfo.class.getMethods())
                        .filter(it -> it.getParameterTypes().length == 0 && it.getReturnType() == IMixinContext.class && it.getName().startsWith("get"))
                        .findAny()
                        .orElse(null);

        LVT_CONTEXT_CONSTRUCTOR =
                (Constructor<Context>) Arrays.stream(Context.class.getConstructors())
                        .filter(it -> {
                            Class<?>[] parameters = it.getParameterTypes();
                            int offset = parameters.length == 4 ? 0 : parameters.length == 5 ? 1 : -1;
                            if (offset == -1) {
                                return false;
                            }
                            return parameters[offset] == Type.class && parameters[offset + 1] == boolean.class && parameters[offset + 2] == Target.class && parameters[offset + 3] == AbstractInsnNode.class;
                        })
                        .findAny()
                        .orElse(null);

        INJECTION_INFO_PRE_INJECT_METHOD = Arrays.stream(InjectionInfo.class.getMethods())
                .filter(it -> it.getName().equals("preInject"))
                .findFirst()
                .orElse(null);

        INJECTION_INFO_GET_ANNOTATION_METHOD = Arrays.stream(InjectionInfo.class.getMethods())
                .filter(it -> it.getName().startsWith("getAnnotation") && it.getReturnType() == AnnotationNode.class)
                .findFirst()
                .orElse(null);
    }

    public static RuntimeException makeInvalidInjectionException(InjectionInfo info, String message) {
        try {
            return INVALID_INJECTION_EXCEPTION_CONSTRUCTOR.newInstance(info, message);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static IMixinContext getMixin(InjectionInfo info) {
        try {
            return (IMixinContext) INJECTION_INFO_GET_MIXIN_METHOD.invoke(info);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Context makeLvtContext(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
        try {
            if (LVT_CONTEXT_CONSTRUCTOR.getParameterCount() == 4) {
                return LVT_CONTEXT_CONSTRUCTOR.newInstance(returnType, argsOnly, target, node);
            } else {
                return LVT_CONTEXT_CONSTRUCTOR.newInstance(info, returnType, argsOnly, target, node);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void preInject(InjectionInfo info) {
        try {
            INJECTION_INFO_PRE_INJECT_METHOD.invoke(info);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static AnnotationNode getAnnotation(InjectionInfo info) {
        try {
            return (AnnotationNode) INJECTION_INFO_GET_ANNOTATION_METHOD.invoke(info);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
