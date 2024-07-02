package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;

public class IdentifierAssignmentExpression extends Expression {
    public final MemberIdentifier identifier;
    public final Expression value;

    public IdentifierAssignmentExpression(ExpressionSource src, MemberIdentifier identifier, Expression value) {
        super(src);
        this.identifier = identifier;
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
            case Opcodes.PUTSTATIC:
                return identifier.matches(ctx.pool, node.getInsn()) && inputsMatch(node, ctx, value);
        }
        return false;
    }
}
