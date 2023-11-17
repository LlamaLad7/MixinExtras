package com.llamalad7.mixinextras.expression.impl.pool;

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

public class LocalPoolEntry implements PoolEntry {
    private final LocalVariableDiscriminator discriminator;
    private final InjectionInfo info;
    private final Type targetLocalType;
    private final boolean isArgsOnly;
    private final Target target;

    LocalPoolEntry(AnnotationNode local, InjectionInfo info, Target target) {
        discriminator = LocalVariableDiscriminator.parse(local);
        this.info = info;
        targetLocalType = Annotations.getValue(local, "type", Type.VOID_TYPE);
        isArgsOnly = Annotations.getValue(local, "argsOnly", (Boolean) false);
        this.target = target;
    }

    @Override
    public boolean matches(AbstractInsnNode insn) {
        if (!(insn instanceof VarInsnNode)) {
            return false;
        }
        VarInsnNode varInsn = (VarInsnNode) insn;
        if (insn.getOpcode() >= Opcodes.ISTORE && insn.getOpcode() <= Opcodes.ASTORE) {
            insn = insn.getNext();
        }
        Context context = InjectorUtils.getOrCreateLocalContext(target, target.addInjectionNode(insn), info, targetLocalType, isArgsOnly);
        if (discriminator.printLVT()) {
            InjectorUtils.printLocals(target, insn, context, discriminator, targetLocalType, isArgsOnly);
            info.addCallbackInvocation(info.getMethod());
        }
        int index;
        try {
            index = discriminator.findLocal(context);
        } catch (InvalidImplicitDiscriminatorException ignored) {
            return false;
        }
        return varInsn.var == index;
    }
}
