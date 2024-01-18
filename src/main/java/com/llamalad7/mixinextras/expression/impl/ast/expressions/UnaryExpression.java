package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;


public class UnaryExpression implements SimpleExpression {
    public final Operator operator;
    public final Expression expression;

    public UnaryExpression(Operator operator, Expression expression) {
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        switch (operator) {
            case MINUS:
                switch (node.getInsn().getOpcode()) {
                    case Opcodes.INEG:
                    case Opcodes.LNEG:
                    case Opcodes.FNEG:
                    case Opcodes.DNEG:
                        return inputsMatch(node, pool, sink, expression);
                }
            case BITWISE_NOT:
                return new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.BITWISE_XOR,
                        new IntLiteralExpression(-1)
                ).matches(node, pool, sink);
        }
        return false;
    }

    public enum Operator {
        MINUS,
        BITWISE_NOT
    }
}
