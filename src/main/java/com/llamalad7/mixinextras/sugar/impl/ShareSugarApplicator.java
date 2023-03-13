package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
    private int localRefIndex;
    private boolean needsSetup;

    ShareSugarApplicator(InjectionInfo info, SugarParameter parameter) {
        super(info, parameter);
        this.id = mixin.getClassRef() + '_' + Annotations.getValue(sugar);
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
        Map<String, Integer> refIndices = TargetDecorations.getOrPut(target, "ShareSugar_LocalRefIndices", HashMap::new);
        needsSetup = !refIndices.containsKey(id);
        if (needsSetup) {
            localRefIndex = target.allocateLocal();
            refIndices.put(id, localRefIndex);
            target.addLocalVariable(localRefIndex, "sharedRef" + localRefIndex, paramType.getDescriptor());
        } else {
            localRefIndex = refIndices.get(id);
        }
    }

    @Override
    void inject(Target target, InjectionNodes.InjectionNode node) {
        if (needsSetup) {
            InsnList init = new InsnList();
            LocalRefUtils.generateWrapping(mixin, init, innerType, () -> init.add(new InsnNode(ASMUtils.getDummyOpcodeForType(innerType))));
            init.add(new VarInsnNode(Opcodes.ASTORE, localRefIndex));
            target.insertBefore(target.insns.getFirst(), init);
        }
        target.insns.insertBefore(node.getCurrentTarget(), new VarInsnNode(Opcodes.ALOAD, localRefIndex));
    }
}
