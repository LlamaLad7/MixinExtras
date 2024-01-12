package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

@SerializedExpressionId("=")
public class IdentifierAssignmentExpression implements Expression {
    public final Identifier identifier;
    public final Expression value;

    public IdentifierAssignmentExpression(Identifier identifier, Expression value) {
        this.identifier = identifier;
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
            case Opcodes.PUTSTATIC:
                return identifier.matches(pool, node.getInsn()) && inputsMatch(node, pool, sink, value);
        }
        return false;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeIdentifier(identifier);
        writer.writeExpression(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new IdentifierAssignmentExpression(reader.readIdentifier(), reader.readExpression());
    }
}
