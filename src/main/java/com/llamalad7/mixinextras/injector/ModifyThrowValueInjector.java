package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ModifyThrowValueInjector extends Injector {
    public ModifyThrowValueInjector(InjectionInfo info) {
        super(info, "@ModifyThrowValue");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        if (node.getOriginalTarget().getOpcode() != Opcodes.ATHROW) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s",
                    this.annotationType, target, this));
        }
        this.checkTargetModifiers(target, false);
        this.injectThrowValueModifier(target, node);
    }

    private final static Type THROWABLE_TYPE = Type.getType(Throwable.class);

    private void injectThrowValueModifier(Target target, InjectionNode node) {
        InjectorData handler = new InjectorData(target, "throw value modifier");
        InsnList insns = new InsnList();

        this.validateParams(handler, THROWABLE_TYPE, THROWABLE_TYPE);

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insns.add(new InsnNode(Opcodes.SWAP));
        }

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }

        this.invokeHandler(insns);
        target.insertBefore(node, insns);
    }
}
