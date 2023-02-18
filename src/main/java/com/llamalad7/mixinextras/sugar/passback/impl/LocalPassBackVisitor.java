package com.llamalad7.mixinextras.sugar.passback.impl;

import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.util.Annotations;

class LocalPassBackVisitor extends PassBackVisitor {
    private final boolean isMutable;

    LocalPassBackVisitor(SugarParameter parameter) {
        super(parameter);
        this.isMutable = Annotations.getValue(parameter.sugar, "mutable", (Boolean) false);
    }

    @Override
    public boolean isRequired() {
        return isMutable;
    }

    @Override
    public void visit(PassBackInfo info) {
        int opcode = parameter.type.getOpcode(Opcodes.ILOAD);
        int lvtIndex = parameter.lvtIndex;
        info.addValue(lvtIndex, parameter.type, insns -> {
            insns.add(new VarInsnNode(opcode, lvtIndex));
        });
    }
}
