package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;

public class BooleanLiteralExpression extends SimpleExpression {
    public final boolean value;

    public BooleanLiteralExpression(ExpressionSource src, boolean value) {
        super(src);
        this.value = value;
    }

    @Override
    protected MatchResult matchImpl(FlowValue node, ExpressionContext ctx) {
        if (!node.typeMatches(Type.BOOLEAN_TYPE)) {
            return MatchResult.FAILURE;
        }
        Object cst = ExpressionASMUtils.getConstant(node.getInsn());
        if (cst == null) {
            return MatchResult.FAILURE;
        }
        return MatchResult.basic(cst.equals(value ? 1 : 0));
    }
}
