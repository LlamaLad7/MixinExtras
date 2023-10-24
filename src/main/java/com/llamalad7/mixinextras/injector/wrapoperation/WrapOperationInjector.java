package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.utils.*;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class WrapOperationInjector extends Injector {
    private static final Handle LMF_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            Bytecode.generateDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class),
            false
    );
    private static final String NPE = Type.getInternalName(NullPointerException.class);

    private final Type operationType = MixinExtrasService.getInstance().changePackage(Operation.class, Type.getType(CompatibilityHelper.getAnnotation(info).desc), WrapOperation.class);

    public WrapOperationInjector(InjectionInfo info) {
        super(info, "@WrapOperation");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetModifiers(target, false);
        this.checkNode(target, node);
        this.wrapOperation(target, node);
    }

    private void checkNode(Target target, InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        if (currentTarget instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = ((MethodInsnNode) currentTarget);
            if (methodInsnNode.name.equals("<init>")) {
                throw CompatibilityHelper.makeInvalidInjectionException(
                        this.info,
                        String.format(
                                "%s annotation is trying to target an <init> call in %s in %s! If this is an instantiation, target the NEW instead.",
                                this.annotationType, target, this
                        )
                );
            }
            return;
        }
        if (!(currentTarget instanceof FieldInsnNode ||
                originalTarget.getOpcode() == Opcodes.INSTANCEOF ||
                originalTarget.getOpcode() == Opcodes.NEW)) {
            throw CompatibilityHelper.makeInvalidInjectionException(
                    this.info,
                    String.format(
                            "%s annotation is targeting an invalid insn in %s in %s",
                            this.annotationType, target, this
                    )
            );
        }
    }

    private void wrapOperation(Target target, InjectionNode node) {
        AbstractInsnNode initialTarget = node.getCurrentTarget();
        InsnList insns = new InsnList();
        boolean isNew = initialTarget.getOpcode() == Opcodes.NEW;
        boolean isDupedNew = InjectorUtils.isDupedNew(node);
        if (isNew) {
            node.decorate(Decorations.WRAPPED, true);
            node = target.addInjectionNode(ASMUtils.findInitNodeFor(target, (TypeInsnNode) initialTarget));
        }
        Type[] argTypes = getCurrentArgTypes(node);
        Type returnType = getReturnType(node);
        AbstractInsnNode champion = this.invokeHandler(target, node, argTypes, returnType, insns);
        if (isDupedNew) {
            // We replace the `NEW` object with a `null` reference, for convenience:
            target.insns.set(initialTarget, new InsnNode(Opcodes.ACONST_NULL));
            // Then, after invoking the handler, we have 2 null references and the actual new object on the stack.
            // We want to get rid of the null references:
            insns.add(new InsnNode(Opcodes.DUP_X2));
            insns.add(new InsnNode(Opcodes.POP));
            insns.add(new InsnNode(Opcodes.POP));
            insns.add(new InsnNode(Opcodes.POP));
        } else if (isNew) {
            // "Get rid" of the `NEW` instruction:
            target.insns.set(initialTarget, new InsnNode(Opcodes.NOP));
            // And pop the result of the wrapper since it isn't used:
            insns.add(new InsnNode(Opcodes.POP));
        }
        AbstractInsnNode finalTarget = node.getCurrentTarget();
        target.wrapNode(finalTarget, champion, insns, new InsnList());
        if (isNew) {
            // We've already replaced the <init> call, but we want to replace the NEW instruction.
            target.getInjectionNode(initialTarget).replace(champion);
        }
        node.decorate(Decorations.WRAPPED, true);
        target.insns.remove(finalTarget);
    }

    private AbstractInsnNode invokeHandler(Target target, InjectionNode node, Type[] argTypes, Type returnType, InsnList insns) {
        InjectorData handler = new InjectorData(target, "operation wrapper");
        boolean hasExtraThis = node.isReplaced() && node.getCurrentTarget().getOpcode() != Opcodes.INVOKESTATIC;
        if (hasExtraThis) {
            // We will add the extra `this` in ourselves in the generated bridge method later.
            argTypes = ArrayUtils.remove(argTypes, 0);
        }
        Type[] originalArgs = getOriginalArgTypes(node);
        this.validateParams(handler, returnType, ArrayUtils.add(originalArgs, operationType));

        // Store *all* the args, including ones added by redirectors and previous operation wrappers.
        // Excess ones will be bound to the lambda.
        int[] argMap = this.storeArgs(target, argTypes, insns, 0);
        if (hasExtraThis) {
            insns.add(new InsnNode(Opcodes.POP));
        }

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        // Push the args which should go to the handler method.
        this.pushArgs(this.methodArgs, insns, argMap, 0, originalArgs.length);
        // Push the receiver of the bridge method (if applicable) and any captured parameters it will need.
        if (hasExtraThis) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        this.pushArgs(argTypes, insns, argMap, originalArgs.length, argMap.length);
        // The trailing params are any arguments which come after the original args, and should therefore be bound to the lambda.
        this.makeSupplier(target, originalArgs, returnType, node, insns, hasExtraThis, ArrayUtils.subarray(argTypes, originalArgs.length, argTypes.length));
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }
        AbstractInsnNode champion = super.invokeHandler(insns);

        if (InjectorUtils.isDynamicInstanceofRedirect(node)) {
            // At this point, we have the boolean result and the checked object on the stack.
            // The object was DUPed by RedirectInjector before the handler was called, so removing the DUP is too risky.
            // Instead, we simply pop the excess object.
            insns.add(new InsnNode(Opcodes.SWAP));
            insns.add(new InsnNode(Opcodes.POP));
        }
        return champion;
    }

    private void makeSupplier(Target target, Type[] argTypes, Type returnType, InjectionNode node, InsnList insns, boolean hasExtraThis, Type[] trailingParams) {
        Type[] descriptorArgs = trailingParams;
        if (hasExtraThis) {
            // The receiver also needs to be a parameter in the INDY descriptor.
            descriptorArgs = ArrayUtils.add(descriptorArgs, 0, Type.getObjectType(this.classNode.name));
        }
        insns.add(new InvokeDynamicInsnNode(
                // The SAM method will be called `call`
                "call",
                // The generated lambda will implement `Operation` and have any trailing parameters bound to it
                Bytecode.generateDescriptor(operationType, (Object[]) descriptorArgs),
                // We want to generate the impl with LMF
                LMF_HANDLE,
                // The SAM method will take an array of args and return an `Object` (the return value of the wrapped call)
                Type.getMethodType(Type.getType(Object.class), Type.getType(Object[].class)),
                // The implementation method will be generated for us to handle array unpacking
                generateSyntheticBridge(target, node, argTypes, returnType, hasExtraThis, trailingParams),
                // Specialization of the SAM signature
                Type.getMethodType(
                        ASMUtils.isPrimitive(returnType) ? Type.getObjectType(returnType == Type.VOID_TYPE ? "java/lang/Void" : Bytecode.getBoxingType(returnType)) : returnType,
                        Type.getType(Object[].class)
                )
        ));
    }

    private Handle generateSyntheticBridge(Target target, InjectionNode node, Type[] argTypes, Type returnType, boolean virtual, Type[] boundParams) {
        // The bridge method's args will consist of any bound parameters followed by an array

        MethodNode method = new MethodNode(
                ASM.API_VERSION,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | (virtual ? 0 : Opcodes.ACC_STATIC),
                UniquenessHelper.getUniqueMethodName(classNode, "mixinextras$bridge$" + getName(node.getCurrentTarget())),
                Bytecode.generateDescriptor(
                        ASMUtils.isPrimitive(returnType) ?
                                Type.getObjectType(
                                        returnType == Type.VOID_TYPE ? "java/lang/Void" : Bytecode.getBoxingType(returnType)
                                ) : returnType,
                        ArrayUtils.add(boundParams, Type.getType(Object[].class))),
                null, null
        );
        method.instructions = new InsnList() {{
            // Bound params have to come first.
            int paramArrayIndex = Arrays.stream(boundParams).mapToInt(Type::getSize).sum() + (virtual ? 1 : 0);
            // Provide a user-friendly error if the wrong args are passed.
            add(new VarInsnNode(Opcodes.ALOAD, paramArrayIndex));
            add(new IntInsnNode(Opcodes.BIPUSH, argTypes.length));
            add(new LdcInsnNode(Arrays.stream(argTypes).map(Type::getClassName).collect(Collectors.joining(", ", "[", "]"))));
            add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(WrapOperationRuntime.class),
                    "checkArgumentCount",
                    Bytecode.generateDescriptor(void.class, Object[].class, int.class, String.class),
                    false
            ));

            if (virtual) {
                add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            Consumer<InsnList> loadArgs = insns -> {
                insns.add(new VarInsnNode(Opcodes.ALOAD, paramArrayIndex));
                for (int i = 0; i < argTypes.length; i++) {
                    Type argType = argTypes[i];
                    insns.add(new InsnNode(Opcodes.DUP));
                    // I'm assuming a wrapped method won't have more than 127 args...
                    insns.add(new IntInsnNode(Opcodes.BIPUSH, i));
                    insns.add(new InsnNode(Opcodes.AALOAD));
                    if (ASMUtils.isPrimitive(argType)) {
                        // Primitive, cast and unbox
                        insns.add(new TypeInsnNode(Opcodes.CHECKCAST, Bytecode.getBoxingType(argType)));
                        insns.add(new MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                Bytecode.getBoxingType(argType),
                                Bytecode.getUnboxingMethod(argType),
                                Type.getMethodDescriptor(argType),
                                false
                        ));
                    } else {
                        // Object type, just cast
                        insns.add(new TypeInsnNode(Opcodes.CHECKCAST, argType.getInternalName()));
                    }
                    // Swap to get the array back on the top of the stack
                    if (argType.getSize() == 2) {
                        insns.add(new InsnNode(Opcodes.DUP2_X1));
                        insns.add(new InsnNode(Opcodes.POP2));
                    } else {
                        insns.add(new InsnNode(Opcodes.SWAP));
                    }
                }
                // We have one dangling array reference, get rid of it
                insns.add(new InsnNode(Opcodes.POP));
                // Next load the bound params:
                int boundParamIndex = virtual ? 1 : 0;
                for (Type boundParamType : boundParams) {
                    insns.add(new VarInsnNode(boundParamType.getOpcode(Opcodes.ILOAD), boundParamIndex));
                    boundParamIndex += boundParamType.getSize();
                }
            };
            add(copyNode(node, paramArrayIndex, target, loadArgs));
            if (returnType == Type.VOID_TYPE) {
                add(new InsnNode(Opcodes.ACONST_NULL));
                add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Void"));
            } else if (ASMUtils.isPrimitive(returnType)) {
                // Primitive, needs boxing
                add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Bytecode.getBoxingType(returnType),
                        "valueOf",
                        Bytecode.generateDescriptor(Type.getObjectType(Bytecode.getBoxingType(returnType)), returnType),
                        false
                ));
            }
            add(new InsnNode(Opcodes.ARETURN));
        }};
        this.classNode.methods.add(method);

        return new Handle(
                virtual ? Opcodes.H_INVOKESPECIAL : Opcodes.H_INVOKESTATIC,
                this.classNode.name,
                method.name,
                method.desc,
                (this.classNode.access & Opcodes.ACC_INTERFACE) != 0
        );
    }

    private InsnList copyNode(InjectionNode node, int paramArrayIndex, Target target, Consumer<InsnList> loadArgs) {
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        InsnList insns = new InsnList();
        if (currentTarget instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) currentTarget;
            if (methodInsnNode.name.equals("<init>")) {
                insns.add(new TypeInsnNode(Opcodes.NEW, methodInsnNode.owner));
                insns.add(new InsnNode(Opcodes.DUP));
            }
        }

        loadArgs.accept(insns);
        insns.add(currentTarget.clone(Collections.emptyMap()));

        if (InjectorUtils.isDynamicInstanceofRedirect(node)) {
            // We have a Class object and need to get it back to a boolean using the first element of the lambda args.
            // The code added by RedirectInjector expects a reference to the checked object already on the stack, so we
            // load the first and only lambda arg, and then swap it with the Class<?> result from the redirector.
            insns.add(new VarInsnNode(Opcodes.ALOAD, paramArrayIndex));
            insns.add(new InsnNode(Opcodes.ICONST_0));
            insns.add(new InsnNode(Opcodes.AALOAD));
            insns.add(new InsnNode(Opcodes.SWAP));
            // Sanity checks for the instructions being moved based on what I expect RedirectInjector to have added.
            checkAndMoveNodes(
                    target.insns,
                    insns,
                    currentTarget,
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNONNULL,
                    it -> it.getOpcode() == Opcodes.NEW && ((TypeInsnNode) it).desc.equals(NPE),
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it instanceof LdcInsnNode && ((LdcInsnNode) it).cst instanceof String,
                    it -> it.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) it).owner.equals(NPE),
                    it -> it.getOpcode() == Opcodes.ATHROW,
                    it -> it instanceof LabelNode,
                    it -> it.getOpcode() == Opcodes.SWAP,
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNULL,
                    it -> it.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) it).name.equals("getClass"),
                    it -> it.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) it).name.equals("isAssignableFrom"),
                    it -> it.getOpcode() == Opcodes.GOTO,
                    it -> it instanceof LabelNode,
                    it -> it.getOpcode() == Opcodes.POP,
                    it -> it.getOpcode() == Opcodes.POP,
                    it -> it.getOpcode() == Opcodes.ICONST_0,
                    it -> it instanceof LabelNode
            );
        }

        if (InjectorUtils.isDupedFactoryRedirect(node)) wrap:{
            AbstractInsnNode ldc = InjectorUtils.findFactoryRedirectThrowString(target, currentTarget);
            if (ldc == null) break wrap;
            // We need to encompass the null check added by RedirectInjector.
            checkAndMoveNodes(
                    target.insns,
                    insns,
                    currentTarget,
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNONNULL,
                    it -> it.getOpcode() == Opcodes.NEW && ((TypeInsnNode) it).desc.equals(NPE),
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it == ldc,
                    it -> it.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) it).name.equals("<init>"),
                    it -> it.getOpcode() == Opcodes.ATHROW,
                    it -> it instanceof LabelNode
            );
        }

        return insns;
    }

    @SafeVarargs
    private final void checkAndMoveNodes(InsnList from, InsnList to, AbstractInsnNode node, Predicate<AbstractInsnNode>... predicates) {
        AbstractInsnNode current = node.getNext();
        for (Predicate<AbstractInsnNode> predicate : predicates) {
            if (!predicate.test(current)) {
                throw new AssertionError("Failed assertion when wrapping instructions. Please inform LlamaLad7!");
            }
            AbstractInsnNode old = current;
            do {
                current = current.getNext();
            } while (current instanceof FrameNode);
            from.remove(old);
            to.add(old);
        }
    }

    private Type getReturnType(InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        AbstractInsnNode currentTarget = node.getCurrentTarget();

        if (originalTarget.getOpcode() == Opcodes.INSTANCEOF) {
            return Type.BOOLEAN_TYPE;
        }

        if (currentTarget instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) currentTarget;
            if (methodInsnNode.name.equals("<init>")) {
                return Type.getObjectType(methodInsnNode.owner);
            }
            return Type.getReturnType(methodInsnNode.desc);
        }
        if (currentTarget instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) currentTarget;
            if (fieldInsnNode.getOpcode() == Opcodes.GETFIELD || fieldInsnNode.getOpcode() == Opcodes.GETSTATIC) {
                return Type.getType(fieldInsnNode.desc);
            }
            return Type.VOID_TYPE;
        }

        throw new UnsupportedOperationException();
    }

    private Type[] getOriginalArgTypes(InjectionNode node) {
        if (node.hasDecoration(Decorations.NEW_ARG_TYPES)) {
            return node.getDecoration(Decorations.NEW_ARG_TYPES);
        }
        return getEffectiveArgTypes(node.getOriginalTarget());
    }

    private Type[] getCurrentArgTypes(InjectionNode node) {
        return getEffectiveArgTypes(node.getCurrentTarget());
    }

    private Type[] getEffectiveArgTypes(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = ((MethodInsnNode) node);
            Type[] args = Type.getArgumentTypes(methodInsnNode.desc);
            if (methodInsnNode.name.equals("<init>")) {
                // The receiver isn't truly an arg because we'll make it ourselves.
                return args;
            }
            switch (methodInsnNode.getOpcode()) {
                case Opcodes.INVOKESTATIC:
                    break;
                case Opcodes.INVOKESPECIAL:
                    args = ArrayUtils.add(args, 0, Type.getObjectType(classNode.name));
                    break;
                default:
                    args = ArrayUtils.add(args, 0, Type.getObjectType(methodInsnNode.owner));
            }
            return args;
        }
        if (node instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = ((FieldInsnNode) node);
            switch (fieldInsnNode.getOpcode()) {
                case Opcodes.GETFIELD:
                    return new Type[]{Type.getObjectType(fieldInsnNode.owner)};
                case Opcodes.PUTFIELD:
                    return new Type[]{Type.getObjectType(fieldInsnNode.owner), Type.getType(fieldInsnNode.desc)};
                case Opcodes.GETSTATIC:
                    return new Type[0];
                case Opcodes.PUTSTATIC:
                    return new Type[]{Type.getType(fieldInsnNode.desc)};
            }
        }
        if (node.getOpcode() == Opcodes.INSTANCEOF) {
            return new Type[]{Type.getType(Object.class)};
        }

        throw new UnsupportedOperationException();
    }

    private String getName(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
            if (methodInsnNode.name.equals("<init>")) {
                String desc = methodInsnNode.owner;
                return "new" + desc.substring(desc.lastIndexOf('/') + 1);
            }
            return ((MethodInsnNode) node).name;
        }
        if (node instanceof FieldInsnNode) {
            return ((FieldInsnNode) node).name;
        }
        if (node.getOpcode() == Opcodes.INSTANCEOF) {
            String desc = ((TypeInsnNode) node).desc;
            return "instanceof" + desc.substring(desc.lastIndexOf('/') + 1);
        }

        throw new UnsupportedOperationException();
    }
}
