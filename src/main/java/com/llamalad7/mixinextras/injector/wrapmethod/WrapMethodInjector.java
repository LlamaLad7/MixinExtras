package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.impl.ShareInfo;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.OperationUtils;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;

public class WrapMethodInjector extends Injector {
    private final Type operationType = MixinExtrasService.getInstance().changePackage(Operation.class, Type.getType(CompatibilityHelper.getAnnotation(info).desc), WrapMethod.class);
    private final List<ShareInfo> shares = new ArrayList<>();

    public WrapMethodInjector(InjectionInfo info) {
        super(info, "@WrapMethod");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetModifiers(target, true);
        this.checkSignature(target);
        info.addCallbackInvocation(methodNode);
        WrapMethodApplicatorExtension.offerWrapper(target, methodNode, operationType, shares);
    }

    private void injectWrapperCall(Target target) {
        InsnList insns = new InsnList();
        MethodNode original = moveOriginal(target.method);

        if (!this.isStatic) {
            // For the handler invocation
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            // To be bound to the operation
            insns.add(new InsnNode(Opcodes.DUP));
        }

        this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, target.arguments.length);

        OperationUtils.makeOperation(
                target.arguments, target.returnType, insns, !target.isStatic, new Type[0],
                classNode, operationType, target.method.name,
                (paramArrayIndex, loadArgs) -> {
                    InsnList call = new InsnList();
                    loadArgs.accept(call);
                    call.add(ASMUtils.getInvokeInstruction(classNode, original));
                    return call;
                }
        );

        this.invokeHandler(insns);
        insns.add(new InsnNode(target.returnType.getOpcode(Opcodes.IRETURN)));

        target.insns.add(insns);
    }

    private MethodNode moveOriginal(MethodNode original) {
        MethodNode newMethod = new MethodNode(
                original.access,
                UniquenessHelper.getUniqueMethodName(
                        classNode, original.name + "$mixinextras$wrapped"
                ),
                original.desc,
                original.signature,
                original.exceptions.toArray(new String[0])
        );
        original.accept(newMethod);
        original.instructions.clear();
        original.tryCatchBlocks = null;
        original.localVariables = null;
        classNode.methods.add(newMethod);
        return newMethod;
    }

    private void checkSignature(Target target) {
        InjectorData handler = new InjectorData(target, "method wrapper");
        String description = String.format("%s %s %s from %s", this.annotationType, handler, this, CompatibilityHelper.getMixin(info));

        if (!returnType.equals(target.returnType)) {
            throw CompatibilityHelper.makeInvalidInjectionException(
                    info,
                    String.format(
                            "%s targeting %s has an incorrect return type Expected %s but got %s!",
                            description, target, target.returnType, returnType
                    )
            );
        }
        int argIndex = 0;
        for (; argIndex < target.arguments.length; argIndex++) {
            Type theirType = target.arguments[argIndex];
            if (argIndex >= methodArgs.length) {
                throw CompatibilityHelper.makeInvalidInjectionException(
                        info,
                        String.format(
                                "%s targeting %s doesn't have enough parameters!",
                                description, target
                        )
                );
            }
            Type ourType = methodArgs[argIndex];

            if (!ourType.equals(theirType)) {
                throw CompatibilityHelper.makeInvalidInjectionException(
                        info,
                        String.format(
                                "%s targeting %s has a mismatching param at index %s! Expected %s but got %s",
                                description, target, argIndex, theirType, ourType
                        )
                );
            }
        }
        if (argIndex >= methodArgs.length || !methodArgs[argIndex++].equals(operationType)) {
            throw CompatibilityHelper.makeInvalidInjectionException(
                    info,
                    String.format(
                            "%s targeting %s is missing Operation parameter!",
                            description, target
                    )
            );
        }
        List<AnnotationNode> sugars = Annotations.getValue(CompatibilityHelper.getAnnotation(info), "sugars");
        if (sugars != null) {
            for (int i = 0; i < argIndex; i++) {
                AnnotationNode sugar = sugars.get(i);
                if (MixinExtrasService.getInstance().isClassOwned(Type.getType(sugar.desc).getClassName())) {
                    throw CompatibilityHelper.makeInvalidInjectionException(
                            info,
                            String.format(
                                    "%s targeting %s has sugar on a non-trailing param which is not allowed!",
                                    description, target
                            )
                    );
                }
            }
        }
        for (; argIndex < methodArgs.length; argIndex++) {
            ShareInfo share;
            if (sugars == null ||
                    (share = ShareInfo.getOrCreate(
                            target,
                            sugars.get(argIndex),
                            methodArgs[argIndex],
                            CompatibilityHelper.getMixin(info).getMixin(),
                            null
                    )) == null) {
                throw CompatibilityHelper.makeInvalidInjectionException(
                        info,
                        String.format(
                                "%s targeting %s has an excess parameter at index %s!",
                                description, target, argIndex
                        )
                );
            }
            shares.add(share);
        }
    }
}
