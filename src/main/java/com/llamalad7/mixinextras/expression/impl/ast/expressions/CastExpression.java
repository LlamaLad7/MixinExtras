package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class CastExpression implements SimpleExpression {
    public final TypeIdentifier type;
    public final Expression expression;

    public CastExpression(TypeIdentifier type, Expression expression) {
        this.type = type;
        this.expression = expression;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        Type castType = getCastType(node.getInsn());
        return castType != null && type.matches(ctx.pool, castType) && inputsMatch(node, ctx, expression);
    }

    private Type getCastType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.CHECKCAST:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
                return Type.INT_TYPE;
            case Opcodes.I2B:
                return Type.BYTE_TYPE;
            case Opcodes.I2C:
               return Type.CHAR_TYPE;
            case Opcodes.I2S:
                return Type.SHORT_TYPE;
            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F:
                return Type.FLOAT_TYPE;
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                return Type.LONG_TYPE;
            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2D:
                return Type.DOUBLE_TYPE;
        }
        return null;
    }
}
