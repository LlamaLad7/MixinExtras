package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.objectweb.asm.Opcodes;

@SerializedTypeName("[]=")
public class ArrayStoreExpression implements Expression {
    @SerializedName("arr")
    public final Expression arr;
    @SerializedName("i")
    public final Expression index;
    @SerializedName("v")
    public final Expression value;

    public ArrayStoreExpression(Expression arr, Expression index, Expression value) {
        this.arr = arr;
        this.index = index;
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                return inputsMatch(node, pool, sink, arr, index, value);
        }
        return false;
    }
}
