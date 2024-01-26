package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MemberAccessExpression implements SimpleExpression {
    public final Expression receiver;
    public final MemberIdentifier name;

    public MemberAccessExpression(Expression receiver, MemberIdentifier name) {
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        switch (insn.getOpcode()) {
            case Opcodes.GETFIELD:
            case Opcodes.ARRAYLENGTH:
                return name.matches(ctx.pool, insn) && inputsMatch(node, ctx, receiver);
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, OutputSink sink) {
        if (node.getInsn().getOpcode() == Opcodes.ARRAYLENGTH) {
            sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_ARGS, new Type[]{node.getInput(0).getType()});
            sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_RETURN_TYPE, Type.INT_TYPE);
        }
        SimpleExpression.super.capture(node, sink);
    }
}
