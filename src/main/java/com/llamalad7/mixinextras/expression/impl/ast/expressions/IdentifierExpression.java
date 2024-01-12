package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

@SerializedExpressionId("id")
public class IdentifierExpression implements SimpleExpression {
    public final String identifier;

    public IdentifierExpression(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
            case Opcodes.GETSTATIC:
                return pool.matches(identifier, node.getInsn());
        }
        return false;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeString(identifier);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new IdentifierExpression(reader.readString());
    }
}
