package com.llamalad7.mixinextras.sugar.impl.handlers;

import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.Bytecode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Information about a sugared handler method. Can be transformed by {@link HandlerTransformer}s.
 * If transformations are required, a new bridge handler will be created with the required changes, and the original
 * will become an ordinary method which the bridge delegates to.
 */
public class HandlerInfo {
    private final Map<Integer, ParameterWrapper> wrappers = new LinkedHashMap<>();

    private static class ParameterWrapper {
        private final Type type;
        private final Type generic;
        private final BiConsumer<InsnList, Runnable> unwrap;

        private ParameterWrapper(Type type, Type generic, BiConsumer<InsnList, Runnable> unwrap) {
            this.type = type;
            this.generic = generic;
            this.unwrap = unwrap;
        }
    }

    public void wrapParameter(SugarParameter param, Type type, Type generic, BiConsumer<InsnList, Runnable> unwrap) {
        wrappers.put(param.paramIndex, new ParameterWrapper(type, generic, unwrap));
    }

    public void transformHandler(ClassNode targetClass, MethodNode handler) {
        Type[] paramTypes = Type.getArgumentTypes(handler.desc);
        InsnList insns = new InsnList();

        if (!Bytecode.isStatic(handler)) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        int index = Bytecode.isStatic(handler) ? 0 : 1;
        for (int i = 0; i < paramTypes.length; i++) {
            VarInsnNode loadInsn = new VarInsnNode(paramTypes[i].getOpcode(Opcodes.ILOAD), index);

            ParameterWrapper wrapper = wrappers.get(i);
            if (wrapper != null) {
                paramTypes[i] = wrapper.type;
                loadInsn.setOpcode(wrapper.type.getOpcode(Opcodes.ILOAD));
                wrapper.unwrap.accept(insns, () -> insns.add(loadInsn));
            } else {
                insns.add(loadInsn);
            }

            index += paramTypes[i].getSize();
        }
        insns.add(ASMUtils.getInvokeInstruction(targetClass, handler));
        insns.add(new InsnNode(Type.getReturnType(handler.desc).getOpcode(Opcodes.IRETURN)));

        handler.instructions = insns;
        handler.localVariables = null;
        handler.name += "$mixinextras$bridge" + UniquenessHelper.getNextId(targetClass.name);
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), paramTypes);
    }

    public void transformGenerics(ArrayList<Type> generics) {
        for (Map.Entry<Integer, ParameterWrapper> entry : wrappers.entrySet()) {
            Type type = entry.getValue().generic;
            generics.set(entry.getKey(), type);
        }
    }
}
