package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.function.BiConsumer;

public class ComparisonInfo {
    private final int comparison;
    protected final AbstractInsnNode node;
    public final Type input;
    public final boolean jumpOnTrue;
    public final boolean needsExpanding;

    public ComparisonInfo(int comparison, AbstractInsnNode node, Type input, boolean needsExpanding, boolean jumpOnTrue) {
        this.comparison = comparison;
        this.node = node;
        this.input = input;
        this.jumpOnTrue = jumpOnTrue;
        this.needsExpanding = needsExpanding;
    }

    public void attach(BiConsumer<String, Object> decorate, BiConsumer<String, Object> decorateInjectorSpecific) {
        decorateInjectorSpecific.accept(Decorations.COMPARISON_INFO, this);
        decorate.accept(Decorations.SIMPLE_OPERATION_ARGS, new Type[]{input, input});
        decorate.accept(Decorations.SIMPLE_OPERATION_RETURN_TYPE, Type.BOOLEAN_TYPE);
        decorate.accept(Decorations.SIMPLE_OPERATION_PARAM_NAMES, new String[]{"left", "right"});
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
