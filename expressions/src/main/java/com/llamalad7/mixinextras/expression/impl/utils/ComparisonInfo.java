package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.utils.InsnReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.function.BiConsumer;

public class ComparisonInfo {
    protected final int comparison;
    protected final InsnReference node;
    public final Type input;
    public final boolean jumpOnTrue;

    public ComparisonInfo(int comparison, FlowValue node, Type input, boolean jumpOnTrue) {
        this.comparison = comparison;
        this.node = new InsnReference(node);
        this.input = input;
        this.jumpOnTrue = jumpOnTrue;
    }

    public void attach(BiConsumer<String, Object> decorate, BiConsumer<String, Object> decorateInjectorSpecific) {
        decorateInjectorSpecific.accept(ExpressionDecorations.COMPARISON_INFO, this);
        decorate.accept(ExpressionDecorations.SIMPLE_OPERATION_ARGS, new Type[]{input, input});
        decorate.accept(ExpressionDecorations.SIMPLE_OPERATION_RETURN_TYPE, Type.BOOLEAN_TYPE);
        decorate.accept(ExpressionDecorations.SIMPLE_OPERATION_PARAM_NAMES, new String[]{"left", "right"});
    }

    public int copyJump(InsnList insns) {
        return comparison;
    }

    public LabelNode getJumpTarget(Target target) {
        return getJumpInsn(target).label;
    }

    public JumpInsnNode getJumpInsn(Target target) {
        return (JumpInsnNode) node.getNode(target).getCurrentTarget();
    }

    public void cleanup(Target target) {
    }
}
