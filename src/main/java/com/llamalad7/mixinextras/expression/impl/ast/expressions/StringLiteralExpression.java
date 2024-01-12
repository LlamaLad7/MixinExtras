package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.spongepowered.asm.util.Bytecode;

import java.io.IOException;
import java.util.Objects;

@SerializedExpressionId("str")
public class StringLiteralExpression implements SimpleExpression {
    public final String value;

    public StringLiteralExpression(String value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        Object cst = Bytecode.getConstant(node.getInsn());
        return Objects.equals(cst, value) || (!value.isEmpty() && Objects.equals(cst, value.charAt(0)));
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeString(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new StringLiteralExpression(reader.readString());
    }
}
