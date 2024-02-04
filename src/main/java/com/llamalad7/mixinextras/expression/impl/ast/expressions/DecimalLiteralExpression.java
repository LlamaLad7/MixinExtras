package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.spongepowered.asm.util.Bytecode;

public class DecimalLiteralExpression extends SimpleExpression {
    public final double value;

    public DecimalLiteralExpression(ExpressionSource src, double value) {
        super(src);
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        Object cst = Bytecode.getConstant(node.getInsn());
        if (cst == null) {
            return false;
        }
        return (cst instanceof Float || cst instanceof Double) && String.valueOf(value).equals(cst.toString());
    }
}
