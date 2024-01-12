package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;

@SerializedExpressionId("[]=")
public class ArrayStoreExpression implements Expression {
    public final Expression arr;
    public final Expression index;
    public final Expression value;

    public ArrayStoreExpression(Expression arr, Expression index, Expression value) {
        this.arr = arr;
        this.index = index;
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
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

    @Override
    public void capture(FlowValue node, OutputSink sink) {
        Type arrayType = node.getInput(0).getType();
        sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_ARGS, new Type[]{arrayType, Type.INT_TYPE, arrayType.getElementType()});
        sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_RETURN_TYPE, Type.VOID_TYPE);
        Expression.super.capture(node, sink);
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(arr);
        writer.writeExpression(index);
        writer.writeExpression(value);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new ArrayStoreExpression(reader.readExpression(), reader.readExpression(), reader.readExpression());
    }
}
