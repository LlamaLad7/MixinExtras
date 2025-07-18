package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class InstanceofExpression extends SimpleExpression {
    public final Expression expression;
    public final TypeIdentifier type;

    public InstanceofExpression(ExpressionSource src, Expression expression, TypeIdentifier type) {
        super(src);
        this.expression = expression;
        this.type = type;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        if (insn.getOpcode() != Opcodes.INSTANCEOF) {
            return false;
        }
        Type checkType = Type.getObjectType(((TypeInsnNode) insn).desc);
        return type.matches(ctx.pool, checkType) && inputsMatch(node, ctx, expression);
    }
}
