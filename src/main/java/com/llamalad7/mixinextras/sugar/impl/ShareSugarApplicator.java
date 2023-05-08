package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.HashMap;
import java.util.Map;

class ShareSugarApplicator extends SugarApplicator {
    private final String id;
    private Type innerType;

    ShareSugarApplicator(InjectionInfo info, SugarParameter parameter) {
        super(info, parameter);
        this.id = mixin.getClassRef() + ':' + Annotations.getValue(sugar);
    }

    @Override
    void validate(Target target, InjectionNodes.InjectionNode node) {
        innerType = LocalRefUtils.getTargetType(paramType, Type.getType(Object.class));
        if (innerType == paramType) {
            throw new SugarApplicationException("@Share parameter must be some variation of LocalRef.");
        }
    }

    @Override
    void prepare(Target target, InjectionNodes.InjectionNode node) {
    }

    @Override
    void inject(Target target, InjectionNodes.InjectionNode node) {
        Map<String, Integer> refIndices = TargetDecorations.getOrPut(target, "ShareSugar_LocalRefIndices", HashMap::new);
        int localRefIndex;
        if (!refIndices.containsKey(id)) {
            localRefIndex = target.allocateLocal();
            refIndices.put(id, localRefIndex);
            target.addLocalVariable(localRefIndex, "sharedRef" + localRefIndex, paramType.getDescriptor());
            InsnList init = new InsnList();
            LocalRefUtils.generateWrapping(init, innerType, () -> init.add(new InsnNode(ASMUtils.getDummyOpcodeForType(innerType))));
            init.add(new VarInsnNode(Opcodes.ASTORE, localRefIndex));
            addToStart(target, init);
        } else {
            localRefIndex = refIndices.get(id);
        }
        target.insns.insertBefore(node.getCurrentTarget(), new VarInsnNode(Opcodes.ALOAD, localRefIndex));
    }

    private void addToStart(Target target, InsnList insns) {
        for (AbstractInsnNode existing : target) {
            if (existing.getOpcode() != -1) {
                target.insns.insertBefore(existing, insns);
                return;
            }
        }
    }
}
