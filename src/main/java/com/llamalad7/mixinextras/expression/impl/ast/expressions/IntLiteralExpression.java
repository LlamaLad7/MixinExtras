package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.spongepowered.asm.util.Bytecode;

import java.io.IOException;

@SerializedExpressionId("int")
public class IntLiteralExpression implements SimpleExpression {
    public final long value;

    public IntLiteralExpression(long value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        Object cst = Bytecode.getConstant(node.getInsn());
        if (cst == null) {
            return false;
        }
        return (cst instanceof Integer || cst instanceof Long) && ((Number) cst).longValue() == value;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeLong(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new IntLiteralExpression(reader.readLong());
    }
}
