package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefClassGenerator;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.List;

public class ShareType {
    private final Type innerType;

    public ShareType(Type innerType) {
        this.innerType = innerType;
    }

    public Type getInnerType() {
        return innerType;
    }

    public Type getImplType() {
        return Type.getObjectType(LocalRefClassGenerator.getForType(innerType));
    }

    public InsnList initialize(int lvtIndex) {
        InsnList init = new InsnList();
        LocalRefUtils.generateNew(init, innerType);
        init.add(new VarInsnNode(Opcodes.ASTORE, lvtIndex));
        init.add(new VarInsnNode(Opcodes.ALOAD, lvtIndex));
        init.add(new InsnNode(ASMUtils.getDummyOpcodeForType(innerType)));
        LocalRefUtils.generateInitialization(init, innerType);
        return init;
    }

    public void addToLvt(Target target, int lvtIndex) {
        LabelNode start = new LabelNode();
        target.insns.insert(start);
        LabelNode end = new LabelNode();
        target.insns.add(end);
        Type implType = Type.getObjectType(LocalRefClassGenerator.getForType(innerType));

        target.addLocalVariable(lvtIndex, "sharedRef" + lvtIndex, implType.getDescriptor());
        List<LocalVariableNode> lvt = target.method.localVariables;
        LocalVariableNode newVar = lvt.get(lvt.size() - 1);
        newVar.start = start;
        newVar.end = end;
    }
}
