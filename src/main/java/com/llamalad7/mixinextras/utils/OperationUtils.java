package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationRuntime;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OperationUtils {
    public static void makeOperation(Type[] argTypes, Type returnType, InsnList insns, boolean virtual,
                                     Type[] trailingParams, ClassNode classNode, Type operationType,
                                     String name, OperationContents contents) {
        Type[] descriptorArgs = trailingParams;
        if (virtual) {
            // The receiver also needs to be a parameter in the INDY descriptor.
            descriptorArgs = ArrayUtils.add(descriptorArgs, 0, Type.getObjectType(classNode.name));
        }
        insns.add(new InvokeDynamicInsnNode(
                // The SAM method will be called `call`
                "call",
                // The generated lambda will implement `Operation` and have any trailing parameters bound to it
                Type.getMethodDescriptor(operationType, descriptorArgs),
                // We want to generate the impl with LMF
                ASMUtils.LMF_HANDLE,
                // The SAM method will take an array of args and return an `Object` (the return value of the wrapped call)
                Type.getMethodType(Type.getType(Object.class), Type.getType(Object[].class)),
                // The implementation method will be generated for us to handle array unpacking
                generateSyntheticBridge(argTypes, returnType, virtual, trailingParams, name, classNode, contents),
                // Specialization of the SAM signature
                Type.getMethodType(
                        ASMUtils.isPrimitive(returnType) ? Type.getObjectType(returnType == Type.VOID_TYPE ? "java/lang/Void" : Bytecode.getBoxingType(returnType)) : returnType,
                        Type.getType(Object[].class)
                )
        ));
    }

    private static Handle generateSyntheticBridge(Type[] argTypes, Type returnType, boolean virtual, Type[] boundParams,
                                                 String name, ClassNode classNode, OperationContents contents) {
        // The bridge method's args will consist of any bound parameters followed by an array

        MethodNode method = new MethodNode(
                ASM.API_VERSION,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | (virtual ? 0 : Opcodes.ACC_STATIC),
                UniquenessHelper.getUniqueMethodName(classNode, "mixinextras$bridge$" + name),
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
            add(contents.generate(paramArrayIndex, loadArgs));
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
        classNode.methods.add(method);

        return new Handle(
                virtual ? Opcodes.H_INVOKESPECIAL : Opcodes.H_INVOKESTATIC,
                classNode.name,
                method.name,
                method.desc,
                (classNode.access & Opcodes.ACC_INTERFACE) != 0
        );
    }

    @FunctionalInterface
    public interface OperationContents {
        InsnList generate(int paramArrayIndex, Consumer<InsnList> loadArgs);
    }
}
