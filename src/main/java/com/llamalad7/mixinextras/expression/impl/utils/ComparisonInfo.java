package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ComparisonInfo {
    private final int comparison;
    protected final AbstractInsnNode node;
    public final Type input;
    public final boolean jumpOnTrue;
    private final boolean needsExpanding;

    public ComparisonInfo(int comparison, AbstractInsnNode node, Type input, boolean needsExpanding, boolean jumpOnTrue) {
        this.comparison = comparison;
        this.node = node;
        this.input = input;
        this.jumpOnTrue = jumpOnTrue;
        this.needsExpanding = needsExpanding;
    }

    public void prepare(Target target, InjectionNode injectionNode) {
        if (needsExpanding) {
            target.insertBefore(injectionNode, new InsnList() {{
                add(new InsnNode(ASMUtils.getDummyOpcodeForType(input)));
            }});
        }
    }

    public int copyJump(InsnList insns) {
        return comparison;
    }

    public LabelNode getJumpTarget() {
        return ((JumpInsnNode) node).label;
    }

    public void cleanup(Target target) {
    }
}
