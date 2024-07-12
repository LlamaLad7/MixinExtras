package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;

public class IdentifierExpression extends SimpleExpression {
    public final String identifier;

    public IdentifierExpression(ExpressionSource src, String identifier) {
        super(src);
        this.identifier = identifier;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
            case Opcodes.GETSTATIC:
                return ctx.pool.matchesMember(identifier, node);
        }
        return false;
    }
}
