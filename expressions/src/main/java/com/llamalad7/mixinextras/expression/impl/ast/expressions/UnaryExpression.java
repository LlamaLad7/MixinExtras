package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;


public class UnaryExpression extends SimpleExpression {
    public final Operator operator;
    public final Expression expression;

    public UnaryExpression(ExpressionSource src, Operator operator, Expression expression) {
        super(src);
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    protected MatchResult matchImpl(FlowValue node, ExpressionContext ctx) {
        switch (operator) {
            case MINUS:
                switch (node.getInsn().getOpcode()) {
                    case Opcodes.INEG:
                    case Opcodes.LNEG:
                    case Opcodes.FNEG:
                    case Opcodes.DNEG:
                        return matchInputs(node, ctx, expression);
                }
            case BITWISE_NOT:
                return new BinaryExpression(
                        src,
                        expression,
                        BinaryExpression.Operator.BITWISE_XOR,
                        new IntLiteralExpression(null, -1)
                ).matchImpl(node, ctx);
        }
        return MatchResult.FAILURE;
    }

    public enum Operator {
        MINUS,
        BITWISE_NOT
    }
}
