package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.impl.ShareInfo;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;

public class WrapMethodInjector extends Injector {
    private final Type operationType = MixinExtrasService.getInstance().changePackage(Operation.class, Type.getType(CompatibilityHelper.getAnnotation(info).desc), WrapMethod.class);
    private final List<ShareInfo> shares = new ArrayList<>();
    private boolean captureParams = true;

    public WrapMethodInjector(InjectionInfo info) {
        super(info, "@WrapMethod");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetModifiers(target, true);
        this.checkSignature(target);
        info.addCallbackInvocation(methodNode);
        WrapMethodApplicatorExtension.offerWrapper(target, methodNode, operationType, shares, captureParams);
    }

    private void checkSignature(Target target) {
        InjectorData handler = new InjectorData(target, "method wrapper");
        String description = String.format("%s %s %s from %s", this.annotationType, handler, this, CompatibilityHelper.getMixin(info));

        if (target.method.name.endsWith("init>")) {
            throw CompatibilityHelper.makeInvalidInjectionException(
                    info,
                    String.format(
                            "%s tried to target %s but targeting initializer methods is not supported!",
                            description, target
                    )
            );
        }

        handler.coerceReturnType = checkCoerce(-1, target.returnType, description, true);

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
            try {
                checkCoerce(argIndex, theirType, description, true);
            } catch (InvalidInjectionException e) {
                // if the first parameter is an 'Operation',
                // check from index 0 with param capture disabled
                if (methodArgs[0].equals(operationType)) {
                    captureParams = false;
                    argIndex = 0;
                    break;
                }
                throw e;
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
