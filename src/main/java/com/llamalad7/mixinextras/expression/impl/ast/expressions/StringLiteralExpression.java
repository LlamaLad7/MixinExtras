package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.spongepowered.asm.util.Bytecode;

public class StringLiteralExpression implements SimpleExpression {
    public final String value;
    private final Integer charValue;

    public StringLiteralExpression(String value) {
        this.value = value;
        if (value.length() == 1) {
            this.charValue = (int) value.charAt(0);
        } else {
            this.charValue = null;
        }
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        Object cst = Bytecode.getConstant(node.getInsn());
        if (cst == null) {
            return false;
        }
        return cst.equals(value) || cst.equals(charValue);
    }
}
