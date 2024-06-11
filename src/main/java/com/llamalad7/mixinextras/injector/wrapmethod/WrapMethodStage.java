package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.sugar.impl.ShareInfo;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.OperationUtils;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.Bytecode;

import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class WrapMethodStage {
    protected abstract MethodNode getVanillaMethod();

    public abstract MethodNode apply(ClassNode targetClass, LinkedHashSet<ShareInfo> gatheredShares);

    public static class Vanilla extends WrapMethodStage {
        private final MethodNode original;

        public Vanilla(MethodNode original) {
            this.original = original;
        }

        @Override
        protected MethodNode getVanillaMethod() {
            return original;
        }

        @Override
        public MethodNode apply(ClassNode targetClass, LinkedHashSet<ShareInfo> gatheredShares) {
            stripShareInitializers(gatheredShares);
            int shareStartIndex = Bytecode.getFirstNonArgLocalIndex(original);
            changeDesc(gatheredShares);
            fixLocals(shareStartIndex, new ArrayList<>(gatheredShares));
            return original;
        }

        private void stripShareInitializers(LinkedHashSet<ShareInfo> gatheredShares) {
            for (ShareInfo share : gatheredShares) {
                share.stripInitializerFrom(original);
            }
        }

        private void changeDesc(LinkedHashSet<ShareInfo> gatheredShares) {
            Type[] shareParams =
                    gatheredShares.stream()
                            .map(it -> it.getShareType().getImplType())
                            .toArray(Type[]::new);
            Type[] params = ArrayUtils.addAll(Type.getArgumentTypes(original.desc), shareParams);
            Type returnType = Type.getReturnType(original.desc);
            original.desc = Type.getMethodDescriptor(returnType, params);
        }

        private void fixLocals(int shareStartIndex, List<ShareInfo> allShares) {
            if (allShares.isEmpty()) {
                return;
            }
            Map<Integer, Integer> oldToNewShares =
                    IntStream.range(0, allShares.size())
                            .boxed()
                            .collect(Collectors.toMap(
                                    i -> allShares.get(i).getLvtIndex(),
                                    i -> shareStartIndex + i
                            ));
            for (ShareInfo share : allShares) {
                share.setLvtIndex(oldToNewShares.get(share.getLvtIndex()));
            }
            IntUnaryOperator changeIndex = index -> {
                Integer newShare = oldToNewShares.get(index);
                if (newShare != null) {
                    return newShare;
                }
                if (index < shareStartIndex) {
                    return index;
                }
                return index + allShares.size();
            };

            for (AbstractInsnNode insn : original.instructions.toArray()) {
                if (insn instanceof VarInsnNode) {
                    VarInsnNode varNode = (VarInsnNode) insn;
                    varNode.var = changeIndex.applyAsInt(varNode.var);
                } else if (insn instanceof IincInsnNode) {
                    IincInsnNode incNode = (IincInsnNode) insn;
                    incNode.var = changeIndex.applyAsInt(incNode.var);
                }
            }
            if (original.localVariables != null) {
                for (LocalVariableNode local : original.localVariables) {
                    local.index = changeIndex.applyAsInt(local.index);
                }
            }
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
        protected MethodNode getVanillaMethod() {
            return inner.getVanillaMethod();
        }

        @Override
        public MethodNode apply(ClassNode targetClass, LinkedHashSet<ShareInfo> gatheredShares) {
            LinkedHashSet<ShareInfo> newShares = new LinkedHashSet<>(gatheredShares);
            List<ShareInfo> sharesToAllocate = new ArrayList<>();
            for (ShareInfo share : shares) {
                if (newShares.add(share)) {
                    // IDs could be shared even between `@WrapMethod`s, so someone else might have claimed this before.
                    sharesToAllocate.add(share);
                }
            }

            MethodNode vanilla = getVanillaMethod();
            Type[] operationArgs = Type.getArgumentTypes(vanilla.desc);
            Type returnType = Type.getReturnType(vanilla.desc);

            MethodNode wrapper = inner.apply(targetClass, newShares);
            MethodNode inner = move(targetClass, wrapper);

            // Our own desc shouldn't end with any Shares that we will be allocating, strip them.
            fixDesc(wrapper, sharesToAllocate.size());

            InsnList insns = new InsnList();
            allocateShares(sharesToAllocate, insns);

            // Load the params for the handler call;
            if (!isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            Bytecode.loadArgs(operationArgs, insns, isStatic ? 0 : 1);

            // Make the `Operation`:
            if (!isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            loadShares(newShares, insns);
            Type[] trailing =
                    newShares.stream()
                            .map(it -> it.getShareType().getImplType())
                            .toArray(Type[]::new);
            OperationUtils.makeOperation(
                    operationArgs, returnType, insns, !isStatic, trailing,
                    targetClass, operationType, inner.name,
                    (paramArrayIndex, loadArgs) -> {
                        InsnList call = new InsnList();
                        loadArgs.accept(call);
                        call.add(ASMUtils.getInvokeInstruction(targetClass, inner));
                        return call;
                    }
            );

            // Load any `@Share`s that the handler method wants:
            loadShares(shares, insns);

            insns.add(ASMUtils.getInvokeInstruction(targetClass, handler));
            insns.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));

            wrapper.instructions.add(insns);
            return wrapper;
        }

        private static void fixDesc(MethodNode wrapper, int shareCount) {
            Type[] argTypes = Type.getArgumentTypes(wrapper.desc);
            argTypes = ArrayUtils.subarray(argTypes, 0, argTypes.length - shareCount);
            wrapper.desc = Type.getMethodDescriptor(Type.getReturnType(wrapper.desc), argTypes);
        }

        private static void allocateShares(List<ShareInfo> sharesToAllocate, InsnList insns) {
            for (ShareInfo share : sharesToAllocate) {
                insns.add(share.initialize());
            }
        }

        private static void loadShares(Collection<ShareInfo> shares, InsnList insns) {
            for (ShareInfo share : shares) {
                insns.add(share.load());
            }
        }
    }

    protected static MethodNode move(ClassNode targetClass, MethodNode original) {
        MethodNode newMethod = new MethodNode(
                original.access,
                UniquenessHelper.getUniqueMethodName(
                        targetClass, original.name + "$mixinextras$wrapped"
                ),
                original.desc,
                null,
                null
        );
        Bytecode.setVisibility(newMethod, Bytecode.Visibility.PRIVATE);
        newMethod.instructions = original.instructions;
        newMethod.instructions.resetLabels();
        original.instructions = new InsnList();
        newMethod.tryCatchBlocks = original.tryCatchBlocks;
        original.tryCatchBlocks = null;
        newMethod.localVariables = original.localVariables;
        original.localVariables = null;
        stripLocalVariableReferences(original.visibleTypeAnnotations);
        stripLocalVariableReferences(original.invisibleTypeAnnotations);
        original.visibleLocalVariableAnnotations = null;
        original.invisibleLocalVariableAnnotations = null;
        targetClass.methods.add(newMethod);
        return newMethod;
    }

    private static void stripLocalVariableReferences(List<TypeAnnotationNode> nodes) {
        if (nodes == null) {
            return;
        }
        nodes.removeIf(
                it -> new TypeReference(it.typeRef).getSort() == TypeReference.LOCAL_VARIABLE
        );
    }
}
