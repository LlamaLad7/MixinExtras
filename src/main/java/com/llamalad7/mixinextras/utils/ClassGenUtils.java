package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClassGenUtils {
    private static final Definer DEFINER;
    private static final Map<String, byte[]> DEFINITIONS = new HashMap<>();

    static {
        Definer theDefiner;
        try {
            Unsafe.class.getMethod("defineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            theDefiner = (name, bytes, scope) -> unsafe.defineClass(name, bytes, 0, bytes.length, scope.lookupClass().getClassLoader(), scope.lookupClass().getProtectionDomain());
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException e1) {
            try {
                //noinspection JavaReflectionMemberAccess
                Method defineClass = MethodHandles.Lookup.class.getMethod("defineClass", byte[].class);
                theDefiner = (name, bytes, scope) -> {
                    try {
                        //noinspection PrimitiveArrayArgumentToVarargsMethod
                        defineClass.invoke(scope, bytes);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (NoSuchMethodException e2) {
                RuntimeException e = new RuntimeException("Could not resolve class definer! Please report to LlamaLad7.");
                e.addSuppressed(e1);
                e.addSuppressed(e2);
                throw e;
            }
        }
        DEFINER = theDefiner;
    }

    public static void defineClass(ClassNode node, MethodHandles.Lookup scope) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        String name = node.name.replace('/', '.');
        DEFINER.define(name, bytes, scope);
        DEFINITIONS.put(name, bytes);
        MixinInternals.registerClassInfo(node);
        MixinInternals.getExtensions().export(MixinEnvironment.getCurrentEnvironment(), node.name, false, node);
    }

    /**
     * Exposed for use in specific custom classloader setups. You probably don't need this.
     */
    public static Map<String, byte[]> getDefinitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    @FunctionalInterface
    private interface Definer {
        void define(String name, byte[] bytes, MethodHandles.Lookup scope);
    }
}
