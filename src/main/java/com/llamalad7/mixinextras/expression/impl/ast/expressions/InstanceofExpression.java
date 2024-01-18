package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class InstanceofExpression implements SimpleExpression {
    public final Expression expression;
    public final Identifier type;

    public InstanceofExpression(Expression expression, Identifier type) {
        this.expression = expression;
        this.type = type;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.INSTANCEOF && type.matches(ctx.getPool(), insn) && inputsMatch(node, ctx, expression);
    }
}
