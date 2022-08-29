package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class WrapOperationInjector extends Injector {
    private static final Handle LMF_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            Bytecode.generateDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class),
            false
    );

    private final List<Pair<Target, InjectionNode>> queuedInjections = new ArrayList<>();

    public WrapOperationInjector(InjectionInfo info) {
        super(info, "@WrapOperation");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        // At this point we only want to store information for later.
        // Performing the actual injection here would allow other injectors to run after us on the same target,
        // which would cause issues due to this injector's extensive changes.
        this.queuedInjections.add(Pair.of(target, node));
    }

    void performInjections() {
        // Here is where we do the *actual* injections, as this is called after all ordinary injectors have run.
        for (Pair<Target, InjectionNode> injection : queuedInjections) {
            Target target = injection.getLeft();
            InjectionNode node = injection.getRight();
            this.checkTargetModifiers(target, false);
            this.checkNode(target, node);
            this.wrapOperation(target, node);
        }
    }

    private void checkNode(Target target, InjectionNode node) {
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        if (!(currentTarget instanceof MethodInsnNode || currentTarget instanceof FieldInsnNode)) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s annotation is targeting an invalid insn in %s in %s",
                            this.annotationType, target, this
                    ));
        }
    }

    private void wrapOperation(Target target, InjectionNode node) {
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        Type[] argTypes = getEffectiveArgTypes(currentTarget);
        Type returnType = getReturnType(currentTarget);
        InsnList insns = new InsnList();
        AbstractInsnNode champion = this.invokeHandler(target, node, argTypes, returnType, insns);
        target.wrapNode(currentTarget, champion, insns, new InsnList());
        target.insns.remove(currentTarget);
    }

    private AbstractInsnNode invokeHandler(Target target, InjectionNode node, Type[] argTypes, Type returnType, InsnList insns) {
        InjectorData handler = new InjectorData(target, "operation wrapper");
        boolean hasExtraThis = node.isReplaced() && node.getCurrentTarget().getOpcode() != Opcodes.INVOKESTATIC;
        if (hasExtraThis) {
            // We will add the extra `this` in ourselves in the generated bridge method later.
            argTypes = ArrayUtils.remove(argTypes, 0);
        }
        Type[] originalArgs = getEffectiveArgTypes(node.getOriginalTarget());
        this.validateParams(handler, returnType, ArrayUtils.add(originalArgs, Type.getType(Operation.class)));

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
        this.makeSupplier(originalArgs, returnType, node, insns, hasExtraThis, ArrayUtils.subarray(argTypes, originalArgs.length, argTypes.length));
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }
        return super.invokeHandler(insns);
    }

    private void makeSupplier(Type[] argTypes, Type returnType, InjectionNode node, InsnList insns, boolean hasExtraThis, Type[] trailingParams) {
        Type[] descriptorArgs = trailingParams;
        if (hasExtraThis) {
            // The receiver also needs to be a parameter in the INDY descriptor.
            descriptorArgs = ArrayUtils.add(descriptorArgs, 0, Type.getObjectType(this.classNode.name));
        }
        insns.add(new InvokeDynamicInsnNode(
                // The SAM method will be called `call`
                "call",
                // The generated lambda will implement `Operation` and have any trailing parameters bound to it
                Bytecode.generateDescriptor(Operation.class, (Object[]) descriptorArgs),
                // We want to generate the impl with LMF
                LMF_HANDLE,
                // The SAM method will take an array of args and return an `Object` (the return value of the wrapped call)
                Type.getMethodType(Type.getType(Object.class), Type.getType(Object[].class)),
                // The implementation method will be generated for us to handle array unpacking
                generateSyntheticBridge(node, argTypes, hasExtraThis, trailingParams),
                // Specialization of the SAM signature
                Type.getMethodType(
                        returnType.getDescriptor().length() == 1 ? Type.getObjectType(returnType == Type.VOID_TYPE ? "java/lang/Void" : Bytecode.getBoxingType(returnType)) : returnType,
                        Type.getType(Object[].class)
                )
        ));
    }

    private Handle generateSyntheticBridge(InjectionNode target, Type[] argTypes, boolean virtual, Type[] boundParams) {
        // The bridge method's args will consist of any bound parameters followed by an array

        Type returnType = getReturnType(target.getCurrentTarget());

        MethodNode method = new MethodNode(
                ASM.API_VERSION,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | (virtual ? 0 : Opcodes.ACC_STATIC),
                "mixinextras$bridge$" + UUID.randomUUID() + '$' + getName(target.getCurrentTarget()),
                Bytecode.generateDescriptor(
                        returnType.getDescriptor().length() == 1 ?
                                Type.getObjectType(
                                        returnType == Type.VOID_TYPE ? "java/lang/Void" : Bytecode.getBoxingType(returnType)
                                ) : returnType,
                        ArrayUtils.add(boundParams, Type.getType(Object[].class))),
                null, null
        );
        method.instructions = new InsnList() {{
            if (virtual) {
                add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            int paramArrayIndex = virtual ? 1 : 0;
            for (Type boundParamType : boundParams) {
                paramArrayIndex += boundParamType.getSize();
            }
            add(new VarInsnNode(Opcodes.ALOAD, paramArrayIndex));
            for (int i = 0; i < argTypes.length; i++) {
                Type argType = argTypes[i];
                add(new InsnNode(Opcodes.DUP));
                // I'm assuming a wrapped method won't have more than 127 args...
                add(new IntInsnNode(Opcodes.BIPUSH, i));
                add(new InsnNode(Opcodes.AALOAD));
                if (argType.getDescriptor().length() == 1) {
                    // Primitive, cast and unbox
                    add(new TypeInsnNode(Opcodes.CHECKCAST, Bytecode.getBoxingType(argType)));
                    add(new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            Bytecode.getBoxingType(argType),
                            Bytecode.getUnboxingMethod(argType),
                            Type.getMethodDescriptor(argType),
                            false
                    ));
                } else {
                    // Object type, just cast
                    add(new TypeInsnNode(Opcodes.CHECKCAST, argType.getInternalName()));
                }
                // Swap to get the array back on the top of the stack
                if (argType.getSize() == 2) {
                    add(new InsnNode(Opcodes.DUP2_X1));
                    add(new InsnNode(Opcodes.POP2));
                } else {
                    add(new InsnNode(Opcodes.SWAP));
                }
            }
            // We have one dangling array reference, get rid of it
            add(new InsnNode(Opcodes.POP));
            int boundParamIndex = virtual ? 1 : 0;
            for (Type boundParamType : boundParams) {
                add(new VarInsnNode(boundParamType.getOpcode(Opcodes.ILOAD), boundParamIndex));
                boundParamIndex += boundParamType.getSize();
            }
            add(target.getCurrentTarget().clone(Collections.emptyMap()));
            if (returnType == Type.VOID_TYPE) {
                add(new InsnNode(Opcodes.ACONST_NULL));
                add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Void"));
            } else if (returnType.getDescriptor().length() == 1) {
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

    private Type getReturnType(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
            return Type.getReturnType(methodInsnNode.desc);
        }

        if (node instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
            if (fieldInsnNode.getOpcode() == Opcodes.GETFIELD || fieldInsnNode.getOpcode() == Opcodes.GETSTATIC) {
                return Type.getType(fieldInsnNode.desc);
            }
            return Type.VOID_TYPE;
        }

        return null;
    }

    private Type[] getEffectiveArgTypes(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = ((MethodInsnNode) node);
            return node.getOpcode() == Opcodes.INVOKESTATIC ?
                    Type.getArgumentTypes(methodInsnNode.desc) :
                    ArrayUtils.addAll(new Type[]{Type.getObjectType(methodInsnNode.owner)}, Type.getArgumentTypes(methodInsnNode.desc));
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

        throw new UnsupportedOperationException();
    }

    private String getName(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            return ((MethodInsnNode) node).name;
        }
        if (node instanceof FieldInsnNode) {
            return ((FieldInsnNode) node).name;
        }

        throw new UnsupportedOperationException();
    }
}
