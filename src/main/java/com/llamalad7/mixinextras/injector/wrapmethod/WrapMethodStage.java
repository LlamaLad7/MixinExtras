package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.sugar.impl.ShareInfo;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.OperationUtils;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.Bytecode;

import java.util.LinkedHashSet;
import java.util.List;

public abstract class WrapMethodStage {
    public abstract MethodNode apply(ClassNode targetClass, LinkedHashSet<ShareInfo> gatheredShares);

    public static class Vanilla extends WrapMethodStage {
        private final MethodNode original;

        public Vanilla(MethodNode original) {
            this.original = original;
        }

        @Override
        public MethodNode apply(ClassNode targetClass, LinkedHashSet<ShareInfo> gatheredShares) {
            return original;
        }
    }

    public static class Wrapper extends WrapMethodStage {
        private final WrapMethodStage inner;
        private final MethodNode handler;
        private final Type operationType;
        private final List<ShareInfo> shares;
        private final boolean isStatic;

        public Wrapper(WrapMethodStage inner, MethodNode handler, Type operationType, List<ShareInfo> shares) {
            this.inner = inner;
            this.handler = handler;
            this.operationType = operationType;
            this.shares = shares;
            this.isStatic = Bytecode.isStatic(handler);
        }

        @Override
        public MethodNode apply(ClassNode targetClass, LinkedHashSet<ShareInfo> gatheredShares) {
            MethodNode wrapper = inner.apply(targetClass, gatheredShares);
            MethodNode inner = move(targetClass, wrapper, "$mixinextras$wrapped");

            InsnList insns = new InsnList();

            if (!isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            Type[] argTypes = Type.getArgumentTypes(inner.desc);
            Type returnType = Type.getReturnType(inner.desc);

            Bytecode.loadArgs(argTypes, insns, isStatic ? 0 : 1);

            if (!isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            OperationUtils.makeOperation(
                    argTypes, returnType, insns, !isStatic, new Type[0],
                    targetClass, operationType, inner.name,
                    (paramArrayIndex, loadArgs) -> {
                        InsnList call = new InsnList();
                        loadArgs.accept(call);
                        call.add(ASMUtils.getInvokeInstruction(targetClass, inner));
                        return call;
                    }
            );

            insns.add(ASMUtils.getInvokeInstruction(targetClass, handler));
            insns.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));

            wrapper.instructions.add(insns);
            return wrapper;
        }
    }

    protected MethodNode move(ClassNode targetClass, MethodNode original, String suffix) {
        MethodNode newMethod = new MethodNode(
                original.access,
                UniquenessHelper.getUniqueMethodName(
                        targetClass, original.name + suffix
                ),
                original.desc,
                original.signature,
                original.exceptions.toArray(new String[0])
        );
        original.accept(newMethod);
        original.instructions.clear();
        original.tryCatchBlocks = null;
        original.localVariables = null;
        targetClass.methods.add(newMethod);
        return newMethod;
    }
}
