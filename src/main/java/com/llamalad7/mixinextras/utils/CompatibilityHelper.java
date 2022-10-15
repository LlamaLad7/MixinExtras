package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Helpers for writing code that is compatible with all variants of Mixin 0.8+
 */
@SuppressWarnings("unchecked")
public class CompatibilityHelper {
    private static final Constructor<InvalidInjectionException> INVALID_INJECTION_EXCEPTION_CONSTRUCTOR;
    private static final Method INJECTION_INFO_GET_MIXIN_METHOD;
    private static final Class<? extends  LocalVariableDiscriminator.Context> MODIFY_VARIABLE_CONTEXT_CLASS;
    private static final Field MODIFY_VARIABLE_CONTEXT_INSNS;
    private static final Constructor<LocalVariableDiscriminator.Context> MODIFY_VARIABLE_CONTEXT_CONSTRUCTOR;
    private static final Method INJECTION_POINT_REGISTER_METHOD;
    private static final Field INJECTOR_LOGGER;
    private static final Method INJECTOR_LOGGER_WARN;

    static {
        INVALID_INJECTION_EXCEPTION_CONSTRUCTOR =
                (Constructor<InvalidInjectionException>) Arrays.stream(InvalidInjectionException.class.getConstructors())
                        .filter(it -> {
                            Class<?>[] parameters = it.getParameterTypes();
                            return parameters.length == 2 && parameters[0].isAssignableFrom(InjectionInfo.class) && parameters[1] == String.class;
                        })
                        .findAny()
                        .orElse(null);

        MODIFY_VARIABLE_CONTEXT_CLASS =
                (Class<? extends LocalVariableDiscriminator.Context>) Arrays.stream(ModifyVariableInjector.class.getDeclaredClasses())
                        .filter(LocalVariableDiscriminator.Context.class::isAssignableFrom)
                        .findAny()
                        .orElse(null);

        MODIFY_VARIABLE_CONTEXT_CONSTRUCTOR =
                (Constructor<LocalVariableDiscriminator.Context>) Optional.ofNullable(MODIFY_VARIABLE_CONTEXT_CLASS).flatMap(
                            context -> Arrays.stream(context.getConstructors())
                                    .filter(it -> {
                                        Class<?>[] parameters = it.getParameterTypes();
                                        int length = parameters.length;
                                        if (length != 4 && length != 5
                                                || length == 5 && !parameters[0].isAssignableFrom(InjectionInfo.class)) {
                                            return false;
                                        }
                                        return parameters[length - 4].isAssignableFrom(Type.class) && parameters[length - 3].isAssignableFrom(Boolean.TYPE) && parameters[length - 2].isAssignableFrom(Target.class) && parameters[length - 1].isAssignableFrom(AbstractInsnNode.class);
                                    })
                                    .findAny()
                                    .map(it -> {
                                        it.setAccessible(true);
                                        return it;
                                    })
                        )
                        .orElse(null);

        INJECTION_INFO_GET_MIXIN_METHOD =
                Arrays.stream(InjectionInfo.class.getMethods())
                        .filter(it -> it.getParameterTypes().length == 0 && it.getReturnType() == IMixinContext.class && it.getName().startsWith("get"))
                        .findAny()
                        .orElse(null);

        MODIFY_VARIABLE_CONTEXT_INSNS =
                Optional.ofNullable(MODIFY_VARIABLE_CONTEXT_CLASS).flatMap(
                        context -> Arrays.stream(context.getDeclaredFields())
                                .filter(it -> it.getName().equals("insns") && InsnList.class.isAssignableFrom(it.getType()))
                                .findAny()
                        )
                        .map(it -> {
                            it.setAccessible(true);
                            return it;
                        })
                        .orElse(null);

        INJECTION_POINT_REGISTER_METHOD =
                Arrays.stream(InjectionPoint.class.getMethods())
                        .filter(it -> {
                            if(!it.getName().equals("register")) {
                                return false;
                            }
                            Class<?>[] parameters = it.getParameterTypes();
                            int length = parameters.length;
                            if (length != 1 && length != 2
                                    || length == 2 && !parameters[1].isAssignableFrom(String.class)) {
                                return false;
                            }
                            return parameters[0].isAssignableFrom(Class.class);
                        })
                        .max(Comparator.comparingInt(Method::getParameterCount))
                        .orElse(null);

        INJECTOR_LOGGER =
                Arrays.stream(Injector.class.getDeclaredFields())
                        .filter(it -> it.getName().equals("logger")) // We don't know the type of the logger
                        .findAny()
                        .map(it -> {
                            it.setAccessible(true);
                            return it;
                        })
                        .orElse(null);

        INJECTOR_LOGGER_WARN =
                Optional.ofNullable(INJECTOR_LOGGER).flatMap(logger ->
                        Arrays.stream(logger.getType().getMethods())
                                .filter(it -> {
                                    if(it.getName().equals("warn")) {
                                        return false;
                                    }
                                    Class<?>[] parameters = it.getParameterTypes();
                                    return parameters.length == 2 && parameters[0].isAssignableFrom(String.class) && parameters[1].isAssignableFrom(Object[].class);
                                })
                                .findAny())
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

    public static LocalVariableDiscriminator.Context createModifyVariableContext(InjectionInfo info, Type returnType, Boolean argsOnly, Target target, AbstractInsnNode node) {
        try {
            if(MODIFY_VARIABLE_CONTEXT_CONSTRUCTOR.getParameterCount() == 4) {
                return MODIFY_VARIABLE_CONTEXT_CONSTRUCTOR.newInstance(returnType, argsOnly, target, node);
            }
            return MODIFY_VARIABLE_CONTEXT_CONSTRUCTOR.newInstance(info, returnType, argsOnly, target, node);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static InsnList getModifyVariableContextInsns(LocalVariableDiscriminator.Context context) {
        try {
            if(!MODIFY_VARIABLE_CONTEXT_CLASS.isInstance(context)) {
                throw new InvalidParameterException("'context' parameter wasn't of type 'ModifyVariableInjector.Context'");
            }
            return (InsnList) MODIFY_VARIABLE_CONTEXT_INSNS.get(context);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerInjectionPoint(Class<? extends InjectionPoint> type, String namespace, Class<? extends InjectionPoint> namespacedType) {
        try {
            if(INJECTION_POINT_REGISTER_METHOD.getParameterCount() == 2) {
                INJECTION_POINT_REGISTER_METHOD.invoke(null, type, namespace);
                return;
            }

            // If Mixin doesn't support namespaces, we use a type that has the namespace specified in the annotation
            INJECTION_POINT_REGISTER_METHOD.invoke(null, namespacedType);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void injectorLoggerWarn(String message, Object ... args) {
        try {
            INJECTOR_LOGGER_WARN.invoke(INJECTOR_LOGGER.get(null), message, args);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
