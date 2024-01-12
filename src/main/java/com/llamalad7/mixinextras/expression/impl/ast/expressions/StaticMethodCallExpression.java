package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.IOException;
import java.util.List;

@SerializedExpressionId("()")
public class StaticMethodCallExpression implements SimpleExpression {
    public final Identifier name;
    public final List<Expression> arguments;

    public StaticMethodCallExpression(Identifier name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.INVOKESTATIC
                && name.matches(pool, insn) && inputsMatch(node, pool, sink, arguments.toArray(new Expression[0]));
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeIdentifier(name);
        writer.writeExpressions(arguments);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new StaticMethodCallExpression(reader.readIdentifier(), reader.readExpressions());
    }
}
