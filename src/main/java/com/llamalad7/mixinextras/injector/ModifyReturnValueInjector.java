package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ModifyReturnValueInjector extends Injector {
    public ModifyReturnValueInjector(InjectionInfo info) {
        super(info, "@ModifyReturnValue");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        int opcode = node.getCurrentTarget().getOpcode();
        if (opcode < Opcodes.IRETURN || opcode >= Opcodes.RETURN) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s",
                    this.annotationType, target, this));
        }
        this.checkTargetModifiers(target, false);
        this.injectReturnValueModifier(target, node);
    }

    private void injectReturnValueModifier(Target target, InjectionNode node) {
        InjectorData handler = new InjectorData(target, "return value modifier");
        StackExtension stack = new StackExtension(target);
        InsnList insns = new InsnList();

        this.validateParams(handler, target.returnType, target.returnType);

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (target.returnType.getSize() == 2) {
                stack.extra(1);
                insns.add(new InsnNode(Opcodes.DUP_X2));
                insns.add(new InsnNode(Opcodes.POP));
            } else {
                insns.add(new InsnNode(Opcodes.SWAP));
            }
        }

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }

        stack.receiver(this.isStatic);
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);

        this.invokeHandler(insns);
        target.insertBefore(node, insns);
    }
}
