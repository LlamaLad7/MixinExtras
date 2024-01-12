package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

@SerializedExpressionId("ret")
public class ReturnExpression implements Expression {
    public final Expression value;

    public ReturnExpression(Expression value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
                return inputsMatch(node, pool, sink, value);
        }
        return false;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new ReturnExpression(reader.readExpression());
    }
}
