package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.VarInsnNode;

public class ThisExpression extends SimpleExpression {
    public ThisExpression(ExpressionSource src) {
        super(src);
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        if (ctx.isStatic) {
            return false;
        }
        return node.getInsn().getOpcode() == Opcodes.ALOAD && ((VarInsnNode) node.getInsn()).var == 0;
    }
}
