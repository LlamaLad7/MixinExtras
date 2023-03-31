package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.HashMap;
import java.util.List;
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
            LabelNode start = new LabelNode();
            LabelNode end = new LabelNode();
            target.addLocalVariable(localRefIndex, "sharedRef" + localRefIndex, paramType.getDescriptor());
            List<LocalVariableNode> lvt = target.method.localVariables;
            LocalVariableNode newVar = lvt.get(lvt.size() - 1);
            newVar.start = start;
            newVar.end = end;
            target.insns.insert(start);
            target.insns.add(end);
            InsnList init = new InsnList();
            LocalRefUtils.generateNew(init, innerType);
            init.add(new VarInsnNode(Opcodes.ASTORE, localRefIndex));
            init.add(new VarInsnNode(Opcodes.ALOAD, localRefIndex));
            init.add(new InsnNode(ASMUtils.getDummyOpcodeForType(innerType)));
            LocalRefUtils.generateInitialization(init, innerType);
            target.insns.insert(start, init);
        } else {
            localRefIndex = refIndices.get(id);
        }
        target.insns.insertBefore(node.getCurrentTarget(), new VarInsnNode(Opcodes.ALOAD, localRefIndex));
    }

}
