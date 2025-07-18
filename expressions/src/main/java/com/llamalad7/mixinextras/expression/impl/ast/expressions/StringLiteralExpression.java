package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;

public class StringLiteralExpression extends SimpleExpression {
    public final String value;
    private final Integer charValue;

    public StringLiteralExpression(ExpressionSource src, String value) {
        super(src);
        this.value = value;
        if (value.length() == 1) {
            this.charValue = (int) value.charAt(0);
        } else {
            this.charValue = null;
        }
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        Object cst = ExpressionASMUtils.getConstant(node.getInsn());
        if (cst == null) {
            return false;
        }
        return cst.equals(value) || (node.typeMatches(Type.CHAR_TYPE) && cst.equals(charValue));
    }
}
