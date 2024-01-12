package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

@SerializedExpressionId("bool")
public class BooleanLiteralExpression implements SimpleExpression {
    public final boolean value;

    public BooleanLiteralExpression(boolean value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        if (value) {
            return node.getInsn().getOpcode() == Opcodes.ICONST_1;
        }
        return node.getInsn().getOpcode() == Opcodes.ICONST_0;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeBoolean(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new BooleanLiteralExpression(reader.readBoolean());
    }
}
