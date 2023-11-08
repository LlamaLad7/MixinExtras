package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

@SerializedTypeName(".()")
public class MethodCallExpression implements Expression {
    @SerializedName("ex")
    public final Expression receiver;
    @SerializedName("id")
    public final Identifier name;
    @SerializedName("args")
    public final List<Expression> arguments;

    public MethodCallExpression(Expression receiver, Identifier name, List<Expression> arguments) {
        this.receiver = receiver;
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEINTERFACE:
                if (!name.matches(pool, node.getInsn())) {
                    return false;
                }
                Expression[] inputs = ArrayUtils.add(arguments.toArray(new Expression[0]), 0, receiver);
                return inputsMatch(node, pool, sink, inputs);
        }
        return false;
    }
}
