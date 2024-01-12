package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;

import java.io.IOException;


@SerializedExpressionId("un")
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

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeEnum(operator);
        writer.writeExpression(expression);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new UnaryExpression(reader.readEnum(Operator::valueOf), reader.readExpression());
    }

    public enum Operator {
        MINUS,
        BITWISE_NOT
    }
}
