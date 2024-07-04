package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.modify.InvalidImplicitDiscriminatorException;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

public class LocalDef implements MemberDefinition {
    private final LocalVariableDiscriminator discriminator;
    private final InjectionInfo info;
    private final Type targetLocalType;
    private final boolean isArgsOnly;
    private final Target target;

    LocalDef(AnnotationNode local, InjectionInfo info, Target target) {
        discriminator = LocalVariableDiscriminator.parse(local);
        this.info = info;
        targetLocalType = Annotations.getValue(local, "type", Type.VOID_TYPE);
        isArgsOnly = Annotations.getValue(local, "argsOnly", (Boolean) false);
        this.target = target;
    }

    @Override
    public boolean matches(FlowValue node) {
        AbstractInsnNode virtualInsn = node.getInsn();
        if (!(virtualInsn instanceof VarInsnNode)) {
            return false;
        }
        VarInsnNode virtualVarInsn = (VarInsnNode) virtualInsn;
        AbstractInsnNode actualInsn = InsnExpander.getRepresentative(node);
        if (virtualVarInsn.getOpcode() >= Opcodes.ISTORE && virtualVarInsn.getOpcode() <= Opcodes.ASTORE) {
            actualInsn = actualInsn.getNext();
        }
        Context context = InjectorUtils.getOrCreateLocalContext(target, target.addInjectionNode(actualInsn), info, targetLocalType, isArgsOnly);
        if (discriminator.printLVT()) {
            InjectorUtils.printLocals(target, actualInsn, context, discriminator, targetLocalType, isArgsOnly);
            info.addCallbackInvocation(info.getMethod());
        }
        int index;
        try {
            index = discriminator.findLocal(context);
        } catch (InvalidImplicitDiscriminatorException ignored) {
            return false;
        }
        return virtualVarInsn.var == index;
    }
}
