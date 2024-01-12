package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

@SerializedExpressionId("null")
public class NullLiteralExpression implements SimpleExpression {
    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return node.getInsn().getOpcode() == Opcodes.ACONST_NULL;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new NullLiteralExpression();
    }
}
