package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class CastExpression implements SimpleExpression {
    public final Identifier type;
    public final Expression expression;

    public CastExpression(Identifier type, Expression expression) {
        this.type = type;
        this.expression = expression;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        switch (insn.getOpcode()) {
            case Opcodes.CHECKCAST:
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F:
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2D:
                return type.matches(ctx.getPool(), insn, Identifier.Role.TYPE);
        }
        return false;
    }
}
