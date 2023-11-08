package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.objectweb.asm.Opcodes;

@SerializedTypeName("id")
public class IdentifierExpression implements Expression {
    @SerializedName("id")
    public final String identifier;

    public IdentifierExpression(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
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
}
