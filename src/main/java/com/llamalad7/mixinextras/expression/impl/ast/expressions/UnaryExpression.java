package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;


public class UnaryExpression implements SimpleExpression {
    public final Operator operator;
    public final Expression expression;

    public UnaryExpression(Operator operator, Expression expression) {
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        switch (operator) {
            case MINUS:
                switch (node.getInsn().getOpcode()) {
                    case Opcodes.INEG:
                    case Opcodes.LNEG:
                    case Opcodes.FNEG:
                    case Opcodes.DNEG:
                        return inputsMatch(node, ctx, expression);
                }
            case BITWISE_NOT:
                return new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.BITWISE_XOR,
                        new IntLiteralExpression(-1)
                ).matches(node, ctx);
        }
        return false;
    }

    public enum Operator {
        MINUS,
        BITWISE_NOT
    }
}
